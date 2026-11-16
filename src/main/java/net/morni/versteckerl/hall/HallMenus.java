package net.morni.versteckerl.hall;

import net.morni.versteckerl.Items;
import net.morni.versteckerl.Texts;
import net.morni.versteckerl.VersteckerlPlugin;
import net.morni.versteckerl.data.Leaderboard;
import net.morni.versteckerl.data.PlayerData;
import net.morni.versteckerl.game.Game;
import net.morni.versteckerl.game.Member;
import net.morni.versteckerl.menu.Menu;
import net.morni.versteckerl.rules.Shape;
import net.morni.versteckerl.world.MapType;
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
    private static final int[] BOARD_SLOTS = {13, 14, 15, 16};

    private HallMenus() {
    }

    public static Material icon(MapType m) {
        return switch (m) {
            case DORFPLATZ -> Material.HAY_BLOCK;
            case WIRTSHAUS -> Material.BARREL;
            case HEUSTADEL -> Material.CARVED_PUMPKIN;
        };
    }

    /** Gestalten einer Karte als Text („Heuballen, Fass, …“), dahinter die aus dem Kramerladen. */
    public static String shapeList(String lang, MapType m) {
        List<String> out = new ArrayList<>();
        for (Shape s : m.freeShapes()) {
            out.add(Texts.plain(lang, "shape." + s.id()));
        }
        String list = String.join(", ", out);
        int extra = m.shopShapes().size();
        return extra == 0 ? list : list + Texts.plain(lang, "menu.vote.shop-shapes", Placeholder.unparsed("n",
                String.valueOf(extra)));
    }

    /** Gestalten aus dem Kramerladen auf einer Karte („Werkbank, Busch …“). */
    public static String shopShapeList(String lang, MapType m) {
        List<String> out = new ArrayList<>();
        for (Shape s : m.shopShapes()) {
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
            if (!m.shopShapes().isEmpty()) {
                lore.add(Texts.text(lang, "menu.vote.shop", Placeholder.unparsed("list", shopShapeList(lang, m))));
            }
            lore.add(Texts.text(lang, "menu.vote.votes", Placeholder.unparsed("n",
                    String.valueOf(plugin.hall().votes().count(m)))));
            boolean mine = plugin.hall().votes().voteOf(p.getUniqueId()) == m;
            if (running != null) {
                lore.add(Texts.text(lang, "menu.vote.busy", Placeholder.unparsed("n",
                        String.valueOf(running.count(Member.Role.HIDER)))));
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
        List<Component> me = new ArrayList<>(statLines(lang, d));
        me.addAll(walletLines(plugin, lang, d));
        menu.set(10, Items.named(Material.PLAYER_HEAD, 1, Texts.text(lang, "menu.stats.me"), me), null);
        menu.set(11, ShopMenus.balance(plugin, lang, d), q -> ShopMenus.open(plugin, q));
        Leaderboard.Board[] boards = Leaderboard.Board.values();
        Material[] icons = {Material.GOLD_INGOT, Material.EMERALD, Material.STICK, Material.BARREL};
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

    /** Guthaben, Serie und Titel für den Steckbrief. */
    public static List<Component> walletLines(VersteckerlPlugin plugin, String lang, PlayerData d) {
        List<Component> out = new ArrayList<>(Texts.lines(lang, "menu.stats.wallet",
                Placeholder.unparsed("balance", String.valueOf(d.balance())),
                Placeholder.unparsed("streak", String.valueOf(d.streak())),
                Placeholder.unparsed("best", String.valueOf(d.bestStreak())),
                Placeholder.unparsed("unlocks", String.valueOf(d.owned().size()))));
        String title = plugin.shop().title(d, lang);
        if (!title.isEmpty()) {
            out.add(Texts.text(lang, "menu.stats.title-line", Placeholder.unparsed("title", title)));
        }
        return out;
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
