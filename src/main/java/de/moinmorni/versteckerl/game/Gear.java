package de.moinmorni.versteckerl.game;

import de.moinmorni.versteckerl.Items;
import de.moinmorni.versteckerl.Texts;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

/**
 * Die Hilfen in der Schnellleiste (Rechtsklick – geht auch auf Bedrock): Versteckte haben Gestalt, Pfiff und
 * Sprungkraft, Gendarmen den Gendarmenstab und die Spürnase. Beide haben dazu die Kramerkiste (Rundenladen), wenn er
 * offen ist.
 */
public final class Gear {

    public static final int SLOT_SHAPE = 0;
    public static final int SLOT_WHISTLE = 1;
    public static final int SLOT_JUMP = 2;
    public static final int SLOT_HIDER_HELPS = 3;
    public static final int SLOT_STICK = 0;
    public static final int SLOT_SNIFF = 1;
    public static final int SLOT_SEEKER_HELPS = 2;

    private Gear() {
    }

    static void hider(Player p, String lang, boolean seeking, boolean changeUsed, boolean helps) {
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
        if (helps) {
            inv.setItem(SLOT_HIDER_HELPS, helpsItem(lang, "item.helps.hider.lore"));
        }
        inv.setHeldItemSlot(8);
    }

    private static ItemStack helpsItem(String lang, String lore) {
        return Items.kitTag(Items.named(Material.CHEST, 1, Texts.text(lang, "item.helps"), Texts.lines(lang, lore)),
                "helps");
    }

    static void seeker(Player p, String lang, boolean sniffUsed, boolean helps) {
        PlayerInventory inv = p.getInventory();
        inv.clear();
        inv.setItem(SLOT_STICK, Items.kitTag(Items.named(Material.STICK, 1, Texts.text(lang, "item.stick"),
                Texts.lines(lang, "item.stick.lore")), "stick"));
        inv.setItem(SLOT_SNIFF, Items.glow(Items.kitTag(Items.named(Material.BONE, 1, Texts.text(lang, "item.sniff"),
                Texts.lines(lang, sniffUsed ? "item.sniff.used.lore" : "item.sniff.lore")), "sniff"), !sniffUsed));
        if (helps) {
            inv.setItem(SLOT_SEEKER_HELPS, helpsItem(lang, "item.helps.seeker.lore"));
        }
        inv.setHeldItemSlot(SLOT_STICK);
    }

    static void none(Player p) {
        p.getInventory().clear();
    }
}
