package net.morni.versteckerl.game;

import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.morni.versteckerl.Items;
import net.morni.versteckerl.Texts;
import net.morni.versteckerl.VersteckerlPlugin;
import net.morni.versteckerl.hall.ShopMenus;
import net.morni.versteckerl.menu.Menu;
import net.morni.versteckerl.rules.Catalog;
import net.morni.versteckerl.rules.Shape;
import org.bukkit.Material;
import org.bukkit.entity.Player;

/**
 * Gestalt wählen – als Truhen-Fenster (geht auch für Bedrock-Spieler). Gestalten aus dem Kramerladen, die man (noch)
 * nicht hat, stehen grau mit Preis dabei – so sieht jeder, was es auf der Karte alles gibt.
 */
public final class ShapeMenu {

    private static final int[] SLOTS = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33,
        34};
    private static final int CLOSE = 40;

    private ShapeMenu() {
    }

    public static void open(VersteckerlPlugin plugin, Game g, Player p) {
        Member m = g.member(p.getUniqueId());
        if (m == null || !m.hider() || m.disguise == null) {
            return;
        }
        String lang = plugin.lang(p);
        boolean seeking = g.phase() == Game.Phase.SEEK;
        Menu menu = new Menu(5, Texts.text(lang, seeking ? "menu.shape.title.change" : "menu.shape.title"));
        List<Shape> allowed = g.allowedShapes(p);
        List<Shape> shapes = new ArrayList<>(g.map().shapes());
        shapes.removeIf(s -> !s.free() && !allowed.contains(s) && !plugin.settings().offered(
                Catalog.get(Catalog.Kind.SHAPE, s.id())));
        for (int i = 0; i < shapes.size() && i < SLOTS.length; i++) {
            Shape s = shapes.get(i);
            boolean mine = m.disguise.shape() == s;
            boolean open = allowed.contains(s);
            List<Component> lore = new ArrayList<>(Texts.lines(lang, "shape." + s.id() + ".hint"));
            lore.add(Component.empty());
            if (!open) {
                Catalog.Item item = Catalog.get(Catalog.Kind.SHAPE, s.id());
                lore.add(Texts.text(lang, "menu.shape.locked", Placeholder.unparsed("n",
                        String.valueOf(plugin.shop().price(item)))));
                lore.add(Texts.text(lang, "menu.shape.locked.where"));
            } else {
                lore.add(Texts.text(lang, mine ? "menu.shape.mine" : seeking ? "menu.shape.click.change"
                        : "menu.shape.click"));
            }
            Material icon = Material.matchMaterial(s.icon());
            Component name = Texts.text(lang, "shape." + s.id()).color(TextColor.color(open
                    ? (s.free() ? Texts.GOLD : ShopMenus.tierColour(s.tier())) : 0x8A8A8A));
            menu.set(SLOTS[i], Items.glow(Items.named(open ? (icon == null ? Material.BARREL : icon) : Material.GRAY_DYE, 1,
                    name, lore), mine), q -> {
                        q.closeInventory();
                        g.chooseShape(q, s);
                    });
        }
        menu.set(CLOSE, Items.named(Material.BARRIER, 1, Texts.text(lang, "menu.close"), null), Player::closeInventory);
        menu.open(p);
    }
}
