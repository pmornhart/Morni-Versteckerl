package net.morni.versteckerl.game;

import net.morni.versteckerl.Holo;
import net.morni.versteckerl.LangBar;
import net.morni.versteckerl.Settings;
import net.morni.versteckerl.Texts;
import net.morni.versteckerl.Ui;
import net.morni.versteckerl.VersteckerlPlugin;
import net.morni.versteckerl.data.Leaderboard;
import net.morni.versteckerl.data.PlayerData;
import net.morni.versteckerl.hall.Shop;
import net.morni.versteckerl.rules.Catalog;
import net.morni.versteckerl.rules.Helps;
import net.morni.versteckerl.rules.Outcome;
import net.morni.versteckerl.rules.Rewards;
import net.morni.versteckerl.rules.Ray;
import net.morni.versteckerl.rules.Settlement;
import net.morni.versteckerl.rules.Shape;
import net.morni.versteckerl.rules.Whistle;
import net.morni.versteckerl.world.MapLayout;
import net.morni.versteckerl.world.MapType;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FluidCollisionMode;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Pose;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

/**
 * Eine Runde Versteckerl auf einer Karte: erst die Versteckzeit (die Versteckten suchen sich ihre Gestalt und einen
 * Platz, die Dorfgendarmen warten in ihrer Stube), dann die Suchzeit. Gefundene werden Gendarmen. Ist bei Ablauf noch
 * jemand versteckt, gewinnen die Versteckten, sonst die Gendarmen.
 *
 * <p>Punkte gibt es nach {@link Rewards}; im Rundenladen ({@link HelpMenu}) kauft man damit kleine Hilfen. Am Ende
 * kommt der Rest mit Serien- und Tagesbonus aufs Guthaben ({@link Settlement}).
 */
public final class Game {

    public enum Phase {
        HIDE,
        SEEK,
        OVER
    }

    private static final int SECOND = 20;
    private static final int BOARD_EVERY = 10;
    private static final double REACH = 4.5;
    private static final double SHAPE_GROW = 0.1;
    private static final int SWING_COOLDOWN = 8;
    private static final int HIT_IMMUNE = 12;
    private static final int GRACE = 8;
    private static final int NO_LOCK_AFTER_HIT = 2 * SECOND;
    private static final double MOVE_EPS = 0.02;
    private static final double RELEASE_DIST = 0.2;
    /** Ab diesem Bruchteil über der Blockunterkante steht man auf etwas fast Vollem (Trampelpfad, Acker). */
    private static final double PATH_HEIGHT = 0.8;
    private static final int HEAL_EVERY = 3 * SECOND;
    private static final int SNIFF_EVERY = 4;
    private static final int WARN_BEFORE = 5 * SECOND;
    private static final int HALF_MINUTE = 30 * SECOND;
    private static final int TICKET_RADIUS = 3;
    private static final int TOP_ROWS = 5;
    /** Die Laterne zeigt, wer sich in den letzten so vielen Ticks bewegt hat. */
    private static final int MOVED_RECENTLY = 10;
    private static final int LANTERN_EVERY = 4;
    private static final int FOG_EVERY = 3;
    private static final int FOG_BLIND = 30;
    /** Der Tagesbonus richtet sich nach dem Tag in Deutschland. */
    private static final ZoneId DAY_ZONE = ZoneId.of("Europe/Berlin");

    private final VersteckerlPlugin plugin;
    private final MapType map;
    private final MapLayout layout;
    private final World world;
    private final Settings settings;
    private final Rewards rewards;
    private final Map<UUID, Member> members = new LinkedHashMap<>();
    private final List<Fog> fogs = new ArrayList<>();
    private final List<UUID> spectators = new ArrayList<>();
    private final LangBar timeBar = new LangBar(BossBar.Color.GREEN, BossBar.Overlay.PROGRESS);
    private final Random random = new Random();
    private final long started = System.currentTimeMillis();
    private Phase phase = Phase.HIDE;
    private int phaseTicks;
    private long seekElapsed;
    private long tick;
    private boolean dutyAnnounced;
    private boolean firstFound;
    private boolean lastHiderPaid;
    private int startHiders;
    private Outcome.Winner winner = Outcome.Winner.NONE;

    /** Eine Nebelwolke aus dem Rundenladen: steht an einer Stelle, bis sie verweht. */
    private record Fog(Location center, long until, int range) {
    }

    Game(VersteckerlPlugin plugin, MapType map, List<Player> players, Set<UUID> seekers) {
        this.plugin = plugin;
        this.map = map;
        this.layout = map.layout();
        this.world = plugin.world();
        this.settings = plugin.settings();
        this.rewards = settings.rewards();
        tickets(true);
        phaseTicks = settings.hideSeconds() * SECOND;
        for (Player p : players) {
            boolean seeker = seekers.contains(p.getUniqueId());
            Member m = new Member(p.getUniqueId(), p.getName(), seeker ? Member.Role.SEEKER : Member.Role.HIDER);
            members.put(p.getUniqueId(), m);
            plugin.store().pin(p.getUniqueId());
            if (seeker) {
                PlayerData d = plugin.data(p);
                d.becameSeeker();
            }
        }
        for (Player p : players) {
            Member m = members.get(p.getUniqueId());
            String lang = plugin.lang(p);
            if (m.hider()) {
                placeHider(p, m);
                Ui.title(p, Texts.text(lang, "start.hider.title"), Texts.text(lang, "start.hider.sub",
                        Placeholder.unparsed("secs", String.valueOf(settings.hideSeconds()))), 3500);
                p.sendMessage(Texts.msg(lang, "start.hider.chat", Placeholder.unparsed("shape",
                        Texts.plain(lang, "shape." + m.disguise.shape().id()))));
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (phase == Phase.HIDE && m.hidden() && p.isOnline()) {
                        ShapeMenu.open(plugin, this, p);
                    }
                }, 10L);
            } else {
                toBooth(p);
                Ui.title(p, Texts.text(lang, "start.seeker.title"), Texts.text(lang, "start.seeker.sub",
                        Placeholder.unparsed("secs", String.valueOf(settings.hideSeconds()))), 3500);
                p.sendMessage(Texts.msg(lang, "start.seeker.chat"));
            }
        }
        startHiders = count(Member.Role.HIDER);
        broadcast(lang -> Texts.msg(lang, "start.roles", Placeholder.unparsed("seekers", names(Member.Role.SEEKER)),
                Placeholder.unparsed("n", String.valueOf(count(Member.Role.HIDER)))));
        applyVisibilityAll();
        updateBar();
    }

    // ---------- Zugriff ----------

    public MapType map() {
        return map;
    }

    public Phase phase() {
        return phase;
    }

    public Member member(UUID uuid) {
        return members.get(uuid);
    }

    public Map<UUID, Member> members() {
        return members;
    }

    public boolean spectating(UUID uuid) {
        return spectators.contains(uuid);
    }

    /** Sekunden, die in der jetzigen Phase noch bleiben. */
    public int secondsLeft() {
        return Math.max(0, (phaseTicks + SECOND - 1) / SECOND);
    }

    public int count(Member.Role role) {
        return (int) members.values().stream().filter(m -> m.online && m.role == role).count();
    }

    public int playerCount() {
        return (int) members.values().stream().filter(m -> m.online).count();
    }

    private String names(Member.Role role) {
        return String.join(", ", members.values().stream().filter(m -> m.online && m.role == role).map(m -> m.name)
                .toList());
    }

    Location loc(MapLayout.Point p, float yaw, float pitch) {
        return new Location(world, map.originX() + p.x(), MapType.FLOOR_Y + p.y(), map.originZ() + p.z(), yaw, pitch);
    }

    private Location hiderSpawn() {
        return loc(layout.hiders(), layout.hidersYaw(), 0f);
    }

    private Location seekerStart() {
        return loc(layout.seekers(), layout.seekersYaw(), 0f);
    }

    private Location booth() {
        return loc(layout.booth(), 0f, 0f);
    }

    private Location view() {
        return loc(layout.view(), 180f, 40f);
    }

    /** Liegt eine Stelle (Welt) auf der Spielfläche dieser Karte? */
    boolean onMap(Location l) {
        return l.getWorld() == world && layout.inside(l.getX() - map.originX(), l.getZ() - map.originZ());
    }

    private void tickets(boolean add) {
        int cx = map.originX() >> 4;
        int cz = map.originZ() >> 4;
        for (int dx = -TICKET_RADIUS; dx <= TICKET_RADIUS; dx++) {
            for (int dz = -TICKET_RADIUS; dz <= TICKET_RADIUS; dz++) {
                if (add) {
                    world.getChunkAt(cx + dx, cz + dz).addPluginChunkTicket(plugin);
                } else {
                    world.getChunkAt(cx + dx, cz + dz).removePluginChunkTicket(plugin);
                }
            }
        }
    }

    /** Für /versteckerladmin status (Tests, Verwaltung). */
    public String status() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format(Locale.ROOT, "karte=%s phase=%s zeit=%d versteckt=%d sucher=%d sieger=%s", map.id(),
                phase, secondsLeft(), count(Member.Role.HIDER), count(Member.Role.SEEKER), winner));
        for (Member m : members.values()) {
            String shape = m.disguise != null ? m.disguise.shape().id() : "-";
            String cell = "-";
            if (m.disguise != null && m.disguise.solid() && m.disguise.cell() != null) {
                Block c = m.disguise.cell();
                cell = c.getX() + "," + c.getY() + "," + c.getZ();
            }
            sb.append(String.format(Locale.ROOT, " %s=%s:%s:%d:%d:%d:%d:%d:%s:%s:%d", m.name, m.role, shape,
                    m.disguise != null && m.disguise.solid() ? 1 : 0, m.lives, m.points, m.finds, m.whistles, cell,
                    m.online ? "on" : "off", m.spent));
        }
        return sb.toString();
    }

    // ---------- Spieler setzen ----------

    private static void reset(Player p, GameMode mode) {
        p.setGameMode(mode);
        p.setFlying(false);
        p.setAllowFlight(false);
        p.setInvulnerable(false);
        p.setPose(Pose.STANDING, false);
        p.clearActivePotionEffects();
        AttributeInstance max = p.getAttribute(Attribute.MAX_HEALTH);
        p.setHealth(max == null ? 20 : max.getValue());
        p.setFoodLevel(20);
        p.setSaturation(5);
        p.setFireTicks(0);
        p.setFallDistance(0);
    }

    private static PotionEffect quiet(PotionEffectType type, int ticks, int level) {
        return new PotionEffect(type, ticks, level, false, false, false);
    }

    private void placeHider(Player p, Member m) {
        String lang = plugin.lang(p);
        p.teleport(hiderSpawn());
        reset(p, GameMode.ADVENTURE);
        p.addPotionEffect(quiet(PotionEffectType.INVISIBILITY, PotionEffect.INFINITE_DURATION, 0));
        m.lives = settings.hitsToFind();
        m.disguise = Disguise.create(plugin, p, startShape(p));
        m.stillSince = tick;
        m.lastPos = p.getLocation();
        Gear.hider(p, lang, false, false, settings.roundShop());
        timeBar.show(p, lang);
        Holo.refresh(plugin, p, lang);
    }

    private void toBooth(Player p) {
        p.teleport(booth());
        reset(p, GameMode.ADVENTURE);
        p.addPotionEffect(quiet(PotionEffectType.BLINDNESS, phaseTicks + 2 * SECOND, 0));
        Gear.none(p);
        timeBar.show(p, plugin.lang(p));
    }

    private void releaseSeeker(Player p, Member m) {
        p.teleport(seekerStart());
        p.removePotionEffect(PotionEffectType.BLINDNESS);
        p.removePotionEffect(PotionEffectType.SLOWNESS);
        Gear.seeker(p, plugin.lang(p), m.sniffUsed, settings.roundShop());
    }

    /** Gestalten, die jemand auf dieser Karte nehmen darf: die freien und die im Kramerladen freigeschalteten. */
    public List<Shape> allowedShapes(Player p) {
        PlayerData d = plugin.data(p);
        Shop shop = plugin.shop();
        return map.shapes().stream().filter(s -> shop.usable(d, s)).toList();
    }

    /** Start-Gestalt: die Lieblingsgestalt, wenn es sie hier gibt, sonst eine zufällige der erlaubten. */
    private Shape startShape(Player p) {
        List<Shape> allowed = allowedShapes(p);
        Catalog.Item fav = plugin.shop().equipped(plugin.data(p), Catalog.Kind.SHAPE);
        Shape favourite = fav == null ? null : Catalog.shape(fav);
        if (favourite != null && allowed.contains(favourite)) {
            return favourite;
        }
        return allowed.get(random.nextInt(allowed.size()));
    }

    /** Nachzügler aus der Party: kommt als Gendarm dazu. */
    public void joinLate(Player p) {
        Member m = members.get(p.getUniqueId());
        if (m == null) {
            m = new Member(p.getUniqueId(), p.getName(), Member.Role.SEEKER);
            members.put(p.getUniqueId(), m);
            plugin.store().pin(p.getUniqueId());
        }
        m.role = Member.Role.SEEKER;
        m.online = true;
        spectators.remove(p.getUniqueId());
        String lang = plugin.lang(p);
        if (phase == Phase.HIDE) {
            toBooth(p);
        } else {
            reset(p, GameMode.ADVENTURE);
            releaseSeeker(p, m);
            timeBar.show(p, lang);
        }
        Holo.refresh(plugin, p, lang);
        Member nm = m;
        broadcast(l -> Texts.msg(l, "game.late", Placeholder.unparsed("player", nm.name)));
        applyVisibilityAll();
    }

    /** Freiwillig gehen (/verlassen) oder Verbindung weg: zählt als gespielt. */
    public void leave(Player p) {
        Member m = members.remove(p.getUniqueId());
        spectators.remove(p.getUniqueId());
        timeBar.hide(p);
        undress(p, m);
        if (m != null) {
            m.online = false;
            report(m, false);
            plugin.store().unpin(p.getUniqueId(), p.isOnline());
            broadcast(l -> Texts.msg(l, "game.left", Placeholder.unparsed("player", m.name)));
        }
    }

    /** Verkleidung, Unsichtbarkeit und Ausrüstung ablegen. */
    private void undress(Player p, Member m) {
        if (m != null && m.disguise != null) {
            m.disguise.remove();
            m.disguise = null;
        }
        p.removePotionEffect(PotionEffectType.INVISIBILITY);
        p.removePotionEffect(PotionEffectType.BLINDNESS);
        p.removePotionEffect(PotionEffectType.JUMP_BOOST);
        p.removePotionEffect(PotionEffectType.SLOWNESS);
        for (Player v : Bukkit.getOnlinePlayers()) {
            v.showEntity(plugin, p);
        }
    }

    public void spectate(Player p) {
        spectators.add(p.getUniqueId());
        p.setGameMode(GameMode.SPECTATOR);
        p.teleport(view());
        String lang = plugin.lang(p);
        timeBar.show(p, lang);
        Holo.refresh(plugin, p, lang);
        applyVisibility(p);
        p.sendMessage(Texts.msg(lang, "spectate.start", Placeholder.unparsed("map", Texts.plain(lang, "map." + map.id()))));
    }

    /** Alle, die gerade etwas von dieser Runde sehen (online). */
    public List<Player> audience() {
        List<Player> out = new ArrayList<>();
        for (Member m : members.values()) {
            Player p = Bukkit.getPlayer(m.uuid);
            if (p != null && m.online) {
                out.add(p);
            }
        }
        for (UUID u : spectators) {
            Player p = Bukkit.getPlayer(u);
            if (p != null) {
                out.add(p);
            }
        }
        return out;
    }

    void broadcast(Function<String, Component> msg) {
        for (Player p : audience()) {
            p.sendMessage(msg.apply(plugin.lang(p)));
        }
    }

    private void soundAll(String key, float pitch) {
        for (Player p : audience()) {
            Ui.sound(p, key, pitch);
        }
    }

    // ---------- Sichtbarkeit ----------

    /**
     * Wer sieht was? Versteckte (die Spieler) sieht nur, wer zuschaut; ihre Anzeige sehen alle – außer wenn sie
     * eingerastet sind (dann steht ein echter Block da und die Anzeige sieht nur der Versteckte selbst).
     */
    public void applyVisibility(Player viewer) {
        boolean spectator = spectators.contains(viewer.getUniqueId());
        for (Member m : members.values()) {
            Player h = Bukkit.getPlayer(m.uuid);
            if (h == null || h == viewer) {
                continue;
            }
            if (m.role == Member.Role.HIDER && phase != Phase.OVER && !spectator) {
                viewer.hideEntity(plugin, h);
            } else {
                viewer.showEntity(plugin, h);
            }
            if (m.disguise != null && m.disguise.display() != null && m.disguise.display().isValid()) {
                if (m.disguise.solid()) {
                    viewer.hideEntity(plugin, m.disguise.display());
                } else {
                    viewer.showEntity(plugin, m.disguise.display());
                }
            }
        }
    }

    public void applyVisibilityAll() {
        for (Player v : Bukkit.getOnlinePlayers()) {
            applyVisibility(v);
        }
    }

    // ---------- Takt ----------

    /** Jeden Tick. Liefert {@code false}, wenn die Runde vorbei und aufgeräumt ist. */
    public boolean tick() {
        tick++;
        if (phase != Phase.OVER && members.values().stream().noneMatch(m -> m.online)) {
            close();
            return false;
        }
        switch (phase) {
            case HIDE -> {
                phaseTicks--;
                tickHiders();
                keepSeekersInBooth();
                hideCues();
                checkEnd(false);
                if (phase == Phase.HIDE && phaseTicks <= 0) {
                    startSeek();
                }
            }
            case SEEK -> {
                phaseTicks--;
                seekElapsed++;
                tickHiders();
                tickSeekers();
                tickFogs();
                whistleDuty();
                if (seekElapsed % HALF_MINUTE == 0) {
                    for (Member m : members.values()) {
                        if (m.hidden()) {
                            m.points += rewards.perHalfMinute();
                        }
                    }
                }
                lastHider();
                checkEnd(phaseTicks <= 0);
            }
            case OVER -> {
                if (--phaseTicks <= 0) {
                    close();
                    return false;
                }
            }
            default -> {
            }
        }
        if (tick % BOARD_EVERY == 0) {
            updateBar();
            actionBars();
            plugin.boards().game(this);
        }
        return true;
    }

    private void hideCues() {
        int left = secondsLeft();
        if (phaseTicks % SECOND != 0) {
            return;
        }
        if (left == 10 || (left <= 3 && left > 0)) {
            for (Player p : audience()) {
                String lang = plugin.lang(p);
                Member m = members.get(p.getUniqueId());
                if (m != null && m.hider()) {
                    p.sendActionBar(Texts.text(lang, "hide.soon", Placeholder.unparsed("secs", String.valueOf(left))));
                }
                Ui.sound(p, "minecraft:block.note_block.hat", left <= 3 ? 1.4f : 1f);
            }
        }
    }

    private void keepSeekersInBooth() {
        Location b = booth();
        for (Member m : members.values()) {
            Player p = Bukkit.getPlayer(m.uuid);
            if (p != null && m.online && m.role == Member.Role.SEEKER
                    && (p.getWorld() != world || p.getLocation().distanceSquared(b) > 4)) {
                p.teleport(b);
            }
        }
    }

    private void startSeek() {
        phase = Phase.SEEK;
        phaseTicks = settings.seekSeconds() * SECOND;
        seekElapsed = 0;
        for (Member m : members.values()) {
            Player p = Bukkit.getPlayer(m.uuid);
            if (p == null || !m.online) {
                continue;
            }
            String lang = plugin.lang(p);
            if (m.role == Member.Role.SEEKER) {
                releaseSeeker(p, m);
                Ui.title(p, Texts.text(lang, "seek.title"), Texts.text(lang, "seek.sub.seeker"), 2500);
            } else {
                Gear.hider(p, lang, true, m.changeUsed, settings.roundShop());
                Ui.title(p, Texts.text(lang, "seek.title"), Texts.text(lang, "seek.sub.hider"), 2500);
            }
        }
        broadcast(lang -> Texts.msg(lang, "seek.chat", Placeholder.unparsed("time", Ui.clock(settings.seekSeconds()))));
        soundAll("minecraft:block.bell.use", 0.8f);
        plugin.stateChanged();
    }

    // ---------- Versteckte: folgen, einrasten ----------

    private void tickHiders() {
        long lockTicks = (long) settings.lockSeconds() * SECOND;
        for (Member m : members.values()) {
            if (!m.hidden() || m.disguise == null) {
                continue;
            }
            Player p = Bukkit.getPlayer(m.uuid);
            if (p == null) {
                continue;
            }
            Location now = p.getLocation();
            if (now.getWorld() != world || now.getY() < MapType.FLOOR_Y - 30 || !onMap(now)) {
                m.disguise.release();
                p.teleport(hiderSpawn());
                now = p.getLocation();
                m.lastPos = now;
                m.stillSince = tick;
                m.disguise.follow(now);
                continue;
            }
            Disguise d = m.disguise;
            if (d.solid()) {
                Location c = d.center();
                double dx = now.getX() - c.getX();
                double dz = now.getZ() - c.getZ();
                double dy = now.getY() - c.getY();
                boolean away = Math.hypot(dx, dz) > RELEASE_DIST || Math.abs(dy) > 0.5;
                if (tick < m.graceUntil) {
                    if (Math.hypot(dx, dz) > 0.05 || Math.abs(dy) > 0.05) {
                        Location back = c.clone();
                        back.setYaw(now.getYaw());
                        back.setPitch(now.getPitch());
                        p.teleport(back);
                    }
                } else if (away) {
                    unlock(p, m);
                    m.stillSince = tick;
                    m.lastMoveTick = tick;
                }
                m.lastPos = p.getLocation();
                continue;
            }
            Location last = m.lastPos;
            boolean moved = last == null || last.getWorld() != now.getWorld()
                    || Math.abs(now.getX() - last.getX()) > MOVE_EPS || Math.abs(now.getZ() - last.getZ()) > MOVE_EPS
                    || Math.abs(now.getY() - last.getY()) > MOVE_EPS;
            if (moved) {
                m.stillSince = tick;
                m.lastMoveTick = tick;
                m.lockHint = false;
                d.follow(now);
            } else if (tick - m.stillSince >= lockTicks && tick >= m.noLockUntil) {
                lock(p, m);
            }
            m.lastPos = now;
        }
    }

    /**
     * Die Zelle, in der jemand einrasten würde: dort, wo die Füße stehen – auf Trampelpfad oder Ackerboden (etwas
     * niedriger als ein Block) die Zelle darüber.
     */
    static Block cellAt(Location feet) {
        Block b = feet.getBlock();
        double frac = feet.getY() - Math.floor(feet.getY());
        return frac >= PATH_HEIGHT ? b.getRelative(BlockFace.UP) : b;
    }

    /** Einrasten versuchen: Zelle frei, fester Boden darunter, niemand sonst drin. */
    private void lock(Player p, Member m) {
        Block cell = cellAt(p.getLocation());
        String lang = plugin.lang(p);
        String why = lockProblem(p, cell);
        if (why != null) {
            if (!m.lockHint) {
                p.sendActionBar(Texts.text(lang, why));
                m.lockHint = true;
            }
            return;
        }
        m.disguise.solidify(cell, p.getLocation().getY());
        m.graceUntil = tick + GRACE;
        Location c = m.disguise.center().clone();
        c.setYaw(p.getLocation().getYaw());
        c.setPitch(p.getLocation().getPitch());
        p.teleport(c);
        m.lastPos = p.getLocation();
        p.sendActionBar(Texts.text(lang, "lock.done"));
        Ui.sound(p, "minecraft:block.wood.place", 0.8f);
    }

    private String lockProblem(Player p, Block cell) {
        if (!onMap(cell.getLocation().add(0.5, 0, 0.5))) {
            return "lock.no";
        }
        boolean free = cell.isEmpty() || cell.isReplaceable() || Tag.WOOL_CARPETS.isTagged(cell.getType());
        if (!free || !cell.getRelative(BlockFace.DOWN).isSolid()) {
            return "lock.no";
        }
        for (Member o : members.values()) {
            if (o != members.get(p.getUniqueId()) && o.disguise != null && o.disguise.solid()
                    && cell.equals(o.disguise.cell())) {
                return "lock.taken";
            }
        }
        BoundingBox box = BoundingBox.of(cell);
        for (Entity e : world.getNearbyEntities(box, e -> e instanceof Player && e != p)) {
            Player o = (Player) e;
            if (o.getGameMode() != GameMode.SPECTATOR) {
                return "lock.taken";
            }
        }
        return null;
    }

    /** Block springt auf (Bewegung oder Treffer). */
    private void unlock(Player p, Member m) {
        if (m.disguise == null || !m.disguise.solid()) {
            return;
        }
        m.disguise.release();
        m.disguise.follow(p.getLocation());
        applyVisibilityAll();
        Ui.sound(p, "minecraft:block.wood.break", 1.2f);
    }

    /** Neue Gestalt aus dem Menü. */
    public void chooseShape(Player p, Shape s) {
        Member m = members.get(p.getUniqueId());
        if (m == null || !m.hidden() || m.disguise == null || phase == Phase.OVER || !map.shapes().contains(s)) {
            return;
        }
        String lang = plugin.lang(p);
        if (!allowedShapes(p).contains(s)) {
            p.sendMessage(Texts.msg(lang, "shape.locked", Placeholder.unparsed("shape", Texts.plain(lang, "shape." + s.id()))));
            return;
        }
        if (m.disguise.shape() == s) {
            return;
        }
        if (phase == Phase.SEEK) {
            if (m.changeUsed) {
                p.sendMessage(Texts.msg(lang, "shape.change.used"));
                return;
            }
            m.changeUsed = true;
            Gear.hider(p, lang, true, true, settings.roundShop());
        }
        m.disguise.shape(s);
        p.sendMessage(Texts.msg(lang, "shape.chosen", Placeholder.unparsed("shape", Texts.plain(lang, "shape." + s.id()))));
        Ui.sound(p, "minecraft:entity.illusioner.mirror_move", 1.2f);
        world.spawnParticle(Particle.CLOUD, p.getLocation().add(0, 0.5, 0), 8, 0.3, 0.3, 0.3, 0.01);
    }

    /** Gestalt-Menü öffnen (Knopf oder /gestalt). */
    public void openShapes(Player p) {
        Member m = members.get(p.getUniqueId());
        String lang = plugin.lang(p);
        if (m == null || !m.hidden()) {
            p.sendMessage(Texts.msg(lang, "shape.only-hiders"));
            return;
        }
        if (phase == Phase.SEEK && m.changeUsed) {
            p.sendMessage(Texts.msg(lang, "shape.change.used"));
            return;
        }
        ShapeMenu.open(plugin, this, p);
    }

    // ---------- Hilfen der Versteckten ----------

    /**
     * Pfiff: der eigene Pfiff-Klang und Noten an der eigenen Stelle. Freiwillig gibt es Punkte: je später, je lauter der
     * Klang und je näher ein Gendarm steht (Mut), desto mehr.
     */
    public void whistle(Player p, boolean forced) {
        Member m = members.get(p.getUniqueId());
        if (m == null || !m.hidden()) {
            return;
        }
        String lang = plugin.lang(p);
        if (phase != Phase.SEEK) {
            p.sendActionBar(Texts.text(lang, "whistle.not-yet"));
            return;
        }
        if (!forced && tick < m.whistleReady) {
            p.sendActionBar(Texts.text(lang, "whistle.cooldown",
                    Placeholder.unparsed("secs", String.valueOf((m.whistleReady - tick + SECOND - 1) / SECOND))));
            return;
        }
        Location at = m.disguise != null && m.disguise.solid() ? m.disguise.center() : p.getLocation();
        Catalog.Item sound = plugin.shop().equipped(plugin.data(p), Catalog.Kind.WHISTLE);
        int loudness = sound == null ? Catalog.QUIET : Math.max(Catalog.QUIET, sound.loudness());
        Flair.whistle(plugin, at, sound == null ? "floete" : sound.id(), loudness);
        m.lastWhistle = seekElapsed;
        m.whistleReady = tick + (long) settings.whistleCooldown() * SECOND;
        m.warned = false;
        if (forced) {
            p.sendMessage(Texts.msg(lang, "whistle.forced"));
            return;
        }
        double nearest = nearestSeeker(at);
        int courage = rewards.courage(nearest);
        int loud = rewards.loudness(loudness);
        int pts = rewards.whistle(seekElapsed / SECOND, loudness, nearest);
        m.points += pts;
        m.whistles++;
        List<String> extra = new ArrayList<>();
        if (courage > 0) {
            extra.add(Texts.raw(lang, "whistle.brave").replace("<n>", String.valueOf(courage)));
        }
        if (loud > 0) {
            extra.add(Texts.raw(lang, "whistle.loud").replace("<n>", String.valueOf(loud)));
        }
        String bonus = extra.isEmpty() ? "" : " <gray>(" + String.join(" · ", extra) + ")</gray>";
        p.sendActionBar(Texts.text(lang, "whistle.done", Placeholder.unparsed("n", String.valueOf(pts)),
                Placeholder.parsed("extra", bonus)));
    }

    /** Abstand zum nächsten Gendarmen (in Blöcken; keiner da = unendlich). */
    private double nearestSeeker(Location at) {
        double best = Double.POSITIVE_INFINITY;
        for (Member s : members.values()) {
            Player sp = Bukkit.getPlayer(s.uuid);
            if (s.online && s.role == Member.Role.SEEKER && sp != null && sp.getWorld() == world) {
                best = Math.min(best, sp.getLocation().distance(at));
            }
        }
        return best;
    }

    /** Wer als Letzter noch versteckt ist, bekommt einmal Extrapunkte (bei mindestens zwei Versteckten am Anfang). */
    private void lastHider() {
        if (lastHiderPaid || startHiders < 2 || count(Member.Role.HIDER) != 1) {
            return;
        }
        lastHiderPaid = true;
        for (Member m : members.values()) {
            Player p = Bukkit.getPlayer(m.uuid);
            if (m.hidden() && p != null) {
                m.points += rewards.lastHider();
                String lang = plugin.lang(p);
                Ui.title(p, Component.empty(), Texts.text(lang, "last.hider", Placeholder.unparsed("n",
                        String.valueOf(rewards.lastHider()))), 2500);
                Ui.sound(p, "minecraft:block.note_block.pling", 1.6f);
                Member last = m;
                broadcast(l -> Texts.msg(l, "last.chat", Placeholder.unparsed("player", last.name)));
            }
        }
    }

    /** Pflicht-Pfiff: ab einem Teil der Suchzeit muss regelmäßig gepfiffen werden. */
    private void whistleDuty() {
        long total = (long) settings.seekSeconds() * SECOND;
        long from = Whistle.mandatoryFrom(total, settings.whistleMandatoryPercent());
        long interval = (long) settings.whistleInterval() * SECOND;
        if (!dutyAnnounced && seekElapsed >= from) {
            dutyAnnounced = true;
            broadcast(lang -> Texts.msg(lang, "whistle.duty", Placeholder.unparsed("secs",
                    String.valueOf(settings.whistleInterval()))));
        }
        for (Member m : members.values()) {
            if (!m.hidden()) {
                continue;
            }
            long due = Whistle.dueIn(seekElapsed, from, m.lastWhistle, interval);
            if (due < 0) {
                continue;
            }
            Player p = Bukkit.getPlayer(m.uuid);
            if (p == null) {
                continue;
            }
            if (due == 0) {
                whistle(p, true);
            } else if (due <= WARN_BEFORE && !m.warned) {
                m.warned = true;
                String lang = plugin.lang(p);
                Ui.title(p, Component.empty(), Texts.text(lang, "whistle.warn",
                        Placeholder.unparsed("secs", String.valueOf((due + SECOND - 1) / SECOND))), 1500);
                Ui.sound(p, "minecraft:block.note_block.bit", 0.7f);
            }
        }
    }

    /** Sprungkraft: kurz höher springen. */
    public void jump(Player p) {
        Member m = members.get(p.getUniqueId());
        if (m == null || !m.hidden()) {
            return;
        }
        String lang = plugin.lang(p);
        if (tick < m.jumpReady) {
            p.sendActionBar(Texts.text(lang, "jump.cooldown",
                    Placeholder.unparsed("secs", String.valueOf((m.jumpReady - tick + SECOND - 1) / SECOND))));
            return;
        }
        m.jumpReady = tick + (long) settings.jumpCooldown() * SECOND;
        p.addPotionEffect(quiet(PotionEffectType.JUMP_BOOST, settings.jumpSeconds() * SECOND, 2));
        p.sendActionBar(Texts.text(lang, "jump.done", Placeholder.unparsed("secs", String.valueOf(settings.jumpSeconds()))));
        Ui.sound(p, "minecraft:entity.rabbit.jump", 1f);
    }

    // ---------- Gendarmen ----------

    private void tickSeekers() {
        for (Member m : members.values()) {
            if (!m.online || m.role != Member.Role.SEEKER) {
                continue;
            }
            Player p = Bukkit.getPlayer(m.uuid);
            if (p == null) {
                continue;
            }
            Location now = p.getLocation();
            if (now.getWorld() != world || now.getY() < MapType.FLOOR_Y - 30 || !onMap(now)) {
                p.teleport(seekerStart());
            }
            if (m.stunnedUntil > 0 && tick >= m.stunnedUntil) {
                m.stunnedUntil = 0;
                p.removePotionEffect(PotionEffectType.SLOWNESS);
                p.sendActionBar(Texts.text(plugin.lang(p), "wrong.recovered"));
            }
            if (tick % HEAL_EVERY == 0) {
                AttributeInstance max = p.getAttribute(Attribute.MAX_HEALTH);
                double top = max == null ? 20 : max.getValue();
                p.setHealth(Math.min(top, p.getHealth() + 1));
            }
            if (m.sniffTarget != null && tick < m.sniffUntil && tick % SNIFF_EVERY == 0) {
                sniffTrail(p, m);
            }
            if (tick < m.lanternUntil && tick % LANTERN_EVERY == 0) {
                lanternGlow(p);
            }
        }
    }

    /** Spürnase: einmal pro Runde kurz eine Spur in Richtung des nächsten Versteckten (nur für sich selbst). */
    public void sniff(Player p) {
        Member m = members.get(p.getUniqueId());
        if (m == null || m.role != Member.Role.SEEKER || !m.online) {
            return;
        }
        String lang = plugin.lang(p);
        if (phase != Phase.SEEK) {
            p.sendActionBar(Texts.text(lang, "sniff.not-yet"));
            return;
        }
        if (m.sniffUsed) {
            p.sendMessage(Texts.msg(lang, "sniff.used"));
            return;
        }
        Member target = null;
        double best = Double.MAX_VALUE;
        for (Member h : members.values()) {
            Player hp = Bukkit.getPlayer(h.uuid);
            if (!h.hidden() || hp == null) {
                continue;
            }
            double d = hp.getLocation().distanceSquared(p.getLocation());
            if (d < best) {
                best = d;
                target = h;
            }
        }
        if (target == null) {
            p.sendMessage(Texts.msg(lang, "sniff.nothing"));
            return;
        }
        m.sniffUsed = true;
        m.sniffTarget = target.uuid;
        m.sniffUntil = tick + (long) settings.sniffSeconds() * SECOND;
        Gear.seeker(p, lang, true, settings.roundShop());
        long rough = Math.max(5, Math.round(Math.sqrt(best) / 5.0) * 5);
        p.sendMessage(Texts.msg(lang, "sniff.start", Placeholder.unparsed("n", String.valueOf(rough))));
        Ui.sound(p, "minecraft:entity.fox.sniff", 1f);
        sniffTrail(p, m);
    }

    private void sniffTrail(Player p, Member m) {
        Player target = Bukkit.getPlayer(m.sniffTarget);
        Member t = members.get(m.sniffTarget);
        if (target == null || t == null || !t.hidden()) {
            m.sniffUntil = 0;
            return;
        }
        Location from = p.getEyeLocation().subtract(0, 0.4, 0);
        Vector dir = target.getLocation().add(0, 0.5, 0).toVector().subtract(from.toVector());
        double len = dir.length();
        if (len < 0.5) {
            return;
        }
        dir.multiply(1 / len);
        Particle.DustOptions dust = new Particle.DustOptions(Color.fromRGB(0xF2B632), 1.2f);
        for (double d = 1.2; d <= Math.min(7, len); d += 0.6) {
            Location at = from.clone().add(dir.clone().multiply(d));
            p.spawnParticle(Particle.DUST, at, 1, 0, 0, 0, 0, dust);
        }
    }

    /** Schlag eines Gendarmen (Arm-Schwung): trifft er eine Gestalt, einen eingerasteten Block oder eine Attrappe? */
    public void swing(Player p) {
        Member m = members.get(p.getUniqueId());
        if (m == null || m.role != Member.Role.SEEKER || !m.online || phase != Phase.SEEK) {
            return;
        }
        if (tick < m.stunnedUntil) {
            p.sendActionBar(Texts.text(plugin.lang(p), "wrong.stunned"));
            return;
        }
        if (tick - m.lastSwing < SWING_COOLDOWN || tick - m.lastAttack <= 2) {
            return;
        }
        m.lastSwing = tick;
        Location eye = p.getEyeLocation();
        Vector dir = eye.getDirection();
        RayTraceResult hitBlock = world.rayTraceBlocks(eye, dir, REACH, FluidCollisionMode.NEVER, true);
        double tBlock = hitBlock == null ? Ray.MISS : hitBlock.getHitPosition().distance(eye.toVector());
        Member best = null;
        double bestT = Ray.MISS;
        for (Member h : members.values()) {
            if (!h.hidden() || h.disguise == null || h.disguise.solid()) {
                continue;
            }
            Player hp = Bukkit.getPlayer(h.uuid);
            if (hp == null) {
                continue;
            }
            Location f = hp.getLocation();
            double t = Ray.shape(eye.getX(), eye.getY(), eye.getZ(), dir.getX(), dir.getY(), dir.getZ(), f.getX(),
                    f.getY(), f.getZ(), SHAPE_GROW, REACH);
            if (t < bestT) {
                bestT = t;
                best = h;
            }
        }
        if (best != null && bestT <= tBlock + 0.05) {
            hit(best, p, m);
            return;
        }
        if (hitBlock == null || hitBlock.getHitBlock() == null) {
            return;
        }
        Block b = hitBlock.getHitBlock();
        for (Member h : members.values()) {
            if (h.hidden() && h.disguise != null && h.disguise.solid() && b.equals(h.disguise.cell())) {
                hit(h, p, m);
                return;
            }
        }
        if (map.decoy(b.getType().getKey().getKey())) {
            wrong(p, m, b);
        }
    }

    /** Ein Gendarm schlägt einen (für ihn eigentlich unsichtbaren) Versteckten direkt. */
    public void attack(Player seeker, Player hider) {
        Member m = members.get(seeker.getUniqueId());
        Member h = members.get(hider.getUniqueId());
        if (m == null || h == null || m.role != Member.Role.SEEKER || !h.hidden() || phase != Phase.SEEK
                || tick < m.stunnedUntil || tick - m.lastSwing < SWING_COOLDOWN) {
            return;
        }
        m.lastAttack = tick;
        m.lastSwing = tick;
        hit(h, seeker, m);
    }

    private void hit(Member h, Player seeker, Member s) {
        if (tick < h.immuneUntil) {
            return;
        }
        Player hp = Bukkit.getPlayer(h.uuid);
        if (hp == null) {
            return;
        }
        h.immuneUntil = tick + HIT_IMMUNE;
        h.lives--;
        s.hits++;
        s.points += rewards.hit();
        Location at = h.disguise != null && h.disguise.solid() ? h.disguise.center() : hp.getLocation();
        world.spawnParticle(Particle.CRIT, at.clone().add(0, 0.6, 0), 12, 0.3, 0.3, 0.3, 0.1);
        world.playSound(Sound.sound(Key.key("minecraft:entity.player.hurt"), Sound.Source.PLAYER, 1f, 1.3f),
                at.getX(), at.getY(), at.getZ());
        if (h.lives <= 0) {
            found(h, hp, s, seeker);
            return;
        }
        unlock(hp, h);
        h.noLockUntil = tick + NO_LOCK_AFTER_HIT;
        h.stillSince = tick;
        String hl = plugin.lang(hp);
        hp.sendActionBar(Texts.text(hl, "hit.hider", Placeholder.parsed("lives", hearts(h.lives))));
        hp.playHurtAnimation(0f);
        seeker.sendActionBar(Texts.text(plugin.lang(seeker), "hit.seeker", Placeholder.unparsed("n",
                String.valueOf(h.lives))));
        Ui.sound(seeker, "minecraft:entity.experience_orb.pickup", 1.4f);
    }

    /** Gefunden: wird Gendarm. */
    private void found(Member h, Player hp, Member s, Player seeker) {
        Shape shape = h.disguise != null ? h.disguise.shape() : null;
        Location where = h.disguise != null && h.disguise.solid() ? h.disguise.center() : hp.getLocation();
        s.finds++;
        s.points += rewards.find();
        if (!firstFound) {
            firstFound = true;
            s.points += rewards.firstFind();
            seeker.sendMessage(Texts.msg(plugin.lang(seeker), "found.first", Placeholder.unparsed("n",
                    String.valueOf(rewards.firstFind()))));
        }
        undress(hp, h);
        h.role = Member.Role.SEEKER;
        reset(hp, GameMode.ADVENTURE);
        Gear.seeker(hp, plugin.lang(hp), h.sniffUsed, settings.roundShop());
        String shapeId = shape == null ? "fass" : shape.id();
        broadcast(lang -> Texts.msg(lang, "found.chat", Placeholder.unparsed("seeker", s.name),
                Placeholder.unparsed("hider", h.name), Placeholder.unparsed("shape", Texts.plain(lang, "shape." + shapeId)),
                Placeholder.unparsed("n", String.valueOf(count(Member.Role.HIDER)))));
        String hl = plugin.lang(hp);
        Ui.title(hp, Texts.text(hl, "found.title"), Texts.text(hl, "found.sub"), 2500);
        Ui.title(seeker, Component.empty(), Texts.text(plugin.lang(seeker), "found.seeker",
                Placeholder.unparsed("hider", h.name)), 1500);
        soundAll("minecraft:entity.player.levelup", 1.6f);
        Flair.find(where, plugin.shop().equippedId(plugin.data(seeker), Catalog.Kind.FIND));
        applyVisibilityAll();
        plugin.stateChanged();
    }

    /** Falscher Block: kostet Leben; ist die Puste aus, muss der Gendarm kurz verschnaufen. */
    private void wrong(Player p, Member m, Block b) {
        String lang = plugin.lang(p);
        double dmg = settings.wrongDamage();
        if (dmg <= 0) {
            return;
        }
        Ui.sound(p, "minecraft:block.wood.hit", 0.6f);
        p.playHurtAnimation(0f);
        if (p.getHealth() - dmg <= 0.5) {
            AttributeInstance max = p.getAttribute(Attribute.MAX_HEALTH);
            p.setHealth(max == null ? 20 : max.getValue());
            m.stunnedUntil = tick + (long) settings.stunSeconds() * SECOND;
            p.addPotionEffect(quiet(PotionEffectType.SLOWNESS, settings.stunSeconds() * SECOND, 3));
            Ui.title(p, Texts.text(lang, "wrong.stun.title"), Texts.text(lang, "wrong.stun.sub",
                    Placeholder.unparsed("secs", String.valueOf(settings.stunSeconds()))), 2000);
            return;
        }
        p.setHealth(p.getHealth() - dmg);
        p.sendActionBar(Texts.text(lang, "wrong.hit", Placeholder.unparsed("thing", thingName(lang, b))));
    }

    /** Name der Gestalt, zu der ein Block gehört (für „Das war nur ein echtes Fass“). */
    private String thingName(String lang, Block b) {
        String name = b.getType().getKey().getKey();
        for (Shape s : map.shapes()) {
            if (s.sameKind(name)) {
                return Texts.plain(lang, "shape." + s.id());
            }
        }
        return name;
    }

    private static String hearts(int lives) {
        return "<red>" + "♥".repeat(Math.max(0, lives)) + "</red>";
    }

    // ---------- Rundenladen ----------

    /** Kramerkiste öffnen (Knopf oder /laden in der Runde). */
    public void openHelps(Player p) {
        Member m = members.get(p.getUniqueId());
        if (m == null || !m.online || phase == Phase.OVER) {
            return;
        }
        if (!settings.roundShop()) {
            p.sendMessage(Texts.msg(plugin.lang(p), "help.closed"));
            return;
        }
        HelpMenu.open(plugin, this, p);
    }

    /** Kann jemand die Hilfe jetzt kaufen? (für das Menü) */
    public Helps.Verdict helpVerdict(Player p, Helps.Help h) {
        Member m = members.get(p.getUniqueId());
        if (m == null || !m.online || !settings.roundShop()) {
            return Helps.Verdict.OFF;
        }
        boolean needed = switch (h) {
            case GESTALTWECHSEL -> m.changeUsed;
            case SPUERNASE -> m.sniffUsed;
            default -> true;
        };
        long cooldown = Math.max(0, m.helpReady.getOrDefault(h, 0L) - tick);
        return Helps.check(h, settings.help(h), m.role == Member.Role.HIDER, phase == Phase.SEEK, needed,
                m.available(), m.uses(h), cooldown);
    }

    /** Sekunden, bis eine Hilfe wieder geht (0 = bereit). */
    public int helpCooldown(UUID uuid, Helps.Help h) {
        Member m = members.get(uuid);
        if (m == null) {
            return 0;
        }
        long left = m.helpReady.getOrDefault(h, 0L) - tick;
        return left <= 0 ? 0 : (int) ((left + SECOND - 1) / SECOND);
    }

    /** Hilfe kaufen und gleich benutzen (bezahlt mit Punkten der Runde). */
    public void help(Player p, Helps.Help h) {
        Member m = members.get(p.getUniqueId());
        if (m == null) {
            return;
        }
        String lang = plugin.lang(p);
        Helps.Verdict v = helpVerdict(p, h);
        Helps.Rule rule = settings.help(h);
        if (v != Helps.Verdict.OK) {
            p.sendMessage(Texts.msg(lang, "help.no." + v.name().toLowerCase(Locale.ROOT).replace('_', '-'),
                    Placeholder.unparsed("help", Texts.plain(lang, "help." + h.id())),
                    Placeholder.unparsed("n", String.valueOf(rule == null ? 0 : rule.price() - m.available())),
                    Placeholder.unparsed("secs", String.valueOf(helpCooldown(p.getUniqueId(), h)))));
            Ui.sound(p, "minecraft:entity.villager.no", 1.2f);
            return;
        }
        m.spent += rule.price();
        m.helpsBought++;
        m.helpUses.merge(h, 1, Integer::sum);
        m.helpReady.put(h, tick + (long) rule.cooldown() * SECOND);
        p.sendMessage(Texts.msg(lang, "help.bought", Placeholder.unparsed("help", Texts.plain(lang, "help." + h.id())),
                Placeholder.unparsed("n", String.valueOf(rule.price())),
                Placeholder.unparsed("left", String.valueOf(m.available()))));
        switch (h) {
            case LOCKPFIFF -> decoyWhistle(p, m, rule);
            case NEBELWOLKE -> fog(p, m, rule);
            case GESTALTWECHSEL -> {
                m.changeUsed = false;
                Gear.hider(p, lang, true, false, settings.roundShop());
                ShapeMenu.open(plugin, this, p);
            }
            case LATERNE -> lantern(p, m, rule);
            case SPUERNASE -> {
                m.sniffUsed = false;
                Gear.seeker(p, lang, false, settings.roundShop());
                sniff(p);
            }
            default -> {
            }
        }
    }

    /** Lockpfiff: der eigene Pfiff erklingt ein Stück weiter weg (auf der Karte). */
    private void decoyWhistle(Player p, Member m, Helps.Rule rule) {
        Location from = m.disguise != null && m.disguise.solid() ? m.disguise.center() : p.getLocation();
        Location spot = null;
        for (int i = 0; i < 12 && spot == null; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double dist = rule.range() * (0.6 + random.nextDouble() * 0.4);
            Location l = from.clone().add(Math.cos(angle) * dist, 0, Math.sin(angle) * dist);
            if (onMap(l)) {
                spot = l;
            }
        }
        if (spot == null) {
            spot = loc(layout.hiders(), 0f, 0f);
        }
        Catalog.Item sound = plugin.shop().equipped(plugin.data(p), Catalog.Kind.WHISTLE);
        Flair.whistle(plugin, spot, sound == null ? "floete" : sound.id(),
                sound == null ? Catalog.QUIET : Math.max(Catalog.QUIET, sound.loudness()));
        p.sendActionBar(Texts.text(plugin.lang(p), "help.lockpfiff.done"));
    }

    /** Nebelwolke an der eigenen Stelle: Gendarmen darin sehen kurz nichts und werden langsam. */
    private void fog(Player p, Member m, Helps.Rule rule) {
        Location c = m.disguise != null && m.disguise.solid() ? m.disguise.center() : p.getLocation();
        fogs.add(new Fog(c.clone(), tick + (long) rule.seconds() * SECOND, rule.range()));
        world.playSound(Sound.sound(Key.key("minecraft:block.fire.extinguish"), Sound.Source.PLAYER, 1.5f, 0.6f),
                c.getX(), c.getY(), c.getZ());
        for (Member s : members.values()) {
            Player sp = Bukkit.getPlayer(s.uuid);
            if (s.online && s.role == Member.Role.SEEKER && sp != null && sp.getWorld() == world
                    && sp.getLocation().distance(c) <= rule.range() + 1) {
                sp.addPotionEffect(quiet(PotionEffectType.BLINDNESS, FOG_BLIND, 0));
                sp.sendActionBar(Texts.text(plugin.lang(sp), "help.nebelwolke.seeker"));
            }
        }
        p.sendActionBar(Texts.text(plugin.lang(p), "help.nebelwolke.done"));
    }

    private void tickFogs() {
        for (Iterator<Fog> it = fogs.iterator(); it.hasNext();) {
            Fog f = it.next();
            if (tick >= f.until()) {
                it.remove();
                continue;
            }
            if (tick % FOG_EVERY != 0) {
                continue;
            }
            Location c = f.center();
            world.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, c.clone().add(0, 0.6, 0), 6, f.range() * 0.4, 0.5,
                    f.range() * 0.4, 0.004);
            world.spawnParticle(Particle.CLOUD, c.clone().add(0, 1, 0), 10, f.range() * 0.45, 0.6, f.range() * 0.45, 0.01);
            for (Member s : members.values()) {
                Player sp = Bukkit.getPlayer(s.uuid);
                if (s.online && s.role == Member.Role.SEEKER && sp != null && sp.getWorld() == world
                        && sp.getLocation().distance(c) <= f.range()) {
                    sp.addPotionEffect(quiet(PotionEffectType.SLOWNESS, FOG_EVERY * 3, 1));
                }
            }
        }
    }

    /** Laterne: ein paar Sekunden zeigen Funken, wer sich in der Nähe bewegt; Versteckte dort bekommen einen Hinweis. */
    private void lantern(Player p, Member m, Helps.Rule rule) {
        m.lanternUntil = tick + (long) rule.seconds() * SECOND;
        Location c = p.getLocation();
        world.playSound(Sound.sound(Key.key("minecraft:block.lantern.place"), Sound.Source.PLAYER, 1.5f, 1.2f),
                c.getX(), c.getY(), c.getZ());
        for (Member h : members.values()) {
            Player hp = Bukkit.getPlayer(h.uuid);
            if (h.hidden() && hp != null && hp.getWorld() == world && hp.getLocation().distance(c) <= rule.range()) {
                Ui.title(hp, Component.empty(), Texts.text(plugin.lang(hp), "help.laterne.hider"), 1500);
            }
        }
        p.sendActionBar(Texts.text(plugin.lang(p), "help.laterne.done", Placeholder.unparsed("secs",
                String.valueOf(rule.seconds()))));
    }

    private void lanternGlow(Player seeker) {
        Helps.Rule rule = settings.help(Helps.Help.LATERNE);
        int range = rule == null ? 16 : rule.range();
        Location eye = seeker.getLocation();
        for (Member h : members.values()) {
            Player hp = Bukkit.getPlayer(h.uuid);
            if (!h.hidden() || hp == null || h.disguise == null || h.disguise.solid()
                    || tick - h.lastMoveTick > MOVED_RECENTLY || hp.getWorld() != world
                    || hp.getLocation().distance(eye) > range) {
                continue;
            }
            Location at = hp.getLocation().add(0, 0.5, 0);
            seeker.spawnParticle(Particle.WAX_ON, at, 6, 0.35, 0.4, 0.35, 0);
            seeker.spawnParticle(Particle.END_ROD, at.clone().add(0, 0.9, 0), 2, 0.1, 0.1, 0.1, 0.01);
        }
    }

    // ---------- Ende ----------

    private void checkEnd(boolean timeUp) {
        if (phase == Phase.OVER) {
            return;
        }
        Outcome.Winner w = Outcome.decide(count(Member.Role.HIDER), count(Member.Role.SEEKER), timeUp);
        if (w != Outcome.Winner.NONE) {
            gameOver(w);
        }
    }

    /** Verwaltung: Runde sofort beenden (wer gerade vorn liegt, gewinnt). */
    public void forceEnd() {
        if (phase == Phase.OVER) {
            return;
        }
        gameOver(count(Member.Role.HIDER) > 0 ? Outcome.Winner.HIDERS : Outcome.Winner.SEEKERS);
    }

    /** Verwaltung/Test: Restzeit der Phase setzen. */
    public void forceTime(int seconds) {
        if (phase != Phase.OVER) {
            phaseTicks = Math.max(1, seconds * SECOND);
        }
    }

    private void gameOver(Outcome.Winner w) {
        phase = Phase.OVER;
        winner = w;
        fogs.clear();
        phaseTicks = settings.afterSeconds() * SECOND;
        for (Member m : members.values()) {
            if (!m.online) {
                continue;
            }
            if (w == Outcome.Winner.HIDERS && m.role == Member.Role.HIDER) {
                m.survived = true;
                m.points += rewards.survive();
            } else if (w == Outcome.Winner.SEEKERS && m.role == Member.Role.SEEKER) {
                m.points += rewards.seekersWin();
            }
        }
        // Aufdecken: wer noch versteckt ist, leuchtet
        for (Member m : members.values()) {
            if (m.disguise != null) {
                m.disguise.release();
                m.disguise.glow();
            }
        }
        applyVisibilityAll();
        List<Member> ranking = new ArrayList<>(members.values());
        ranking.sort(Comparator.comparingInt((Member m) -> m.points).reversed());
        String head = w == Outcome.Winner.HIDERS ? "over.hiders" : "over.seekers";
        for (Player p : audience()) {
            String lang = plugin.lang(p);
            Member me = members.get(p.getUniqueId());
            boolean won = me != null && won(me);
            Ui.title(p, Texts.text(lang, head), me == null ? Component.empty()
                    : Texts.text(lang, won ? "over.you-won" : "over.you-lost"), 3500);
            p.sendMessage(Texts.msg(lang, head + ".chat", Placeholder.unparsed("names",
                    String.join(", ", members.values().stream().filter(this::won).map(m -> m.name).toList()))));
            for (int i = 0; i < Math.min(TOP_ROWS, ranking.size()); i++) {
                Member r = ranking.get(i);
                String title = titleOf(r, lang);
                p.sendMessage(Texts.text(lang, "over.row", Placeholder.unparsed("pos", String.valueOf(i + 1)),
                        Placeholder.unparsed("name", r.name), Placeholder.unparsed("title", title.isEmpty() ? ""
                                : " " + title), Placeholder.unparsed("n", String.valueOf(r.points))));
            }
            if (me != null) {
                p.sendMessage(Texts.msg(lang, "over.mine", Placeholder.unparsed("n", String.valueOf(me.points))));
            }
            p.sendMessage(Texts.msg(lang, "over.back", Placeholder.unparsed("secs", String.valueOf(settings.afterSeconds()))));
            Ui.sound(p, won ? "minecraft:ui.toast.challenge_complete" : "minecraft:entity.villager.no", 1f);
            if (won) {
                Flair.win(plugin, p, plugin.shop().equippedId(plugin.data(p), Catalog.Kind.WIN));
            }
        }
        for (Member m : members.values()) {
            report(m, true);
        }
        plugin.saveBoard();
        plugin.getLogger().info("Versteckerl " + map.id() + ": " + w + " nach " + (System.currentTimeMillis() - started) / 1000
                + " s (" + members.values().stream().map(m -> m.name + "=" + m.points).toList() + ").");
        plugin.stateChanged();
    }

    private boolean won(Member m) {
        return (winner == Outcome.Winner.HIDERS && m.role == Member.Role.HIDER && m.online)
                || (winner == Outcome.Winner.SEEKERS && m.role == Member.Role.SEEKER && m.online);
    }

    /**
     * Statistik melden (Netzwerk + lokal + Steckbriefe) und abrechnen: Rest der Runde, Serien- und Tagesbonus aufs
     * Guthaben. Wer vorher geht, zählt als nicht gewonnen (die Serie endet), behält aber, was er gesammelt hat.
     */
    private void report(Member m, boolean tellBest) {
        boolean winH = winner == Outcome.Winner.HIDERS && m.role == Member.Role.HIDER && m.online;
        boolean winS = winner == Outcome.Winner.SEEKERS && m.role == Member.Role.SEEKER && m.online;
        PlayerData data = plugin.store().get(m.uuid, m.name);
        boolean best = data.addRound(new PlayerData.Round(winH, winS, m.finds, m.survived, m.whistles, m.points));
        String today = LocalDate.now(DAY_ZONE).toString();
        Settlement bill = Settlement.of(m.points, m.spent, winH || winS, data.streak(), data.dailyOpen(today), rewards);
        data.settle(bill, today, m.helpsBought);
        plugin.store().save(data);
        m.bill = bill;
        m.balanceAfter = data.balance();
        long now = System.currentTimeMillis();
        plugin.board().put(Leaderboard.Board.POINTS, m.name, data.bestPoints(), now);
        plugin.board().put(Leaderboard.Board.TOTAL, m.name, data.points(), now);
        plugin.board().put(Leaderboard.Board.FOUND, m.name, data.found(), now);
        plugin.board().put(Leaderboard.Board.SURVIVED, m.name, data.survived(), now);
        Map<String, Long> add = new LinkedHashMap<>();
        add.put("games", 1L);
        add.put("wins_hider", winH ? 1L : 0L);
        add.put("wins_seeker", winS ? 1L : 0L);
        add.put("found", (long) m.finds);
        add.put("survived", m.survived ? 1L : 0L);
        add.put("whistles", (long) m.whistles);
        add.put("points", (long) m.points);
        add.put("earned", (long) bill.credit());
        add.put("bonus", (long) bill.bonus());
        add.put("helps", (long) m.helpsBought);
        Map<String, Long> bestValues = new LinkedHashMap<>();
        bestValues.put("best_points", (long) m.points);
        bestValues.put("best_streak", (long) bill.streak());
        plugin.net().stats(m.uuid, add, bestValues);
        Player p = Bukkit.getPlayer(m.uuid);
        if (p == null || !p.isOnline()) {
            return;
        }
        String lang = plugin.lang(p);
        if (best && tellBest && m.points > 0) {
            p.sendMessage(Texts.msg(lang, "over.best", Placeholder.unparsed("n", String.valueOf(m.points))));
        }
        bill(p, lang, bill, data.balance());
    }

    /** Abrechnung im Chat (und kurz in der Aktionsleiste). */
    private void bill(Player p, String lang, Settlement b, long balance) {
        p.sendMessage(Texts.msg(lang, "settle.head"));
        p.sendMessage(Texts.text(lang, "settle.round", Placeholder.unparsed("n", String.valueOf(b.round()))));
        if (b.spent() > 0) {
            p.sendMessage(Texts.text(lang, "settle.spent", Placeholder.unparsed("n", String.valueOf(b.spent()))));
        }
        if (b.streakBonus() > 0) {
            p.sendMessage(Texts.text(lang, "settle.streak", Placeholder.unparsed("n", String.valueOf(b.streakBonus())),
                    Placeholder.unparsed("streak", String.valueOf(b.streak()))));
        }
        if (b.daily() > 0) {
            p.sendMessage(Texts.text(lang, "settle.daily", Placeholder.unparsed("n", String.valueOf(b.daily()))));
        }
        p.sendMessage(Texts.text(lang, "settle.total", Placeholder.unparsed("n", String.valueOf(b.credit())),
                Placeholder.unparsed("balance", String.valueOf(balance))));
        p.sendActionBar(Texts.text(lang, "settle.bar", Placeholder.unparsed("n", String.valueOf(b.credit())),
                Placeholder.unparsed("balance", String.valueOf(balance))));
        if (b.credit() > 0) {
            Ui.sound(p, "minecraft:entity.experience_orb.pickup", 0.9f);
        }
    }

    /** Titel einer Person für Rangliste und Leiste („❀ Meister der Tarnung“, leer ohne). */
    private String titleOf(Member m, String lang) {
        return plugin.store().loaded(m.uuid).map(d -> plugin.shop().title(d, lang)).orElse("");
    }

    /** Titel (für die Leiste). */
    public String titleOf(UUID uuid, String lang) {
        Member m = members.get(uuid);
        return m == null ? "" : titleOf(m, lang);
    }

    /** Aufräumen und alle zurück in die Gendarmerie. */
    public void close() {
        if (phase != Phase.OVER) {
            phase = Phase.OVER;
            for (Member m : members.values()) {
                report(m, false);
            }
            plugin.saveBoard();
        }
        List<Player> back = audience();
        for (Member m : members.values()) {
            Player p = Bukkit.getPlayer(m.uuid);
            if (m.disguise != null) {
                m.disguise.remove();
                m.disguise = null;
            }
            if (p != null) {
                undress(p, m);
            }
            plugin.store().unpin(m.uuid, p != null && p.isOnline());
        }
        for (Player p : back) {
            timeBar.hide(p);
            plugin.hall().enter(p);
        }
        members.clear();
        spectators.clear();
        fogs.clear();
        tickets(false);
    }

    // ---------- Anzeigen ----------

    private void updateBar() {
        if (phase == Phase.HIDE) {
            int total = settings.hideSeconds() * SECOND;
            timeBar.update(lang -> Texts.text(lang, "bar.hide", Placeholder.unparsed("time", Ui.clock(secondsLeft()))),
                    phaseTicks / (float) total, BossBar.Color.GREEN);
        } else if (phase == Phase.SEEK) {
            int total = settings.seekSeconds() * SECOND;
            int left = count(Member.Role.HIDER);
            timeBar.update(lang -> Texts.text(lang, "bar.seek", Placeholder.unparsed("time", Ui.clock(secondsLeft())),
                    Placeholder.unparsed("n", String.valueOf(left))), phaseTicks / (float) total,
                    phaseTicks < 60 * SECOND ? BossBar.Color.RED : BossBar.Color.YELLOW);
        } else {
            timeBar.update(lang -> Texts.text(lang, winner == Outcome.Winner.HIDERS ? "bar.over.hiders" : "bar.over.seekers"),
                    1f, BossBar.Color.PURPLE);
        }
    }

    private void actionBars() {
        if (phase == Phase.OVER) {
            return;
        }
        long lockTicks = (long) settings.lockSeconds() * SECOND;
        for (Member m : members.values()) {
            Player p = Bukkit.getPlayer(m.uuid);
            if (p == null || !m.online) {
                continue;
            }
            String lang = plugin.lang(p);
            if (m.hider() && m.disguise != null) {
                if (m.lockHint) {
                    continue;
                }
                String state;
                if (m.disguise.solid()) {
                    state = Texts.raw(lang, "hider.state.solid");
                } else {
                    double f = Math.min(1, (tick - m.stillSince) / (double) lockTicks);
                    state = Texts.raw(lang, "hider.state.still").replace("<bar>", Ui.bar(f, 5));
                }
                p.sendActionBar(Texts.text(lang, "hider.bar", Placeholder.unparsed("shape",
                        Texts.plain(lang, "shape." + m.disguise.shape().id())), Placeholder.parsed("lives", hearts(m.lives)),
                        Placeholder.parsed("state", state)));
            } else if (m.role == Member.Role.SEEKER && phase == Phase.SEEK && tick >= m.stunnedUntil
                    && tick - m.lastSwing > SECOND) {
                p.sendActionBar(Texts.text(lang, m.sniffUsed ? "seeker.bar.used" : "seeker.bar",
                        Placeholder.unparsed("n", String.valueOf(count(Member.Role.HIDER)))));
            }
        }
    }

    /** Für die Leiste: Punkte einer Person (0, wenn sie nicht mitspielt). */
    public int pointsOf(UUID uuid) {
        Member m = members.get(uuid);
        return m == null ? 0 : m.points;
    }

    /** Wurde jemand schon gefunden (spielt jetzt als Gendarm)? */
    public boolean converted(UUID uuid) {
        Member m = members.get(uuid);
        return m != null && m.startedHider && m.role == Member.Role.SEEKER;
    }

    /** Alle Gestalt-Anzeigen (und Krämer) in der Welt, die übrig sind (nach einem Absturz), entfernen. */
    public static void cleanStray(World w) {
        for (Entity e : w.getEntities()) {
            if (e.getScoreboardTags().contains(Disguise.TAG)
                    || e.getScoreboardTags().contains(net.morni.versteckerl.hall.Kraemer.TAG)) {
                e.remove();
            }
        }
    }
}
