package de.moinmorni.versteckerl;

import de.moinmorni.versteckerl.data.Leaderboard;
import de.moinmorni.versteckerl.data.PlayerData;
import de.moinmorni.versteckerl.data.Store;
import de.moinmorni.versteckerl.game.Game;
import de.moinmorni.versteckerl.game.Games;
import de.moinmorni.versteckerl.hall.Hall;
import de.moinmorni.versteckerl.hall.Kraemer;
import de.moinmorni.versteckerl.hall.Shop;
import de.moinmorni.versteckerl.listen.GameListener;
import de.moinmorni.versteckerl.listen.GuardListener;
import de.moinmorni.versteckerl.listen.PlayerListener;
import de.moinmorni.versteckerl.menu.Menu;
import de.moinmorni.versteckerl.menu.MenuListener;
import de.moinmorni.versteckerl.net.Network;
import de.moinmorni.versteckerl.world.Construction;
import de.moinmorni.versteckerl.world.HallPlan;
import de.moinmorni.versteckerl.world.MapType;
import de.moinmorni.versteckerl.world.VoidGenerator;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Difficulty;
import org.bukkit.GameRules;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Morni Versteckerl: Verstecken mit Verwandlung. Die Versteckten werden zu einem Gegenstand der Karte, die
 * Dorfgendarmen suchen sie. Drei Karten (Dorfplatz, Wirtshaus, Heustadel). Punkte aus den Runden kommen aufs
 * Guthaben; damit kauft man im Kramerladen der Gendarmerie Gestalten, Pfiff-Klänge, Effekte und Titel. Spricht mit dem
 * MoinMorni-Netzwerk, wenn es da ist, und läuft sonst allein.
 */
public final class VersteckerlPlugin extends JavaPlugin {

    private static final String ADMIN = "moinmorni.versteckerl.admin";
    private static final long SECOND = 20L;
    private static final long LANGUAGE_CHECK = 40L;
    private static final long STATE_REFRESH = 20L * 60;
    private static final long BOARD_REFRESH = 20L * 30;
    private static final long TRAIL_EVERY = 3L;
    private static VersteckerlPlugin instance;

    private Settings settings;
    private Network net;
    private Store store;
    private Leaderboard board;
    private World world;
    private Construction construction;
    private Hall hall;
    private Games games;
    private Boards boards;
    private Shop shop;
    private Kraemer kraemer;
    private volatile boolean ready;
    private boolean stateQueued;
    private String lastState = "";
    private int lastPlayers = -1;
    private final Map<UUID, String> lastLang = new HashMap<>();

    public static VersteckerlPlugin instance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        settings = Settings.load(getConfig());
        Items.init(this);
        net = Network.connect(this);
        getLogger().info(net.connected()
                ? "Mit dem MoinMorni-Netzwerk verbunden (Server „" + net.serverName() + "“)."
                : "Kein Netzwerk-Plugin: Versteckerl läuft allein (Sprache aus dem Spiel, Statistik nur lokal).");
        store = new Store(getLogger(), getDataFolder().toPath());
        board = loadBoard();
        world = createWorld(settings.world());
        Game.cleanStray(world);
        shop = new Shop(this);
        boards = new Boards(this);
        hall = new Hall(this);
        games = new Games(this);

        var pm = getServer().getPluginManager();
        pm.registerEvents(new MenuListener(), this);
        pm.registerEvents(new PlayerListener(this), this);
        pm.registerEvents(new GameListener(this), this);
        pm.registerEvents(new GuardListener(this), this);
        Commands commands = new Commands(this);
        for (String name : List.of("versteckerladmin", "karte", "bereit", "gestalt", "zuschauen", "verlassen", "laden",
                "stats", "top")) {
            PluginCommand c = getCommand(name);
            if (c != null) {
                c.setExecutor(commands);
                c.setTabCompleter(commands);
            }
        }

        keepHallLoaded();
        construction = new Construction(this, world);
        construction.ensure(false, () -> {
            ready = true;
            getLogger().info("Versteckerl ist bereit: " + MapType.values().length + " Karten.");
        });
        boards.spawnHall();
        kraemer = new Kraemer(this);
        kraemer.spawn();

        var scheduler = getServer().getScheduler();
        scheduler.runTaskTimer(this, games::tick, 1L, 1L);
        scheduler.runTaskTimer(this, () -> {
            hall.tick();
            boards.hallAll();
            kraemer.tick();
            refreshMenus();
        }, SECOND, SECOND);
        scheduler.runTaskTimer(this, hall::trails, TRAIL_EVERY, TRAIL_EVERY);
        scheduler.runTaskTimer(this, this::checkLanguages, LANGUAGE_CHECK, LANGUAGE_CHECK);
        scheduler.runTaskTimer(this, () -> reportState(true), STATE_REFRESH, STATE_REFRESH);
        scheduler.runTaskTimer(this, boards::refreshHall, BOARD_REFRESH, BOARD_REFRESH);

        for (Player p : getServer().getOnlinePlayers()) {
            data(p);
            hall.enter(p);
        }
        stateChanged();
    }

    @Override
    public void onDisable() {
        if (games != null) {
            games.abortAll();
        }
        if (boards != null) {
            boards.removeHall();
        }
        if (kraemer != null) {
            kraemer.remove();
        }
        Holo.removeAll();
        if (store != null) {
            if (board != null) {
                store.write(boardFile(), board.toYaml());
            }
            store.close();
        }
        instance = null;
    }

    /**
     * Die Gendarmerie bleibt immer geladen. Ihre Tafeln sind nicht dauerhaft gespeichert – ohne Ticket entlädt der
     * Server die Chunks, sobald nach dem Start niemand da ist, und die Tafeln wären bis zum nächsten Neustart weg.
     */
    private void keepHallLoaded() {
        int from = -HallPlan.REACH >> 4;
        int to = HallPlan.REACH >> 4;
        for (int cx = from; cx <= to; cx++) {
            for (int cz = from; cz <= to; cz++) {
                world.getChunkAt(cx, cz).addPluginChunkTicket(this);
            }
        }
    }

    private World createWorld(String name) {
        World w = getServer().getWorld(name);
        if (w == null) {
            w = new WorldCreator(name).generator(new VoidGenerator()).environment(World.Environment.NORMAL)
                    .generateStructures(false).createWorld();
        }
        if (w == null) {
            throw new IllegalStateException("Welt " + name + " ließ sich nicht anlegen");
        }
        w.setGameRule(GameRules.ADVANCE_TIME, false);
        w.setGameRule(GameRules.ADVANCE_WEATHER, false);
        w.setGameRule(GameRules.SPAWN_MOBS, false);
        w.setGameRule(GameRules.SPAWN_MONSTERS, false);
        w.setGameRule(GameRules.SPAWN_PHANTOMS, false);
        w.setGameRule(GameRules.SPAWN_PATROLS, false);
        w.setGameRule(GameRules.SPAWN_WANDERING_TRADERS, false);
        w.setGameRule(GameRules.SHOW_DEATH_MESSAGES, false);
        w.setGameRule(GameRules.SHOW_ADVANCEMENT_MESSAGES, false);
        w.setGameRule(GameRules.IMMEDIATE_RESPAWN, true);
        w.setGameRule(GameRules.RANDOM_TICK_SPEED, 0);
        w.setGameRule(GameRules.MOB_GRIEFING, false);
        w.setGameRule(GameRules.BLOCK_DROPS, false);
        w.setGameRule(GameRules.ENTITY_DROPS, false);
        w.setGameRule(GameRules.FALL_DAMAGE, false);
        w.setGameRule(GameRules.NATURAL_HEALTH_REGENERATION, false);
        w.setTime(5000L);
        w.setStorm(false);
        w.setThundering(false);
        w.setDifficulty(Difficulty.PEACEFUL);
        w.setSpawnLocation(new org.bukkit.Location(w, HallPlan.SPAWN_X, MapType.FLOOR_Y + HallPlan.SPAWN_Y,
                HallPlan.SPAWN_Z));
        return w;
    }

    private Path boardFile() {
        return getDataFolder().toPath().resolve("bestenliste.yml");
    }

    private Leaderboard loadBoard() {
        Path f = boardFile();
        if (Files.isRegularFile(f)) {
            try {
                return Leaderboard.fromYaml(Files.readString(f, StandardCharsets.UTF_8));
            } catch (IOException | IllegalArgumentException e) {
                getLogger().warning("bestenliste.yml ließ sich nicht lesen: " + e.getMessage());
            }
        }
        return new Leaderboard();
    }

    public void saveBoard() {
        store.write(boardFile(), board.toYaml());
        boards.refreshHall();
    }

    private void checkLanguages() {
        for (Player p : getServer().getOnlinePlayers()) {
            String lang = lang(p);
            String before = lastLang.put(p.getUniqueId(), lang);
            if (before != null && !before.equals(lang)) {
                Holo.refresh(this, p, lang);
                if (hall.inHall(p.getUniqueId())) {
                    hall.items(p, lang);
                }
            }
        }
    }

    public void forgetLanguage(UUID uuid) {
        lastLang.remove(uuid);
    }

    /** Offene Menüs (Kramerladen, Kramerkiste) jede Sekunde neu zeichnen – Guthaben, Punkte, Abklingzeiten. */
    private void refreshMenus() {
        for (Player p : getServer().getOnlinePlayers()) {
            if (p.getOpenInventory().getTopInventory().getHolder(false) instanceof Menu menu) {
                menu.refresh();
            }
        }
    }

    // ---------- Zugriff ----------

    public Settings settings() {
        return settings;
    }

    public Network net() {
        return net;
    }

    public Store store() {
        return store;
    }

    public Leaderboard board() {
        return board;
    }

    public World world() {
        return world;
    }

    public Hall hall() {
        return hall;
    }

    public Games games() {
        return games;
    }

    public Boards boards() {
        return boards;
    }

    public Shop shop() {
        return shop;
    }

    public boolean ready() {
        return ready;
    }

    public String lang(Player p) {
        return net.language(p.getUniqueId());
    }

    public PlayerData data(Player p) {
        return store.get(p.getUniqueId(), p.getName());
    }

    public boolean builder(Player p) {
        return p.getGameMode() == org.bukkit.GameMode.CREATIVE && p.hasPermission(ADMIN);
    }

    /** Aus der Gendarmerie einer Runde zuschauen. */
    public void spectate(Player p, Game g) {
        hall.leave(p.getUniqueId());
        g.spectate(p);
    }

    public void stateChanged() {
        if (stateQueued) {
            return;
        }
        stateQueued = true;
        getServer().getScheduler().runTask(this, () -> {
            stateQueued = false;
            reportState(false);
        });
    }

    private void reportState(boolean force) {
        String state = games.all().isEmpty() ? "waiting" : "running";
        int players = getServer().getOnlinePlayers().size();
        if (force || !state.equals(lastState) || players != lastPlayers) {
            lastState = state;
            lastPlayers = players;
            net.state(state, players);
        }
    }

    void rebuild(Runnable done) {
        if (construction.running()) {
            return;
        }
        ready = false;
        games.abortAll();
        construction.ensure(true, () -> {
            ready = true;
            done.run();
        });
        boards.spawnHall();
        kraemer.spawn();
        for (Player p : getServer().getOnlinePlayers()) {
            hall.enter(p);
        }
    }

    void reloadSettings() {
        reloadConfig();
        settings = Settings.load(getConfig());
        for (Player p : getServer().getOnlinePlayers()) {
            if (hall.inHall(p.getUniqueId())) {
                hall.items(p, lang(p));
            }
        }
    }
}
