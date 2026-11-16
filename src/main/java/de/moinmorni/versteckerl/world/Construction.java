package de.moinmorni.versteckerl.world;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.banner.PatternType;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

/**
 * Baut Gendarmerie und Karten aus den Bauplänen – nur wenn sich die Bau-Version geändert hat (gemerkt in
 * {@code bau.yml}). Die Gendarmerie entsteht sofort, die Karten Stück für Stück (höchstens {@link #BUDGET_NANOS} pro
 * Tick). Vorher wird abgeräumt, was der alte Bau belegt hatte.
 */
public final class Construction {

    /** Bau-Version: erhöhen, wenn sich Gendarmerie oder Karten ändern – dann wird beim nächsten Start neu gebaut. */
    public static final int VERSION = 1;
    private static final long BUDGET_NANOS = 25_000_000L;
    private static final int CHECK_EVERY = 256;

    private interface Job {
        boolean step(long deadline);
    }

    private final Plugin plugin;
    private final Logger log;
    private final World world;
    private final Path stateFile;
    private final Deque<Job> jobs = new ArrayDeque<>();
    private final Map<String, BlockData> cache = new HashMap<>();
    private final Set<String> broken = new HashSet<>();
    private BukkitTask task;
    private int placed;

    public Construction(Plugin plugin, World world) {
        this.plugin = plugin;
        this.log = plugin.getLogger();
        this.world = world;
        this.stateFile = plugin.getDataFolder().toPath().resolve("bau.yml");
    }

    public boolean running() {
        return task != null;
    }

    /** Baut, falls nötig; {@code done} läuft auf dem Hauptthread, sobald alles steht. */
    public void ensure(boolean force, Runnable done) {
        YamlConfiguration state = readState();
        if (!force && state.getInt("version") == VERSION) {
            done.run();
            return;
        }
        log.info("Gendarmerie und Karten werden gebaut (Bau-Version " + VERSION + ") …");
        long start = System.currentTimeMillis();
        List<String> boxes = new ArrayList<>();
        List<int[]> later = new ArrayList<>();
        for (String box : state.getStringList("bereiche")) {
            int[] b = parseBox(box);
            if (b == null) {
                continue;
            }
            if (Math.abs(b[0]) < 500 && Math.abs(b[3]) < 500) {
                jobs.add(clearJob(b));
            } else {
                later.add(b);
            }
        }
        Plan hall = HallPlan.build();
        boxes.add(boxOf(hall, 0, MapType.FLOOR_Y, 0));
        jobs.add(placeJob(hall, 0, MapType.FLOOR_Y, 0));
        runNow();
        later.forEach(b -> jobs.add(clearJob(b)));
        for (MapType m : MapType.values()) {
            Plan plan = m.plan();
            boxes.add(boxOf(plan, m.originX(), MapType.FLOOR_Y, m.originZ()));
            jobs.add(placeJob(plan, m.originX(), MapType.FLOOR_Y, m.originZ()));
        }
        jobs.add(deadline -> {
            YamlConfiguration fresh = new YamlConfiguration();
            fresh.set("version", VERSION);
            fresh.set("bereiche", boxes);
            writeState(fresh);
            log.info("Fertig gebaut: " + placed + " Blöcke in " + (System.currentTimeMillis() - start) / 1000 + " s.");
            done.run();
            return true;
        });
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);
    }

    private void runNow() {
        while (!jobs.isEmpty()) {
            Job job = jobs.peekFirst();
            if (job.step(Long.MAX_VALUE)) {
                jobs.pollFirst();
            }
        }
    }

    private void tick() {
        long deadline = System.nanoTime() + BUDGET_NANOS;
        while (!jobs.isEmpty() && System.nanoTime() < deadline) {
            Job job = jobs.peekFirst();
            boolean finished;
            try {
                finished = job.step(deadline);
            } catch (RuntimeException e) {
                log.log(Level.WARNING, "Bauschritt fehlgeschlagen – wird übersprungen", e);
                finished = true;
            }
            if (finished) {
                jobs.pollFirst();
            }
        }
        if (jobs.isEmpty() && task != null) {
            task.cancel();
            task = null;
        }
    }

    /** Blockzustand aus Text; unbekannte Namen werden einmal laut gemeldet und ausgelassen. */
    private BlockData data(String spec) {
        BlockData d = cache.get(spec);
        if (d != null || broken.contains(spec)) {
            return d;
        }
        try {
            d = Bukkit.createBlockData(spec.indexOf(':') >= 0 ? spec : "minecraft:" + spec);
            cache.put(spec, d);
        } catch (IllegalArgumentException e) {
            broken.add(spec);
            log.severe("Unbekannter Block im Bauplan: " + spec + " – wird ausgelassen.");
        }
        return d;
    }

    private Job placeJob(Plan plan, int ox, int oy, int oz) {
        List<Long> keys = new ArrayList<>(plan.size());
        List<String> specs = new ArrayList<>(plan.size());
        plan.forEach((k, s) -> {
            keys.add(k);
            specs.add(s);
        });
        List<Plan.Banner> banners = plan.banners();
        int[] index = {0};
        return deadline -> {
            int i = index[0];
            int n = 0;
            while (i < keys.size()) {
                long k = keys.get(i);
                BlockData d = data(specs.get(i));
                if (d != null) {
                    world.getBlockAt(ox + Plan.keyX(k), oy + Plan.keyY(k), oz + Plan.keyZ(k)).setBlockData(d, false);
                    placed++;
                }
                i++;
                if (++n % CHECK_EVERY == 0 && System.nanoTime() > deadline) {
                    index[0] = i;
                    return false;
                }
            }
            index[0] = i;
            for (Plan.Banner b : banners) {
                Block block = world.getBlockAt(ox + b.x(), oy + b.y(), oz + b.z());
                BlockState st = block.getState();
                if (st instanceof org.bukkit.block.Banner banner) {
                    banner.setPatterns(patterns(b.patterns()));
                    banner.update(true, false);
                }
            }
            return true;
        };
    }

    /** {@code "lime:rhombus"} → Muster. */
    private List<Pattern> patterns(List<String> specs) {
        var registry = io.papermc.paper.registry.RegistryAccess.registryAccess()
                .getRegistry(io.papermc.paper.registry.RegistryKey.BANNER_PATTERN);
        List<Pattern> out = new ArrayList<>();
        for (String s : specs) {
            String[] parts = s.split(":");
            try {
                PatternType type = registry.get(NamespacedKey.minecraft(parts[1]));
                if (type != null) {
                    out.add(new Pattern(DyeColor.valueOf(parts[0].toUpperCase(Locale.ROOT)), type));
                }
            } catch (RuntimeException e) {
                log.warning("Bannermuster unbekannt: " + s);
            }
        }
        return out;
    }

    private Job clearJob(int[] b) {
        int[] cursor = {b[0], b[1], b[2]};
        boolean[] entitiesGone = {false};
        return deadline -> {
            if (!entitiesGone[0]) {
                for (Entity e : world.getEntities()) {
                    if (!(e instanceof Player)) {
                        org.bukkit.Location l = e.getLocation();
                        if (l.getX() >= b[0] && l.getX() <= b[3] + 1 && l.getZ() >= b[2] && l.getZ() <= b[5] + 1) {
                            e.remove();
                        }
                    }
                }
                entitiesGone[0] = true;
            }
            int n = 0;
            while (cursor[0] <= b[3]) {
                Block block = world.getBlockAt(cursor[0], cursor[1], cursor[2]);
                if (block.getType() != Material.AIR) {
                    block.setType(Material.AIR, false);
                }
                if (++cursor[2] > b[5]) {
                    cursor[2] = b[2];
                    if (++cursor[1] > b[4]) {
                        cursor[1] = b[1];
                        cursor[0]++;
                    }
                }
                if (++n % CHECK_EVERY == 0 && System.nanoTime() > deadline) {
                    return false;
                }
            }
            return true;
        };
    }

    private static String boxOf(Plan plan, int ox, int oy, int oz) {
        int[] b = plan.bounds();
        return (ox + b[0]) + " " + (oy + b[1]) + " " + (oz + b[2]) + " " + (ox + b[3]) + " " + (oy + b[4]) + " "
                + (oz + b[5]);
    }

    private static int[] parseBox(String s) {
        String[] parts = s.trim().split("\\s+");
        if (parts.length != 6) {
            return null;
        }
        int[] b = new int[6];
        try {
            for (int i = 0; i < 6; i++) {
                b[i] = Integer.parseInt(parts[i]);
            }
        } catch (NumberFormatException e) {
            return null;
        }
        return b;
    }

    private YamlConfiguration readState() {
        YamlConfiguration y = new YamlConfiguration();
        if (Files.isRegularFile(stateFile)) {
            try {
                y.loadFromString(Files.readString(stateFile, StandardCharsets.UTF_8));
            } catch (IOException | InvalidConfigurationException e) {
                log.warning("bau.yml ließ sich nicht lesen – es wird neu gebaut: " + e.getMessage());
            }
        }
        return y;
    }

    private void writeState(YamlConfiguration y) {
        try {
            Files.createDirectories(stateFile.getParent());
            Files.writeString(stateFile, y.saveToString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.log(Level.WARNING, "bau.yml ließ sich nicht schreiben", e);
        }
    }
}
