package net.morni.versteckerl.game;

import net.morni.versteckerl.Items;
import net.morni.versteckerl.Texts;
import net.morni.versteckerl.VersteckerlPlugin;
import net.morni.versteckerl.menu.Menu;
import net.morni.versteckerl.rules.Shape;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;

/** Gestalt wählen – als Truhen-Fenster (geht auch für Bedrock-Spieler). */
public final class ShapeMenu {

    private static final int[] SLOTS = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25};

    private ShapeMenu() {
    }

    public static void open(VersteckerlPlugin plugin, Game g, Player p) {
        Member m = g.member(p.getUniqueId());
        if (m == null || !m.hider() || m.disguise == null) {
            return;
        }
        String lang = plugin.lang(p);
        boolean seeking = g.phase() == Game.Phase.SEEK;
        Menu menu = new Menu(4, Texts.text(lang, seeking ? "menu.shape.title.change" : "menu.shape.title"));
        List<Shape> shapes = g.map().shapes();
        for (int i = 0; i < shapes.size() && i < SLOTS.length; i++) {
            Shape s = shapes.get(i);
            boolean mine = m.disguise.shape() == s;
            List<Component> lore = new ArrayList<>(Texts.lines(lang, "shape." + s.id() + ".hint"));
            lore.add(Component.empty());
            lore.add(Texts.text(lang, mine ? "menu.shape.mine" : seeking ? "menu.shape.click.change" : "menu.shape.click"));
            Material icon = Material.matchMaterial(s.icon());
            menu.set(SLOTS[i], Items.glow(Items.named(icon == null ? Material.BARREL : icon, 1,
                    Texts.text(lang, "shape." + s.id()).color(TextColor.color(Texts.GOLD)), lore), mine), q -> {
                        q.closeInventory();
                        g.chooseShape(q, s);
                    });
        }
        menu.set(31, Items.named(Material.BARRIER, 1, Texts.text(lang, "menu.close"), null), Player::closeInventory);
        menu.open(p);
    }
}
