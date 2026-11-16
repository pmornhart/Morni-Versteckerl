package net.morni.versteckerl.hall;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.morni.versteckerl.Items;
import net.morni.versteckerl.Texts;
import net.morni.versteckerl.Ui;
import net.morni.versteckerl.VersteckerlPlugin;
import net.morni.versteckerl.data.PlayerData;
import net.morni.versteckerl.menu.Menu;
import net.morni.versteckerl.rules.Catalog;
import net.morni.versteckerl.rules.Rewards;
import net.morni.versteckerl.rules.Shape;
import net.morni.versteckerl.rules.Tier;
import net.morni.versteckerl.world.MapType;
import org.bukkit.Material;
import org.bukkit.entity.Player;

/**
 * Die Fenster des Kramerladens (Truhen – gehen auch auf Bedrock): oben das Guthaben, darunter die sechs Abteilungen
 * und „So verdienst du Punkte“. In einer Abteilung zeigt jedes Stück Seltenheit, Beschreibung und was ein Klick tut:
 * kaufen (zweimal klicken), ausrüsten oder ablegen.
 */
public final class ShopMenus {

    private static final int BALANCE = 4;
    private static final int[] KIND_SLOTS = {19, 20, 21, 23, 24, 25};
    private static final int HOW = 31;
    private static final int CLOSE_MAIN = 40;
    private static final int[] ITEM_SLOTS = {19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34};
    private static final int BACK = 45;
    private static final int CLOSE = 49;
    private static final int COMMON = 0xE6E6E6;
    private static final int RARE = 0x5DA9E9;
    private static final int EPIC = 0xF2B632;

    private ShopMenus() {
    }

    /** Farbe einer Seltenheit (Namen im Laden). */
    public static int tierColour(Tier t) {
        return switch (t) {
            case FREE -> Texts.EMERALD;
            case COMMON -> COMMON;
            case RARE -> RARE;
            case EPIC -> EPIC;
        };
    }

    // ---------- Übersicht ----------

    public static void open(VersteckerlPlugin plugin, Player p) {
        String lang = plugin.lang(p);
        if (!plugin.shop().open()) {
            p.sendMessage(Texts.msg(lang, "shop.closed"));
            return;
        }
        Menu menu = new Menu(5, Texts.text(lang, "menu.shop.title"));
        menu.refresher(() -> fillMain(plugin, p, menu));
        fillMain(plugin, p, menu);
        menu.open(p);
        Ui.sound(p, "minecraft:entity.villager.trade", 1f);
    }

    private static void fillMain(VersteckerlPlugin plugin, Player p, Menu menu) {
        String lang = plugin.lang(p);
        PlayerData d = plugin.data(p);
        menu.clear();
        frame(menu, 5);
        menu.set(BALANCE, balance(plugin, lang, d), null);
        Catalog.Kind[] kinds = Catalog.Kind.values();
        for (int i = 0; i < kinds.length && i < KIND_SLOTS.length; i++) {
            Catalog.Kind k = kinds[i];
            List<Catalog.Item> items = offered(plugin, k);
            long have = items.stream().filter(it -> !it.free() && d.owns(it.key())).count();
            long total = items.stream().filter(it -> !it.free()).count();
            List<Component> lore = new ArrayList<>(Texts.lines(lang, "menu.shop.kind." + k.section() + ".desc"));
            lore.add(Component.empty());
            lore.add(Texts.text(lang, "menu.shop.kind.have", Placeholder.unparsed("n", String.valueOf(have)),
                    Placeholder.unparsed("total", String.valueOf(total))));
            Catalog.Item eq = plugin.shop().equipped(d, k);
            if (eq != null && !(k == Catalog.Kind.SHAPE)) {
                lore.add(Texts.text(lang, "menu.shop.kind.equipped", Placeholder.unparsed("item", Shop.name(lang, eq))));
            } else if (eq != null) {
                lore.add(Texts.text(lang, "menu.shop.kind.favourite", Placeholder.unparsed("item", Shop.name(lang, eq))));
            }
            lore.add(Texts.text(lang, "menu.shop.kind.click"));
            menu.set(KIND_SLOTS[i], Items.named(kindIcon(k), 1, Texts.text(lang, "menu.shop.kind." + k.section()), lore),
                    q -> category(plugin, q, k));
        }
        menu.set(HOW, Items.named(Material.WRITABLE_BOOK, 1, Texts.text(lang, "menu.shop.how"), how(plugin, lang)),
                null);
        menu.set(CLOSE_MAIN, Items.named(Material.BARRIER, 1, Texts.text(lang, "menu.close"), null),
                Player::closeInventory);
    }

    private static List<Catalog.Item> offered(VersteckerlPlugin plugin, Catalog.Kind k) {
        return Catalog.of(k).stream().filter(i -> plugin.settings().offered(i)).toList();
    }

    private static Material kindIcon(Catalog.Kind k) {
        return switch (k) {
            case SHAPE -> Material.ARMOR_STAND;
            case WHISTLE -> Material.NOTE_BLOCK;
            case FIND -> Material.FIREWORK_STAR;
            case TRAIL -> Material.PINK_PETALS;
            case WIN -> Material.FIREWORK_ROCKET;
            case TITLE -> Material.NAME_TAG;
        };
    }

    /** Guthaben-Anzeige: Guthaben, Punkte insgesamt, Serie, Tagesbonus, Titel. */
    static org.bukkit.inventory.ItemStack balance(VersteckerlPlugin plugin, String lang, PlayerData d) {
        String today = java.time.LocalDate.now(java.time.ZoneId.of("Europe/Berlin")).toString();
        String title = plugin.shop().title(d, lang);
        List<Component> lore = new ArrayList<>(Texts.lines(lang, "menu.shop.balance.lore",
                Placeholder.unparsed("total", String.valueOf(d.points())),
                Placeholder.unparsed("streak", String.valueOf(d.streak())),
                Placeholder.unparsed("best", String.valueOf(d.bestStreak())),
                Placeholder.unparsed("spent", String.valueOf(d.spent())),
                Placeholder.unparsed("daily", Texts.plain(lang, d.dailyOpen(today) ? "menu.shop.daily.open"
                        : "menu.shop.daily.taken"))));
        if (!title.isEmpty()) {
            lore.add(Texts.text(lang, "menu.shop.balance.title", Placeholder.unparsed("title", title)));
        }
        return Items.glow(Items.named(Material.EMERALD, 1, Texts.text(lang, "menu.shop.balance",
                Placeholder.unparsed("n", String.valueOf(d.balance()))), lore), true);
    }

    /** „So verdienst du Punkte“ mit den Werten aus der config.yml. */
    private static List<Component> how(VersteckerlPlugin plugin, String lang) {
        Rewards r = plugin.settings().rewards();
        return Texts.lines(lang, "menu.shop.how.lore",
                Placeholder.unparsed("hit", String.valueOf(r.hit())),
                Placeholder.unparsed("find", String.valueOf(r.find())),
                Placeholder.unparsed("first", String.valueOf(r.firstFind())),
                Placeholder.unparsed("swin", String.valueOf(r.seekersWin())),
                Placeholder.unparsed("half", String.valueOf(r.perHalfMinute())),
                Placeholder.unparsed("wmax", String.valueOf(r.whistleMax())),
                Placeholder.unparsed("loud", String.valueOf(r.loud())),
                Placeholder.unparsed("vloud", String.valueOf(r.veryLoud())),
                Placeholder.unparsed("brave", String.valueOf(r.braveNear())),
                Placeholder.unparsed("bblocks", String.valueOf(r.braveNearBlocks())),
                Placeholder.unparsed("last", String.valueOf(r.lastHider())),
                Placeholder.unparsed("survive", String.valueOf(r.survive())),
                Placeholder.unparsed("streak", String.valueOf(r.streakPerWin())),
                Placeholder.unparsed("daily", String.valueOf(r.daily())));
    }

    private static void frame(Menu menu, int rows) {
        for (int i = 0; i < 9; i++) {
            menu.set(i, Items.pane(Material.GREEN_STAINED_GLASS_PANE), null);
            menu.set((rows - 1) * 9 + i, Items.pane(Material.GREEN_STAINED_GLASS_PANE), null);
        }
    }

    // ---------- Abteilung ----------

    public static void category(VersteckerlPlugin plugin, Player p, Catalog.Kind kind) {
        String lang = plugin.lang(p);
        Menu menu = new Menu(6, Texts.text(lang, "menu.shop.title.kind", Placeholder.unparsed("kind",
                Texts.plain(lang, "menu.shop.kind." + kind.section()))));
        menu.refresher(() -> fillCategory(plugin, p, menu, kind));
        fillCategory(plugin, p, menu, kind);
        menu.open(p);
        Ui.sound(p, "minecraft:item.book.page_turn", 1f);
    }

    private static void fillCategory(VersteckerlPlugin plugin, Player p, Menu menu, Catalog.Kind kind) {
        String lang = plugin.lang(p);
        PlayerData d = plugin.data(p);
        Shop shop = plugin.shop();
        menu.clear();
        frame(menu, 6);
        menu.set(BALANCE, balance(plugin, lang, d), null);
        List<Catalog.Item> items = offered(plugin, kind);
        for (int i = 0; i < items.size() && i < ITEM_SLOTS.length; i++) {
            Catalog.Item it = items.get(i);
            Shop.State state = shop.state(d, it);
            menu.set(ITEM_SLOTS[i], itemStack(plugin, lang, d, it, state, shop.confirming(p.getUniqueId(), it)), q -> {
                shop.click(q, it);
                fillCategory(plugin, q, menu, kind);
            });
        }
        menu.set(BACK, Items.named(Material.ARROW, 1, Texts.text(lang, "menu.back"), null), q -> open(plugin, q));
        menu.set(CLOSE, Items.named(Material.BARRIER, 1, Texts.text(lang, "menu.close"), null), Player::closeInventory);
    }

    private static org.bukkit.inventory.ItemStack itemStack(VersteckerlPlugin plugin, String lang, PlayerData d,
            Catalog.Item it, Shop.State state, boolean confirming) {
        List<Component> lore = new ArrayList<>();
        lore.add(Texts.text(lang, "tier." + it.tier().id()));
        lore.addAll(Texts.lines(lang, Shop.descKey(it)));
        if (it.kind() == Catalog.Kind.SHAPE) {
            lore.add(Texts.text(lang, "menu.shop.maps", Placeholder.unparsed("list", maps(lang, Catalog.shape(it)))));
        }
        if (it.kind() == Catalog.Kind.WHISTLE) {
            lore.add(Texts.text(lang, "menu.shop.loud." + Math.max(1, it.loudness()), Placeholder.unparsed("n",
                    String.valueOf(plugin.settings().rewards().loudness(it.loudness())))));
        }
        if (it.need() != null) {
            lore.add(Texts.text(lang, "menu.shop.need", Placeholder.unparsed("need", Shop.need(lang, it, d))));
        }
        lore.add(Component.empty());
        int price = plugin.shop().price(it);
        String stateKey = switch (state) {
            case EQUIPPED -> it.kind() == Catalog.Kind.SHAPE ? "menu.shop.state.favourite"
                    : it.free() ? "menu.shop.state.equipped.free" : "menu.shop.state.equipped";
            case OWNED -> it.kind() == Catalog.Kind.SHAPE ? "menu.shop.state.owned.shape" : "menu.shop.state.owned";
            case BUY -> confirming ? "menu.shop.state.confirm" : "menu.shop.state.buy";
            case TOO_POOR -> "menu.shop.state.poor";
            case NEED -> "menu.shop.state.need";
            case OFF -> "menu.shop.state.off";
        };
        lore.addAll(Texts.lines(lang, stateKey, Placeholder.unparsed("n", String.valueOf(price)),
                Placeholder.unparsed("missing", String.valueOf(Math.max(0, price - d.balance())))));
        Component name = Texts.text(lang, Shop.nameKey(it)).color(TextColor.color(tierColour(it.tier())));
        if (it.kind() == Catalog.Kind.TITLE && !it.symbol().isEmpty()) {
            name = Component.text(it.symbol() + " ").color(TextColor.color(tierColour(it.tier()))).append(name);
        }
        boolean glow = state == Shop.State.EQUIPPED || confirming;
        return Items.glow(Items.named(icon(it), 1, name, lore), glow);
    }

    /** Karten, auf denen es eine Gestalt gibt. */
    private static String maps(String lang, Shape s) {
        List<String> out = new ArrayList<>();
        for (MapType m : MapType.values()) {
            if (s != null && m.shapes().contains(s)) {
                out.add(Texts.plain(lang, "map." + m.id()));
            }
        }
        return String.join(", ", out);
    }

    /** Symbol eines Stücks im Laden. */
    static Material icon(Catalog.Item it) {
        if (it.kind() == Catalog.Kind.SHAPE) {
            Shape s = Catalog.shape(it);
            Material m = s == null ? null : Material.matchMaterial(s.icon());
            return m == null ? Material.BARREL : m;
        }
        String id = it.id().toLowerCase(Locale.ROOT);
        return switch (it.kind()) {
            case WHISTLE -> switch (id) {
                case "kuckuck" -> Material.FEATHER;
                case "henne" -> Material.EGG;
                case "murmeltier" -> Material.RABBIT_HIDE;
                case "kuhglocke" -> Material.BELL;
                case "jodler" -> Material.NOTE_BLOCK;
                case "trompete" -> Material.JUKEBOX;
                case "alphorn" -> Material.GOAT_HORN;
                default -> Material.AMETHYST_SHARD;
            };
            case FIND -> switch (id) {
                case "herzerl" -> Material.POPPY;
                case "rauchwolke" -> Material.CAMPFIRE;
                case "konfetti" -> Material.FIREWORK_STAR;
                case "blueten" -> Material.PINK_PETALS;
                case "weissblau" -> Material.LIGHT_BLUE_DYE;
                case "goldregen" -> Material.GOLD_NUGGET;
                default -> Material.EMERALD;
            };
            case TRAIL -> switch (id) {
                case "noten" -> Material.NOTE_BLOCK;
                case "herzerl" -> Material.POPPY;
                case "schneeflocken" -> Material.SNOWBALL;
                case "blueten" -> Material.PINK_PETALS;
                case "funken" -> Material.GLOWSTONE_DUST;
                case "gluehwuermchen" -> Material.FIREFLY_BUSH;
                default -> Material.LEATHER_BOOTS;
            };
            case WIN -> switch (id) {
                case "glocken" -> Material.BELL;
                case "konfetti" -> Material.FIREWORK_STAR;
                case "goldregen" -> Material.GOLD_INGOT;
                case "feuerwerk" -> Material.FIREWORK_ROCKET;
                case "weissblau" -> Material.LIGHT_BLUE_DYE;
                default -> Material.GRAY_DYE;
            };
            case TITLE -> it.free() ? Material.PAPER : Material.NAME_TAG;
            default -> Material.PAPER;
        };
    }
}
