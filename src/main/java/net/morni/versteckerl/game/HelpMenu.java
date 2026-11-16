package net.morni.versteckerl.game;

import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.morni.versteckerl.Items;
import net.morni.versteckerl.Texts;
import net.morni.versteckerl.VersteckerlPlugin;
import net.morni.versteckerl.menu.Menu;
import net.morni.versteckerl.rules.Helps;
import org.bukkit.Material;
import org.bukkit.entity.Player;

/**
 * Die Kramerkiste: Rundenladen während der Suche, als Truhen-Fenster. Jede Hilfe zeigt Preis, wie oft sie noch geht
 * und die Abklingzeit; ein Klick kauft sie und setzt sie gleich ein.
 */
public final class HelpMenu {

    private static final int[] SLOTS = {11, 13, 15};
    private static final int INFO = 4;
    private static final int CLOSE = 22;

    private HelpMenu() {
    }

    public static void open(VersteckerlPlugin plugin, Game g, Player p) {
        Member m = g.member(p.getUniqueId());
        if (m == null) {
            return;
        }
        String lang = plugin.lang(p);
        Menu menu = new Menu(3, Texts.text(lang, "menu.helps.title"));
        menu.refresher(() -> fill(plugin, g, p, menu));
        fill(plugin, g, p, menu);
        menu.open(p);
    }

    private static void fill(VersteckerlPlugin plugin, Game g, Player p, Menu menu) {
        Member m = g.member(p.getUniqueId());
        if (m == null) {
            p.closeInventory();
            return;
        }
        String lang = plugin.lang(p);
        menu.clear();
        menu.set(INFO, Items.named(Material.SUNFLOWER, 1, Texts.text(lang, "menu.helps.points",
                Placeholder.unparsed("n", String.valueOf(m.available()))), Texts.lines(lang, "menu.helps.points.lore",
                Placeholder.unparsed("earned", String.valueOf(m.points())),
                Placeholder.unparsed("spent", String.valueOf(m.spent())))), null);
        List<Helps.Help> mine = new ArrayList<>();
        for (Helps.Help h : Helps.Help.values()) {
            if (h.forHiders() == m.hider()) {
                mine.add(h);
            }
        }
        for (int i = 0; i < mine.size() && i < SLOTS.length; i++) {
            Helps.Help h = mine.get(i);
            Helps.Rule rule = plugin.settings().help(h);
            Helps.Verdict v = g.helpVerdict(p, h);
            List<Component> lore = new ArrayList<>(Texts.lines(lang, "help." + h.id() + ".desc"));
            lore.add(Component.empty());
            lore.add(Texts.text(lang, "menu.helps.price", Placeholder.unparsed("n", String.valueOf(rule.price()))));
            lore.add(Texts.text(lang, "menu.helps.uses", Placeholder.unparsed("n",
                    String.valueOf(Math.max(0, rule.perRound() - m.uses(h)))),
                    Placeholder.unparsed("max", String.valueOf(rule.perRound()))));
            if (rule.cooldown() > 0) {
                lore.add(Texts.text(lang, "menu.helps.cooldown", Placeholder.unparsed("secs",
                        String.valueOf(rule.cooldown()))));
            }
            lore.add(Component.empty());
            lore.add(Texts.text(lang, "menu.helps.state." + v.name().toLowerCase(java.util.Locale.ROOT).replace('_', '-'),
                    Placeholder.unparsed("n", String.valueOf(Math.max(0, rule.price() - m.available()))),
                    Placeholder.unparsed("secs", String.valueOf(g.helpCooldown(p.getUniqueId(), h)))));
            Material icon = switch (h) {
                case LOCKPFIFF -> Material.AMETHYST_SHARD;
                case NEBELWOLKE -> Material.WHITE_WOOL;
                case GESTALTWECHSEL -> Material.ARMOR_STAND;
                case LATERNE -> Material.LANTERN;
                case SPUERNASE -> Material.BONE;
            };
            menu.set(SLOTS[i], Items.glow(Items.named(icon, 1, Texts.text(lang, "help." + h.id()), lore),
                    v == Helps.Verdict.OK), q -> {
                        q.closeInventory();
                        g.help(q, h);
                    });
        }
        menu.set(CLOSE, Items.named(Material.BARRIER, 1, Texts.text(lang, "menu.close"), null), Player::closeInventory);
    }
}
