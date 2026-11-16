package net.morni.versteckerl.game;

import net.morni.versteckerl.Items;
import net.morni.versteckerl.Texts;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

/**
 * Die Hilfen in der Schnellleiste (Rechtsklick – geht auch auf Bedrock): Versteckte haben Gestalt, Pfiff und
 * Sprungkraft, Gendarmen den Gendarmenstab und die Spürnase.
 */
public final class Gear {

    public static final int SLOT_SHAPE = 0;
    public static final int SLOT_WHISTLE = 1;
    public static final int SLOT_JUMP = 2;
    public static final int SLOT_STICK = 0;
    public static final int SLOT_SNIFF = 1;

    private Gear() {
    }

    static void hider(Player p, String lang, boolean seeking, boolean changeUsed) {
        PlayerInventory inv = p.getInventory();
        inv.clear();
        String shapeKey = seeking ? "item.shape.change" : "item.shape";
        ItemStack shape = Items.named(Material.ARMOR_STAND, 1, Texts.text(lang, shapeKey),
                Texts.lines(lang, seeking ? (changeUsed ? "item.shape.used.lore" : "item.shape.change.lore")
                        : "item.shape.lore"));
        inv.setItem(SLOT_SHAPE, Items.glow(Items.kitTag(shape, "shape"), !(seeking && changeUsed)));
        inv.setItem(SLOT_WHISTLE, Items.kitTag(Items.named(Material.AMETHYST_SHARD, 1, Texts.text(lang, "item.whistle"),
                Texts.lines(lang, "item.whistle.lore")), "whistle"));
        inv.setItem(SLOT_JUMP, Items.kitTag(Items.named(Material.RABBIT_FOOT, 1, Texts.text(lang, "item.jump"),
                Texts.lines(lang, "item.jump.lore")), "jump"));
        inv.setHeldItemSlot(8);
    }

    static void seeker(Player p, String lang, boolean sniffUsed) {
        PlayerInventory inv = p.getInventory();
        inv.clear();
        inv.setItem(SLOT_STICK, Items.kitTag(Items.named(Material.STICK, 1, Texts.text(lang, "item.stick"),
                Texts.lines(lang, "item.stick.lore")), "stick"));
        inv.setItem(SLOT_SNIFF, Items.glow(Items.kitTag(Items.named(Material.BONE, 1, Texts.text(lang, "item.sniff"),
                Texts.lines(lang, sniffUsed ? "item.sniff.used.lore" : "item.sniff.lore")), "sniff"), !sniffUsed));
        inv.setHeldItemSlot(SLOT_STICK);
    }

    static void none(Player p) {
        p.getInventory().clear();
    }
}
