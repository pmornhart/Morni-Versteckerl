package net.morni.versteckerl.hall;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.morni.versteckerl.Settings;
import net.morni.versteckerl.Texts;
import net.morni.versteckerl.Ui;
import net.morni.versteckerl.VersteckerlPlugin;
import net.morni.versteckerl.data.PlayerData;
import net.morni.versteckerl.game.Flair;
import net.morni.versteckerl.rules.Catalog;
import net.morni.versteckerl.rules.Shape;
import org.bukkit.entity.Player;

/**
 * Der Kramerladen: Freischalten mit Guthaben und Ausrüsten. Ein Kauf braucht zwei Klicks (der erste zeigt Preis und
 * Vorschau, der zweite kauft) – so kauft niemand aus Versehen. Gekauft wird in einem Schritt
 * ({@link PlayerData#buy}): prüfen, abbuchen, freischalten; das Guthaben wird nie negativ. Danach wird gleich
 * gespeichert und dem Netzwerk gemeldet.
 */
public final class Shop {

    /** Zustand eines Stücks für eine Person. */
    public enum State {
        /** gerade ausgerüstet */
        EQUIPPED,
        /** gehört einem (oder ist frei) – Klick rüstet aus */
        OWNED,
        /** genug Guthaben – Klick kauft (mit Nachfrage) */
        BUY,
        /** zu wenig Guthaben */
        TOO_POOR,
        /** Bedingung noch nicht erfüllt (z. B. erst ab 25 Funden) */
        NEED,
        /** im Laden abgeschaltet */
        OFF
    }

    private record Pending(String key, long until) {
    }

    private static final long CONFIRM_MS = 8000;

    private final VersteckerlPlugin plugin;
    private final Map<UUID, Pending> pending = new HashMap<>();

    public Shop(VersteckerlPlugin plugin) {
        this.plugin = plugin;
    }

    private Settings settings() {
        return plugin.settings();
    }

    /** Ist der Kramerladen offen (config.yml {@code laden.an})? */
    public boolean open() {
        return settings().shop();
    }

    public int price(Catalog.Item i) {
        return settings().price(i);
    }

    /** Darf jemand diese Gestalt nehmen? Frei – oder freigeschaltet und im Laden nicht abgeschaltet. */
    public boolean usable(PlayerData d, Shape shape) {
        if (shape.free()) {
            return true;
        }
        Catalog.Item i = Catalog.get(Catalog.Kind.SHAPE, shape.id());
        return i != null && settings().offered(i) && d.owns(i.key());
    }

    /**
     * Ausgerüstetes Stück einer Abteilung; abgeschaltet oder nicht (mehr) da → das Standard-Stück. Gestalten: die
     * Lieblingsgestalt (oder null).
     */
    public Catalog.Item equipped(PlayerData d, Catalog.Kind kind) {
        Catalog.Item i = Catalog.get(kind, d.equipped(kind));
        if (i != null && (i.free() || (settings().offered(i) && d.owns(i.key())))) {
            return i;
        }
        return Catalog.standard(kind);
    }

    /** Id des Ausgerüsteten (Standard, wenn nichts) – für Klänge und Effekte. */
    public String equippedId(PlayerData d, Catalog.Kind kind) {
        Catalog.Item i = equipped(d, kind);
        return i == null ? "" : i.id();
    }

    public State state(PlayerData d, Catalog.Item i) {
        if (!settings().offered(i)) {
            return State.OFF;
        }
        Catalog.Item eq = equipped(d, i.kind());
        if (eq != null && eq.key().equals(i.key())) {
            return State.EQUIPPED;
        }
        if (i.free() || d.owns(i.key())) {
            return State.OWNED;
        }
        if (i.need() != null && d.stat(i.need().stat()) < i.need().min()) {
            return State.NEED;
        }
        return d.balance() >= price(i) ? State.BUY : State.TOO_POOR;
    }

    /** Wartet ein Kauf auf den zweiten Klick? */
    public boolean confirming(UUID uuid, Catalog.Item i) {
        Pending p = pending.get(uuid);
        return p != null && p.key().equals(i.key()) && System.currentTimeMillis() <= p.until();
    }

    /** Klick auf ein Stück im Laden. */
    public void click(Player p, Catalog.Item i) {
        PlayerData d = plugin.data(p);
        String lang = plugin.lang(p);
        String name = name(lang, i);
        switch (state(d, i)) {
            case OFF -> p.sendMessage(Texts.msg(lang, "shop.off", Placeholder.unparsed("item", name)));
            case EQUIPPED -> {
                if (i.free()) {
                    p.sendMessage(Texts.msg(lang, "shop.already", Placeholder.unparsed("item", name)));
                } else {
                    d.equip(i.kind(), null);
                    plugin.store().save(d);
                    p.sendMessage(Texts.msg(lang, i.kind() == Catalog.Kind.SHAPE ? "shop.favourite.off" : "shop.unequipped",
                            Placeholder.unparsed("item", name)));
                    Ui.sound(p, "minecraft:item.armor.equip_leather", 0.8f);
                }
            }
            case OWNED -> {
                d.equip(i.kind(), i.free() ? null : i.id());
                plugin.store().save(d);
                p.sendMessage(Texts.msg(lang, i.kind() == Catalog.Kind.SHAPE ? "shop.favourite" : "shop.equipped",
                        Placeholder.unparsed("item", name)));
                Ui.sound(p, "minecraft:item.armor.equip_leather", 1.2f);
                Flair.preview(plugin, p, i);
            }
            case NEED -> p.sendMessage(Texts.msg(lang, "shop.need", Placeholder.unparsed("item", name),
                    Placeholder.unparsed("need", need(lang, i, d))));
            case TOO_POOR -> {
                p.sendMessage(Texts.msg(lang, "shop.too-poor", Placeholder.unparsed("item", name),
                        Placeholder.unparsed("n", String.valueOf(price(i) - d.balance()))));
                Ui.sound(p, "minecraft:entity.villager.no", 1f);
                Flair.preview(plugin, p, i);
            }
            case BUY -> {
                if (confirming(p.getUniqueId(), i)) {
                    pending.remove(p.getUniqueId());
                    buy(p, d, i);
                } else {
                    pending.put(p.getUniqueId(), new Pending(i.key(), System.currentTimeMillis() + CONFIRM_MS));
                    p.sendMessage(Texts.msg(lang, "shop.confirm", Placeholder.unparsed("item", name),
                            Placeholder.unparsed("n", String.valueOf(price(i)))));
                    Ui.sound(p, "minecraft:ui.button.click", 1.2f);
                    Flair.preview(plugin, p, i);
                }
            }
            default -> {
            }
        }
    }

    private void buy(Player p, PlayerData d, Catalog.Item i) {
        String lang = plugin.lang(p);
        int price = price(i);
        switch (d.buy(i.key(), price)) {
            case OK -> {
                d.equip(i.kind(), i.id());
                plugin.store().save(d);
                Map<String, Long> add = new LinkedHashMap<>();
                add.put("unlocks", 1L);
                add.put("spent", (long) price);
                plugin.net().stats(p.getUniqueId(), add, Map.of());
                p.sendMessage(Texts.msg(lang, i.kind() == Catalog.Kind.SHAPE ? "shop.bought.shape" : "shop.bought",
                        Placeholder.unparsed("item", name(lang, i)), Placeholder.unparsed("n", String.valueOf(price)),
                        Placeholder.unparsed("balance", String.valueOf(d.balance()))));
                Ui.sound(p, "minecraft:entity.villager.celebrate", 1f);
                Ui.sound(p, "minecraft:entity.player.levelup", 1.4f);
                Flair.preview(plugin, p, i);
                plugin.getLogger().info(p.getName() + " kauft " + i.key() + " für " + price + " (Guthaben jetzt "
                        + d.balance() + ").");
                plugin.boards().hall(p);
            }
            case OWNED -> p.sendMessage(Texts.msg(lang, "shop.owned", Placeholder.unparsed("item", name(lang, i))));
            case TOO_POOR -> p.sendMessage(Texts.msg(lang, "shop.too-poor", Placeholder.unparsed("item", name(lang, i)),
                    Placeholder.unparsed("n", String.valueOf(price - d.balance()))));
            default -> {
            }
        }
    }

    public void forget(UUID uuid) {
        pending.remove(uuid);
    }

    // ---------- Namen ----------

    /** Name eines Stücks in der Sprache. */
    public static String name(String lang, Catalog.Item i) {
        return Texts.plain(lang, i.kind() == Catalog.Kind.SHAPE ? "shape." + i.id() : nameKey(i));
    }

    /** Text-Schlüssel des Namens ({@code shop.pfiff.kuhglocke}). */
    public static String nameKey(Catalog.Item i) {
        return i.kind() == Catalog.Kind.SHAPE ? "shape." + i.id() : "shop." + i.kind().prefix() + "." + i.id();
    }

    /** Text-Schlüssel der Beschreibung. */
    public static String descKey(Catalog.Item i) {
        return i.kind() == Catalog.Kind.SHAPE ? "shape." + i.id() + ".hint" : nameKey(i) + ".desc";
    }

    /** Bedingung als Text: „25 Funde (du hast 12)“. */
    public static String need(String lang, Catalog.Item i, PlayerData d) {
        if (i.need() == null) {
            return "";
        }
        return Texts.plain(lang, "shop.need." + i.need().stat().id(), Placeholder.unparsed("n",
                String.valueOf(i.need().min())), Placeholder.unparsed("have", String.valueOf(d.stat(i.need().stat()))));
    }

    /** Titel mit Zeichen davor („❀ Meister der Tarnung“), leer ohne Titel. */
    public String title(PlayerData d, String lang) {
        Catalog.Item t = equipped(d, Catalog.Kind.TITLE);
        if (t == null || t.free()) {
            return "";
        }
        return t.symbol() + " " + name(lang, t);
    }
}
