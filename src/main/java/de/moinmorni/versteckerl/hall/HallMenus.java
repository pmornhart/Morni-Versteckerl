package de.moinmorni.versteckerl.hall;

import de.moinmorni.versteckerl.Items;
import de.moinmorni.versteckerl.Texts;
import de.moinmorni.versteckerl.VersteckerlPlugin;
import de.moinmorni.versteckerl.data.Leaderboard;
import de.moinmorni.versteckerl.data.PlayerData;
import de.moinmorni.versteckerl.game.Game;
import de.moinmorni.versteckerl.menu.Menu;
import de.moinmorni.versteckerl.rules.Shape;
import de.moinmorni.versteckerl.world.MapType;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Material;
import org.bukkit.entity.Player;

/** Menüs der Gendarmerie: Karte wählen (oder zuschauen) und Steckbriefe (Statistik und Bestenlisten). */
public final class HallMenus {

    private static final int[] MAP_SLOTS = {11, 13, 15};
    private static final int[] BOARD_SLOTS = {14, 15, 16};

    private HallMenus() {
    }

    public static Material icon(MapType m) {
        return switch (m) {
            case DORFPLATZ -> Material.HAY_BLOCK;
            case WIRTSHAUS -> Material.BARREL;
            case HEUSTADEL -> Material.CARVED_PUMPKIN;
        };
    }

    /** Gestalten einer Karte als Text („Heuballen, Fass, …“). */
    public static String shapeList(String lang, MapType m) {
        List<String> out = new ArrayList<>();
        for (Shape s : m.shapes()) {
            out.add(Texts.plain(lang, "shape." + s.id()));
        }
        return String.join(", ", out);
    }

    public static void vote(VersteckerlPlugin plugin, Player p) {
        String lang = plugin.lang(p);
        Menu menu = new Menu(3, Texts.text(lang, "menu.vote.title"));
        MapType[] maps = MapType.values();
        for (int i = 0; i < maps.length && i < MAP_SLOTS.length; i++) {
            MapType m = maps[i];
            Game running = plugin.games().on(m);
            List<Component> lore = new ArrayList<>(Texts.lines(lang, "map." + m.id() + ".desc"));
            lore.add(Texts.text(lang, "menu.vote.shapes", Placeholder.unparsed("list", shapeList(lang, m))));
            lore.add(Texts.text(lang, "menu.vote.votes", Placeholder.unparsed("n",
                    String.valueOf(plugin.hall().votes().count(m)))));
            boolean mine = plugin.hall().votes().voteOf(p.getUniqueId()) == m;
            if (running != null) {
                lore.add(Texts.text(lang, "menu.vote.busy", Placeholder.unparsed("n",
                        String.valueOf(running.count(de.moinmorni.versteckerl.game.Member.Role.HIDER)))));
            } else {
                lore.add(Texts.text(lang, mine ? "menu.vote.mine" : "menu.vote.click"));
            }
            menu.set(MAP_SLOTS[i], Items.glow(Items.named(icon(m), 1, Texts.text(lang, "map." + m.id())
                    .color(TextColor.color(Texts.EMERALD)), lore), mine), q -> {
                        q.closeInventory();
                        Game g = plugin.games().on(m);
                        if (g != null) {
                            plugin.spectate(q, g);
                        } else {
                            plugin.hall().vote(q, m);
                        }
                    });
        }
        menu.set(22, Items.named(Material.BARRIER, 1, Texts.text(lang, "menu.close"), null), Player::closeInventory);
        menu.open(p);
    }

    public static void stats(VersteckerlPlugin plugin, Player p) {
        String lang = plugin.lang(p);
        Menu menu = new Menu(3, Texts.text(lang, "menu.stats.title"));
        PlayerData d = plugin.data(p);
        menu.set(11, Items.named(Material.PLAYER_HEAD, 1, Texts.text(lang, "menu.stats.me"), statLines(lang, d)), null);
        Leaderboard.Board[] boards = Leaderboard.Board.values();
        Material[] icons = {Material.GOLD_INGOT, Material.STICK, Material.BARREL};
        for (int i = 0; i < boards.length; i++) {
            menu.set(BOARD_SLOTS[i], Items.named(icons[i], 1, Texts.text(lang, "board." + boards[i].id()),
                    rows(plugin, lang, boards[i], Leaderboard.SIZE)), null);
        }
        menu.set(22, Items.named(Material.BARRIER, 1, Texts.text(lang, "menu.close"), null), Player::closeInventory);
        menu.open(p);
    }

    /** Zeilen einer Bestenliste. */
    public static List<Component> rows(VersteckerlPlugin plugin, String lang, Leaderboard.Board board, int max) {
        List<Component> lore = new ArrayList<>();
        List<Leaderboard.Entry> top = plugin.board().top(board);
        if (top.isEmpty()) {
            lore.add(Texts.text(lang, "menu.stats.empty"));
        }
        for (int i = 0; i < Math.min(max, top.size()); i++) {
            Leaderboard.Entry e = top.get(i);
            lore.add(Texts.text(lang, "menu.stats.row", Placeholder.unparsed("pos", String.valueOf(i + 1)),
                    Placeholder.unparsed("name", e.name()), Placeholder.unparsed("n", String.valueOf(e.value()))));
        }
        return lore;
    }

    public static List<Component> statLines(String lang, PlayerData d) {
        return Texts.lines(lang, "menu.stats.lines", Placeholder.unparsed("games", String.valueOf(d.games())),
                Placeholder.unparsed("winh", String.valueOf(d.winsHider())),
                Placeholder.unparsed("wins", String.valueOf(d.winsSeeker())),
                Placeholder.unparsed("found", String.valueOf(d.found())),
                Placeholder.unparsed("survived", String.valueOf(d.survived())),
                Placeholder.unparsed("whistles", String.valueOf(d.whistles())),
                Placeholder.unparsed("points", String.valueOf(d.points())),
                Placeholder.unparsed("best", String.valueOf(d.bestPoints())));
    }
}
