package de.moinmorni.versteckerl.hall;

import de.moinmorni.versteckerl.Holo;
import de.moinmorni.versteckerl.Items;
import de.moinmorni.versteckerl.Texts;
import de.moinmorni.versteckerl.Ui;
import de.moinmorni.versteckerl.VersteckerlPlugin;
import de.moinmorni.versteckerl.game.Game;
import de.moinmorni.versteckerl.rules.Groups;
import de.moinmorni.versteckerl.rules.HallRules;
import de.moinmorni.versteckerl.rules.Roles;
import de.moinmorni.versteckerl.rules.Votes;
import de.moinmorni.versteckerl.world.HallPlan;
import de.moinmorni.versteckerl.world.MapType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.entity.Pose;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

/**
 * Die Gendarmerie: hier wartet man, stimmt über die Karte ab und sagt „bereit“. Ein Countdown startet die nächste
 * Runde mit höchstens 16 Leuten – Partys bleiben zusammen. Zu Beginn wird ausgelost, wer Dorfgendarm wird.
 */
public final class Hall {

    public static final int SLOT_VOTE = 0;
    public static final int SLOT_READY = 1;
    public static final int SLOT_STATS = 4;
    public static final int SLOT_LOBBY = 8;
    private static final int ALL_READY_SECONDS = 5;

    private final VersteckerlPlugin plugin;
    private final Set<UUID> waiting = new LinkedHashSet<>();
    private final Set<UUID> ready = new HashSet<>();
    private final Map<UUID, UUID> leaders = new HashMap<>();
    private final Votes<MapType> votes = new Votes<>(MapType.class);
    private final List<UUID> preferredSeekers = new ArrayList<>();
    private final Random random = new Random();
    private int countdown = -1;

    public Hall(VersteckerlPlugin plugin) {
        this.plugin = plugin;
    }

    public Location spawn() {
        return new Location(plugin.world(), HallPlan.SPAWN_X, MapType.FLOOR_Y + HallPlan.SPAWN_Y, HallPlan.SPAWN_Z,
                HallPlan.SPAWN_YAW, 0f);
    }

    public boolean inHall(UUID uuid) {
        return waiting.contains(uuid);
    }

    public Set<UUID> waiting() {
        return waiting;
    }

    public Votes<MapType> votes() {
        return votes;
    }

    public int countdown() {
        return countdown;
    }

    public boolean ready(UUID uuid) {
        return ready.contains(uuid);
    }

    /** Verwaltung/Test: diese Leute werden in der nächsten Runde Gendarmen. */
    public void preferSeekers(List<UUID> uuids) {
        preferredSeekers.clear();
        preferredSeekers.addAll(uuids);
    }

    public List<UUID> preferredSeekers() {
        return preferredSeekers;
    }

    /** In die Gendarmerie (Betreten, nach der Runde, /verlassen). */
    public void enter(Player p) {
        String lang = plugin.lang(p);
        p.teleport(spawn());
        p.setGameMode(GameMode.ADVENTURE);
        p.setFlying(false);
        p.setAllowFlight(false);
        p.setInvulnerable(false);
        p.setPose(Pose.STANDING, false);
        p.clearActivePotionEffects();
        var max = p.getAttribute(Attribute.MAX_HEALTH);
        p.setHealth(max == null ? 20 : max.getValue());
        p.setFoodLevel(20);
        p.setFireTicks(0);
        p.setExp(0);
        p.setLevel(0);
        items(p, lang);
        waiting.add(p.getUniqueId());
        ready.remove(p.getUniqueId());
        Holo.refresh(plugin, p, lang);
        plugin.boards().hall(p);
        refreshParty(p.getUniqueId());
    }

    public void items(Player p, String lang) {
        PlayerInventory inv = p.getInventory();
        inv.clear();
        inv.setItem(SLOT_VOTE, Items.hallTag(Items.named(Material.COMPASS, 1, Texts.text(lang, "item.vote"),
                Texts.lines(lang, "item.vote.lore")), "vote"));
        inv.setItem(SLOT_READY, readyItem(p, lang));
        inv.setItem(SLOT_STATS, Items.hallTag(Items.named(Material.PAPER, 1, Texts.text(lang, "item.stats"),
                Texts.lines(lang, "item.stats.lore")), "stats"));
        inv.setItem(SLOT_LOBBY, Items.hallTag(Items.named(Material.RED_BED, 1, Texts.text(lang, "item.lobby"),
                Texts.lines(lang, "item.lobby.lore")), "lobby"));
        inv.setHeldItemSlot(SLOT_VOTE);
    }

    private ItemStack readyItem(Player p, String lang) {
        boolean r = ready.contains(p.getUniqueId());
        return Items.glow(Items.hallTag(Items.named(r ? Material.LIME_DYE : Material.GRAY_DYE, 1,
                Texts.text(lang, "item.ready"), Texts.lines(lang, "item.ready.lore")), "ready"), r);
    }

    public void leave(UUID uuid) {
        waiting.remove(uuid);
        ready.remove(uuid);
        votes.forget(uuid);
        leaders.remove(uuid);
    }

    public void toggleReady(Player p) {
        if (!waiting.contains(p.getUniqueId())) {
            return;
        }
        String lang = plugin.lang(p);
        if (ready.remove(p.getUniqueId())) {
            p.sendMessage(Texts.msg(lang, "hall.not-ready"));
        } else {
            ready.add(p.getUniqueId());
            p.sendMessage(Texts.msg(lang, "hall.ready"));
            Ui.sound(p, "minecraft:block.note_block.pling", 1.4f);
        }
        p.getInventory().setItem(SLOT_READY, readyItem(p, lang));
    }

    public void vote(Player p, MapType map) {
        votes.vote(p.getUniqueId(), map);
        String lang = plugin.lang(p);
        p.sendMessage(Texts.msg(lang, "hall.voted", Placeholder.unparsed("map", Texts.plain(lang, "map." + map.id()))));
        Ui.sound(p, "minecraft:item.book.page_turn", 1f);
        plugin.boards().refreshHall();
    }

    /** Party-Leitung nachschlagen (kommt auf einem anderen Thread zurück). */
    public void refreshParty(UUID uuid) {
        plugin.net().party(uuid).thenAccept(party -> Bukkit.getScheduler().runTask(plugin, () -> {
            if (party.isPresent()) {
                leaders.put(uuid, party.get().leader());
            } else {
                leaders.remove(uuid);
            }
        }));
    }

    /** Jede Sekunde: Countdown, Start. */
    public void tick() {
        // Wer weg ist oder im Zuschauer-/Kreativmodus etwas verwaltet, wird nicht eingeplant (/verlassen holt zurück).
        waiting.removeIf(u -> {
            Player p = Bukkit.getPlayer(u);
            if (p != null && (p.getGameMode() == GameMode.SPECTATOR || p.getGameMode() == GameMode.CREATIVE)) {
                plugin.boards().sidebar().hide(p);
                return true;
            }
            return p == null;
        });
        ready.retainAll(waiting);
        votes.retain(waiting);
        if (!plugin.ready()) {
            return;
        }
        Set<MapType> free = plugin.games().free();
        int n = waiting.size();
        if (free.isEmpty() || !HallRules.countdown(n, ready.size(), plugin.settings().hallMinPlayers())) {
            countdown = -1;
            for (UUID u : waiting) {
                Player p = Bukkit.getPlayer(u);
                if (p != null) {
                    String lang = plugin.lang(p);
                    p.sendActionBar(free.isEmpty() ? Texts.text(lang, "hall.no-map")
                            : Texts.text(lang, "hall.waiting", Placeholder.unparsed("n", String.valueOf(n)),
                                    Placeholder.unparsed("min", String.valueOf(plugin.settings().hallMinPlayers()))));
                }
            }
            return;
        }
        if (countdown < 0) {
            countdown = plugin.settings().hallCountdown();
        }
        if (HallRules.allReady(n, ready.size()) && countdown > ALL_READY_SECONDS) {
            countdown = ALL_READY_SECONDS;
        }
        MapType next = votes.winner(free);
        for (UUID u : waiting) {
            Player p = Bukkit.getPlayer(u);
            if (p != null) {
                String lang = plugin.lang(p);
                p.sendActionBar(Texts.text(lang, "hall.countdown", Placeholder.unparsed("secs", String.valueOf(countdown)),
                        Placeholder.unparsed("map", Texts.plain(lang, "map." + next.id()))));
                if (countdown <= 3 && countdown > 0) {
                    Ui.sound(p, "minecraft:block.note_block.hat", 1.2f);
                }
            }
        }
        if (countdown-- <= 0) {
            start(next);
            countdown = -1;
        }
    }

    /** Runde starten (Countdown abgelaufen oder /versteckerladmin start). */
    public Game start(MapType map) {
        Map<UUID, List<UUID>> groups = new LinkedHashMap<>();
        for (UUID u : waiting) {
            UUID leader = leaders.getOrDefault(u, u);
            if (!waiting.contains(leader)) {
                leader = u;
            }
            groups.computeIfAbsent(leader, k -> new ArrayList<>()).add(u);
        }
        List<UUID> picked = Groups.pick(new ArrayList<>(groups.values()), plugin.settings().maxPlayers());
        List<Player> players = new ArrayList<>();
        for (UUID u : picked) {
            Player p = Bukkit.getPlayer(u);
            if (p != null) {
                players.add(p);
            }
        }
        if (players.size() < HallRules.ABSOLUTE_MIN) {
            return null;
        }
        // Gruppen nur mit denen, die mitspielen
        List<List<UUID>> teams = new ArrayList<>();
        for (List<UUID> g : groups.values()) {
            List<UUID> in = new ArrayList<>(g);
            in.retainAll(picked);
            if (!in.isEmpty()) {
                teams.add(in);
            }
        }
        Map<UUID, Integer> times = new HashMap<>();
        for (Player p : players) {
            times.put(p.getUniqueId(), plugin.data(p).seekerRounds());
        }
        int count = Roles.seekerCount(players.size(), plugin.settings().seekerPercent());
        Set<UUID> seekers = Roles.pickSeekers(teams, count, times, preferredSeekers, random);
        preferredSeekers.clear();
        players.forEach(p -> leave(p.getUniqueId()));
        votes.played(map, System.currentTimeMillis());
        for (Player p : players) {
            String lang = plugin.lang(p);
            p.sendMessage(Texts.msg(lang, "hall.starting", Placeholder.unparsed("map", Texts.plain(lang, "map." + map.id())),
                    Placeholder.unparsed("n", String.valueOf(players.size()))));
        }
        for (UUID u : waiting) {
            Player p = Bukkit.getPlayer(u);
            if (p != null) {
                p.sendMessage(Texts.msg(plugin.lang(p), "hall.full"));
            }
        }
        return plugin.games().start(map, players, seekers);
    }

    /** Kann eine Party-Verstärkung noch in die laufende Runde? */
    public boolean room(Game g) {
        return g.phase() != Game.Phase.OVER && g.playerCount() < plugin.settings().maxPlayers();
    }
}
