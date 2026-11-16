package net.morni.versteckerl;

import net.morni.versteckerl.data.Leaderboard;
import net.morni.versteckerl.game.Game;
import net.morni.versteckerl.game.Member;
import net.morni.versteckerl.hall.HallMenus;
import net.morni.versteckerl.rules.Settlement;
import net.morni.versteckerl.world.HallPlan;
import net.morni.versteckerl.world.MapType;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/** Leiste rechts (Runde und Gendarmerie) und die schwebenden Tafeln in der Gendarmerie. */
public final class Boards {

    private static final int TOP_ROWS = 3;

    private final VersteckerlPlugin plugin;
    private final Sidebar sidebar = new Sidebar();
    private final List<Holo> hallHolos = new ArrayList<>();

    Boards(VersteckerlPlugin plugin) {
        this.plugin = plugin;
    }

    public Sidebar sidebar() {
        return sidebar;
    }

    /** Leiste für alle in einer Runde. */
    public void game(Game g) {
        for (Player p : g.audience()) {
            String lang = plugin.lang(p);
            List<Component> lines = new ArrayList<>();
            lines.add(Component.empty());
            lines.add(Texts.text(lang, "side.map", Placeholder.unparsed("map", Texts.plain(lang, "map." + g.map().id()))));
            String phase = switch (g.phase()) {
                case HIDE -> "side.phase.hide";
                case SEEK -> "side.phase.seek";
                default -> "side.phase.over";
            };
            lines.add(Texts.text(lang, phase, Placeholder.unparsed("time", Ui.clock(g.secondsLeft()))));
            lines.add(Component.text(" "));
            Member me = g.member(p.getUniqueId());
            if (me != null && me.bill() != null) {
                bill(lines, lang, me);
                lines.add(Component.text("   "));
                lines.add(Texts.text(lang, "side.site"));
                sidebar.show(p, Texts.text(lang, "side.title"), lines);
                continue;
            }
            lines.add(Texts.text(lang, "side.hiders", Placeholder.unparsed("n", String.valueOf(g.count(Member.Role.HIDER)))));
            lines.add(Texts.text(lang, "side.seekers", Placeholder.unparsed("n", String.valueOf(g.count(Member.Role.SEEKER)))));
            if (me != null) {
                lines.add(Component.text("  "));
                lines.add(Texts.text(lang, me.hider() ? "side.role.hider" : "side.role.seeker"));
                lines.add(me.spent() > 0
                        ? Texts.text(lang, "side.points.left", Placeholder.unparsed("n", String.valueOf(me.points())),
                                Placeholder.unparsed("left", String.valueOf(me.available())))
                        : Texts.text(lang, "side.points", Placeholder.unparsed("n", String.valueOf(me.points()))));
                String title = g.titleOf(p.getUniqueId(), lang);
                if (!title.isEmpty()) {
                    lines.add(Texts.text(lang, "side.title-line", Placeholder.unparsed("title", title)));
                }
            } else {
                lines.add(Component.text("  "));
                lines.add(Texts.text(lang, "side.role.spectator"));
            }
            lines.add(Component.text("   "));
            lines.add(Texts.text(lang, "side.site"));
            sidebar.show(p, Texts.text(lang, "side.title"), lines);
        }
    }

    /** Nach der Runde: die Abrechnung (wie im Chat, aber bleibt stehen, bis es zurück in die Gendarmerie geht). */
    private static void bill(List<Component> lines, String lang, Member me) {
        Settlement b = me.bill();
        lines.add(Texts.text(lang, "side.bill.head"));
        lines.add(Texts.text(lang, "side.bill.round", Placeholder.unparsed("n", String.valueOf(b.round()))));
        if (b.spent() > 0) {
            lines.add(Texts.text(lang, "side.bill.spent", Placeholder.unparsed("n", String.valueOf(b.spent()))));
        }
        if (b.streakBonus() > 0) {
            lines.add(Texts.text(lang, "side.bill.streak", Placeholder.unparsed("n", String.valueOf(b.streakBonus()))));
        }
        if (b.daily() > 0) {
            lines.add(Texts.text(lang, "side.bill.daily", Placeholder.unparsed("n", String.valueOf(b.daily()))));
        }
        lines.add(Texts.text(lang, "side.bill.total", Placeholder.unparsed("n", String.valueOf(b.credit()))));
        lines.add(Texts.text(lang, "side.bill.balance", Placeholder.unparsed("n", String.valueOf(me.balanceAfter()))));
    }

    /** Leiste in der Gendarmerie. */
    public void hall(Player p) {
        String lang = plugin.lang(p);
        List<Component> lines = new ArrayList<>();
        lines.add(Component.empty());
        lines.add(Texts.text(lang, "side.hall.players", Placeholder.unparsed("n", String.valueOf(plugin.hall().waiting().size()))));
        int cd = plugin.hall().countdown();
        lines.add(cd >= 0 ? Texts.text(lang, "side.hall.start", Placeholder.unparsed("secs", String.valueOf(cd)))
                : Texts.text(lang, "side.hall.wait"));
        lines.add(Component.text(" "));
        plugin.store().loaded(p.getUniqueId()).ifPresent(d -> {
            lines.add(Texts.text(lang, "side.hall.balance", Placeholder.unparsed("n", String.valueOf(d.balance()))));
            lines.add(Texts.text(lang, "side.hall.total", Placeholder.unparsed("n", String.valueOf(d.points()))));
            if (d.streak() > 1) {
                lines.add(Texts.text(lang, "side.hall.streak", Placeholder.unparsed("n", String.valueOf(d.streak()))));
            }
            lines.add(Component.text("   "));
            lines.add(Texts.text(lang, "side.hall.found", Placeholder.unparsed("n", String.valueOf(d.found()))));
            lines.add(Texts.text(lang, "side.hall.survived", Placeholder.unparsed("n", String.valueOf(d.survived()))));
            lines.add(Texts.text(lang, "side.hall.best", Placeholder.unparsed("n", String.valueOf(d.bestPoints()))));
        });
        lines.add(Component.text("    "));
        lines.add(Texts.text(lang, "side.site"));
        sidebar.show(p, Texts.text(lang, "side.title"), lines);
    }

    public void hallAll() {
        for (UUID u : plugin.hall().waiting()) {
            Player p = Bukkit.getPlayer(u);
            if (p != null) {
                hall(p);
            }
        }
    }

    // ---------- Tafeln in der Gendarmerie ----------

    public void spawnHall() {
        removeHall();
        MapType[] maps = MapType.values();
        for (int i = 0; i < maps.length && i < HallPlan.STANDS.length; i++) {
            MapType m = maps[i];
            int[] t = HallPlan.STANDS[i];
            Location at = new Location(plugin.world(), t[0] + 0.5, MapType.FLOOR_Y + 2.95, t[1] + 1.2);
            hallHolos.add(Holo.spawn(plugin, at, lang -> stand(lang, m), 0.8f, 160));
        }
        Location board = new Location(plugin.world(), HallPlan.BOARD[0] + 1.6, MapType.FLOOR_Y + 1.35,
                HallPlan.BOARD[1] + 0.5);
        hallHolos.add(Holo.spawn(plugin, board, this::wanted, 0.6f, 200));
    }

    private Component stand(String lang, MapType m) {
        return Texts.text(lang, "board.stand", Placeholder.unparsed("map", Texts.plain(lang, "map." + m.id())),
                Placeholder.unparsed("desc", Texts.plain(lang, "board.stand.desc." + m.id())),
                Placeholder.unparsed("list", HallMenus.shapeList(lang, m)));
    }

    private Component wanted(String lang) {
        Component c = Texts.text(lang, "board.title");
        for (Leaderboard.Board b : Leaderboard.Board.values()) {
            c = c.append(Component.newline()).append(Component.newline()).append(Texts.text(lang, "board." + b.id()));
            for (Component row : HallMenus.rows(plugin, lang, b, TOP_ROWS)) {
                c = c.append(Component.newline()).append(row);
            }
        }
        return c;
    }

    public void refreshHall() {
        if (hallHolos.isEmpty()) {
            return;
        }
        if (!hallHolos.stream().allMatch(Holo::valid)) {
            spawnHall();
            return;
        }
        MapType[] maps = MapType.values();
        for (int i = 0; i < maps.length && i < hallHolos.size() - 1; i++) {
            MapType m = maps[i];
            hallHolos.get(i).update(lang -> stand(lang, m));
        }
        hallHolos.get(hallHolos.size() - 1).update(this::wanted);
    }

    public void removeHall() {
        hallHolos.forEach(Holo::remove);
        hallHolos.clear();
    }
}
