package de.moinmorni.versteckerl.game;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import de.moinmorni.versteckerl.rules.Catalog;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

/**
 * Klang und Optik der Sachen aus dem Kramerladen – nur aus Minecraft-Klängen und -Partikeln zusammengesetzt, eigene
 * Abfolgen: Pfiff-Klänge (Flöte, Kuckuck, Henne, Murmeltier, Kuhglocke, Jodler, Trompete, Alphorn), Fund-Effekte,
 * Spuren in der Gendarmerie und Sieg-Effekte. Lautere Pfiffe hört man weiter und sieht man mehr.
 */
public final class Flair {

    /** Ein Ton: nach {@code delay} Ticks Klang {@code key} in Tonhöhe {@code pitch} (0,5–2). */
    private record Note(int delay, String key, float pitch) {
    }

    private static final String FLUTE = "minecraft:block.note_block.flute";
    private static final String HARP = "minecraft:block.note_block.harp";
    private static final String CHIME = "minecraft:block.note_block.chime";
    private static final String COW_BELL = "minecraft:block.note_block.cow_bell";
    private static final String TRUMPET = "minecraft:block.note_block.trumpet";
    private static final String CHICKEN = "minecraft:entity.chicken.ambient";
    private static final String EGG = "minecraft:entity.chicken.egg";
    private static final String HORN = "minecraft:item.goat_horn.sound.0";
    private static final String BELL = "minecraft:block.bell.use";
    /** Tonhöhen wie am Notenblock (Fis = 0,5 … Fis' = 2). */
    private static final float C = 0.707f;
    private static final float D = 0.794f;
    private static final float E = 0.891f;
    private static final float G = 1.059f;
    private static final float A = 1.189f;
    private static final float C2 = 1.414f;
    private static final float E2 = 1.782f;
    private static final float G2 = 2.0f;
    private static final Map<String, List<Note>> WHISTLES = Map.of(
            "floete", List.of(new Note(0, FLUTE, 1.3f), new Note(4, FLUTE, 1.8f)),
            "kuckuck", List.of(new Note(0, FLUTE, 1.5f), new Note(6, FLUTE, 1.19f), new Note(18, FLUTE, 1.5f),
                    new Note(24, FLUTE, 1.19f)),
            "henne", List.of(new Note(0, CHICKEN, 1.0f), new Note(6, CHICKEN, 1.25f), new Note(12, EGG, 1.0f)),
            "murmeltier", List.of(new Note(0, FLUTE, 2.0f), new Note(1, CHIME, 2.0f), new Note(9, FLUTE, 2.0f),
                    new Note(10, CHIME, 2.0f)),
            "kuhglocke", List.of(new Note(0, COW_BELL, 1.0f), new Note(5, COW_BELL, 1.06f),
                    new Note(10, COW_BELL, 0.94f), new Note(15, COW_BELL, 1.0f), new Note(20, COW_BELL, 1.06f)),
            "jodler", List.of(new Note(0, HARP, C), new Note(3, FLUTE, G), new Note(6, HARP, E), new Note(9, FLUTE, C2),
                    new Note(12, FLUTE, G2), new Note(15, FLUTE, E2), new Note(18, HARP, G), new Note(21, FLUTE, C2),
                    new Note(26, HARP, C)),
            "trompete", List.of(new Note(0, TRUMPET, G), new Note(3, TRUMPET, G), new Note(6, TRUMPET, G),
                    new Note(9, TRUMPET, C2), new Note(17, TRUMPET, A), new Note(20, TRUMPET, C2)),
            "alphorn", List.of(new Note(0, HORN, 0.9f), new Note(0, HARP, D), new Note(10, HARP, A)));
    private static final Color GREEN = Color.fromRGB(0x2BB673);
    private static final Color GOLD = Color.fromRGB(0xF2B632);
    private static final Color BLUE = Color.fromRGB(0x5DA9E9);
    private static final Color[] CONFETTI = {Color.fromRGB(0xE8453C), Color.fromRGB(0xF2B632), Color.fromRGB(0x2BB673),
        Color.fromRGB(0x5DA9E9), Color.fromRGB(0xB565D9), Color.fromRGB(0xFF8FC7)};
    private static final int WIN_BURSTS = 3;
    private static final long WIN_EVERY = 12L;

    private Flair() {
    }

    /** Lautstärke für den Klang: leise 3 (hört man etwa 48 Blöcke weit), laut 4, sehr laut 6. */
    private static float volume(int loudness) {
        return loudness >= Catalog.VERY_LOUD ? 6f : loudness == Catalog.LOUD ? 4f : 3f;
    }

    // ---------- Pfiffe ----------

    /**
     * Pfiff an einer Stelle: der Klang für alle in der Nähe, dazu Noten – lautere Klänge mit mehr und höheren Noten
     * (auffälliger).
     */
    public static void whistle(Plugin plugin, Location at, String sound, int loudness) {
        World w = at.getWorld();
        Location ear = at.clone().add(0, 0.6, 0);
        float vol = volume(loudness);
        for (Note n : WHISTLES.getOrDefault(sound, WHISTLES.get("floete"))) {
            Runnable play = () -> w.playSound(Sound.sound(Key.key(n.key()), Sound.Source.PLAYER, vol, n.pitch()),
                    ear.getX(), ear.getY(), ear.getZ());
            if (n.delay() == 0) {
                play.run();
            } else {
                Bukkit.getScheduler().runTaskLater(plugin, play, n.delay());
            }
        }
        w.spawnParticle(Particle.NOTE, at.clone().add(0, 1.3, 0), 4 + 3 * (loudness - 1), 0.3, 0.2, 0.3, 1);
        if (loudness >= Catalog.LOUD) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> w.spawnParticle(Particle.NOTE, at.clone().add(0, 2.1, 0),
                    6, 0.4, 0.3, 0.4, 1), 5L);
        }
        if (loudness >= Catalog.VERY_LOUD) {
            for (int i = 0; i < 3; i++) {
                double h = 2.6 + i * 0.6;
                Bukkit.getScheduler().runTaskLater(plugin, () -> w.spawnParticle(Particle.END_ROD,
                        at.clone().add(0, h, 0), 3, 0.1, 0.1, 0.1, 0.02), 4L + 4L * i);
            }
        }
    }

    /** Probehören im Laden: nur für diese Person, leise und an ihrem Ohr. */
    public static void previewWhistle(Plugin plugin, Player p, String sound) {
        for (Note n : WHISTLES.getOrDefault(sound, WHISTLES.get("floete"))) {
            Runnable play = () -> p.playSound(Sound.sound(Key.key(n.key()), Sound.Source.MASTER, 1f, n.pitch()));
            if (n.delay() == 0) {
                play.run();
            } else {
                Bukkit.getScheduler().runTaskLater(plugin, play, n.delay());
            }
        }
    }

    // ---------- Fund-Effekte ----------

    /** Effekt, wenn ein Gendarm jemanden findet (für alle sichtbar). */
    public static void find(Location at, String id) {
        burst(at.getWorld(), null, at.clone().add(0, 1, 0), id);
        String sound = switch (id) {
            case "rauchwolke" -> "minecraft:block.fire.extinguish";
            case "konfetti", "weissblau" -> "minecraft:entity.firework_rocket.twinkle";
            case "goldregen" -> "minecraft:block.amethyst_block.chime";
            case "blueten" -> "minecraft:block.cherry_leaves.place";
            default -> null;
        };
        if (sound != null) {
            at.getWorld().playSound(Sound.sound(Key.key(sound), Sound.Source.PLAYER, 1.2f, 1.1f), at.getX(), at.getY(),
                    at.getZ());
        }
    }

    /** Partikelwolke einer Art – für alle ({@code only} null) oder nur für eine Person (Vorschau im Laden). */
    private static void burst(World w, Player only, Location at, String id) {
        switch (id) {
            case "herzerl" -> spawn(w, only, Particle.HEART, at, 12, 0.6, 0.5, 0, null);
            case "rauchwolke" -> {
                spawn(w, only, Particle.LARGE_SMOKE, at, 30, 0.5, 0.6, 0.02, null);
                spawn(w, only, Particle.CLOUD, at, 25, 0.6, 0.5, 0.03, null);
            }
            case "konfetti" -> {
                for (Color c : CONFETTI) {
                    spawn(w, only, Particle.DUST, at.clone().add(0, 0.4, 0), 9, 0.7, 0.7, 0,
                            new Particle.DustOptions(c, 1.3f));
                }
            }
            case "blueten" -> spawn(w, only, Particle.CHERRY_LEAVES, at.clone().add(0, 0.8, 0), 40, 0.9, 0.6, 0, null);
            case "weissblau" -> {
                for (int i = 0; i < 16; i++) {
                    double a = i * Math.PI / 8;
                    Location l = at.clone().add(Math.cos(a) * 0.9, 0.2 + i * 0.06, Math.sin(a) * 0.9);
                    spawn(w, only, Particle.DUST, l, 2, 0.05, 0.05, 0,
                            new Particle.DustOptions(i % 2 == 0 ? Color.WHITE : BLUE, 1.4f));
                }
                spawn(w, only, Particle.FIREWORK, at.clone().add(0, 0.6, 0), 20, 0.3, 0.4, 0.08, null);
            }
            case "goldregen" -> {
                BlockData gold = Material.GOLD_BLOCK.createBlockData();
                spawn(w, only, Particle.FALLING_DUST, at.clone().add(0, 1.6, 0), 40, 0.8, 0.2, 0, gold);
                spawn(w, only, Particle.DUST, at.clone().add(0, 0.6, 0), 15, 0.5, 0.5, 0,
                        new Particle.DustOptions(GOLD, 1.2f));
            }
            default -> spawn(w, only, Particle.HAPPY_VILLAGER, at, 20, 0.4, 0.6, 0, null);
        }
    }

    private static <T> void spawn(World w, Player only, Particle type, Location at, int count, double spread,
            double up, double speed, T data) {
        if (only != null) {
            only.spawnParticle(type, at, count, spread, up, spread, speed, data);
        } else {
            w.spawnParticle(type, at, count, spread, up, spread, speed, data);
        }
    }

    // ---------- Spuren (nur in der Gendarmerie) ----------

    /** Ein Stück Spur an den Füßen (für alle in der Gendarmerie sichtbar). */
    public static void trail(Player p, String id) {
        Location at = p.getLocation().add(0, 0.15, 0);
        World w = p.getWorld();
        ThreadLocalRandom r = ThreadLocalRandom.current();
        switch (id) {
            case "noten" -> w.spawnParticle(Particle.NOTE, at.clone().add(0, 0.4, 0), 0, r.nextDouble(), 0, 0, 1);
            case "herzerl" -> w.spawnParticle(Particle.HEART, at.clone().add(0, 0.3, 0), 1, 0.15, 0.05, 0.15, 0);
            case "schneeflocken" -> w.spawnParticle(Particle.SNOWFLAKE, at.clone().add(0, 0.3, 0), 3, 0.2, 0.1, 0.2, 0.01);
            case "blueten" -> w.spawnParticle(Particle.CHERRY_LEAVES, at.clone().add(0, 0.4, 0), 2, 0.25, 0.1, 0.25, 0);
            case "funken" -> {
                w.spawnParticle(Particle.DUST, at, 2, 0.2, 0.05, 0.2, 0,
                        new Particle.DustOptions(r.nextBoolean() ? Color.WHITE : BLUE, 1.0f));
                w.spawnParticle(Particle.ELECTRIC_SPARK, at, 1, 0.15, 0.05, 0.15, 0);
            }
            case "gluehwuermchen" -> w.spawnParticle(Particle.FIREFLY, at.clone().add(0, 0.6, 0), 2, 0.5, 0.4, 0.5, 0);
            default -> {
            }
        }
    }

    // ---------- Sieg-Effekte ----------

    /** Am Ende einer gewonnenen Runde: dreimal kurz hintereinander über dem Kopf. */
    public static void win(Plugin plugin, Player p, String id) {
        if ("keiner".equals(id)) {
            return;
        }
        for (int i = 0; i < WIN_BURSTS; i++) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (p.isOnline()) {
                    winBurst(p.getLocation().add(0, 2.3, 0), id);
                }
            }, 1L + i * WIN_EVERY);
        }
    }

    private static void winBurst(Location at, String id) {
        World w = at.getWorld();
        switch (id) {
            case "glocken" -> {
                w.playSound(Sound.sound(Key.key(BELL), Sound.Source.PLAYER, 1.5f, 1.2f), at.getX(), at.getY(), at.getZ());
                w.spawnParticle(Particle.NOTE, at, 6, 0.6, 0.3, 0.6, 1);
            }
            case "konfetti", "goldregen", "weissblau" -> burst(w, null, at, id);
            case "feuerwerk" -> firework(at);
            default -> {
            }
        }
    }

    private static void firework(Location at) {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        Location l = at.clone().add(r.nextDouble(-0.6, 0.6), 0.8, r.nextDouble(-0.6, 0.6));
        Firework fw = at.getWorld().spawn(l, Firework.class, f -> {
            FireworkMeta meta = f.getFireworkMeta();
            meta.addEffect(FireworkEffect.builder().with(r.nextBoolean() ? FireworkEffect.Type.BALL_LARGE
                    : FireworkEffect.Type.STAR).withColor(GREEN, GOLD).withFade(Color.WHITE, BLUE).withFlicker().build());
            meta.setPower(0);
            f.setFireworkMeta(meta);
            f.setPersistent(false);
            f.setVelocity(new Vector());
        });
        fw.detonate();
    }

    // ---------- Vorschau im Laden ----------

    /** Vorschau eines Effekts nur für diese Person, kurz vor ihr. */
    public static void preview(Plugin plugin, Player p, Catalog.Item item) {
        Location front = p.getEyeLocation().add(p.getEyeLocation().getDirection().setY(0).normalize().multiply(1.6))
                .subtract(0, 0.6, 0);
        switch (item.kind()) {
            case WHISTLE -> previewWhistle(plugin, p, item.id());
            case FIND -> burst(p.getWorld(), p, front, item.id());
            case WIN -> {
                if ("feuerwerk".equals(item.id())) {
                    p.spawnParticle(Particle.FIREWORK, front.clone().add(0, 0.6, 0), 30, 0.3, 0.3, 0.3, 0.1);
                    p.playSound(Sound.sound(Key.key("minecraft:entity.firework_rocket.large_blast"),
                            Sound.Source.MASTER, 1f, 1f));
                } else if ("glocken".equals(item.id())) {
                    p.playSound(Sound.sound(Key.key(BELL), Sound.Source.MASTER, 1f, 1.2f));
                    p.spawnParticle(Particle.NOTE, front.clone().add(0, 0.6, 0), 6, 0.5, 0.3, 0.5, 1);
                } else {
                    burst(p.getWorld(), p, front, item.id());
                }
            }
            case TRAIL -> {
                for (int i = 0; i < 4; i++) {
                    Location l = front.clone().add(0, -0.7, 0);
                    switch (item.id()) {
                        case "noten" -> p.spawnParticle(Particle.NOTE, l.clone().add(0, 0.4, 0), 0, i / 4.0, 0, 0, 1);
                        case "herzerl" -> p.spawnParticle(Particle.HEART, l.clone().add(0, 0.3, 0), 2, 0.3, 0.1, 0.3, 0);
                        case "schneeflocken" -> p.spawnParticle(Particle.SNOWFLAKE, l, 6, 0.3, 0.2, 0.3, 0.01);
                        case "blueten" -> p.spawnParticle(Particle.CHERRY_LEAVES, l, 6, 0.4, 0.2, 0.4, 0);
                        case "funken" -> p.spawnParticle(Particle.DUST, l, 6, 0.3, 0.1, 0.3, 0,
                                new Particle.DustOptions(i % 2 == 0 ? Color.WHITE : BLUE, 1.0f));
                        case "gluehwuermchen" -> p.spawnParticle(Particle.FIREFLY, l.clone().add(0, 0.6, 0), 4, 0.6, 0.4,
                                0.6, 0);
                        default -> {
                        }
                    }
                }
            }
            default -> {
            }
        }
    }
}
