package net.morni.versteckerl;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.morni.versteckerl.rules.Catalog;
import net.morni.versteckerl.rules.Helps;
import net.morni.versteckerl.rules.Rewards;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * Werte aus der config.yml (Erklärungen stehen dort). Punkte ({@code punkte.*}), Rundenladen ({@code rundenladen.*})
 * und Kramerladen ({@code laden.*}) haben ihre Vorgaben im Code ({@link Rewards#DEFAULTS}, {@link Helps#defaults()},
 * {@link Catalog}) – fehlt ein Wert, gilt die Vorgabe.
 */
public record Settings(
        String world,
        int hallCountdown,
        int hallMinPlayers,
        int maxPlayers,
        int seekerPercent,
        int hideSeconds,
        int seekSeconds,
        int afterSeconds,
        int lockSeconds,
        int hitsToFind,
        double wrongDamage,
        int stunSeconds,
        int whistleCooldown,
        int whistleMandatoryPercent,
        int whistleInterval,
        int jumpSeconds,
        int jumpCooldown,
        int sniffSeconds,
        Rewards rewards,
        boolean roundShop,
        Map<Helps.Help, Helps.Rule> helps,
        boolean shop,
        Map<String, Integer> prices,
        Set<String> off) {

    private static final int MAX_POINTS = 1000;
    private static final int MAX_PRICE = 100_000;

    public static Settings load(FileConfiguration c) {
        return new Settings(
                c.getString("welt", "versteckerl"),
                clamp(c.getInt("gendarmerie.countdown-sekunden", 30), 3, 300),
                clamp(c.getInt("gendarmerie.mindestens-spieler", 4), 2, 16),
                clamp(c.getInt("runde.hoechstens-spieler", 16), 2, 16),
                clamp(c.getInt("runde.sucher-anteil-prozent", 25), 5, 75),
                clamp(c.getInt("runde.versteck-sekunden", 40), 5, 300),
                clamp(c.getInt("runde.such-sekunden", 300), 20, 1800),
                clamp(c.getInt("runde.nachspiel-sekunden", 10), 1, 60),
                clamp(c.getInt("runde.einrasten-sekunden", 3), 1, 20),
                clamp(c.getInt("runde.treffer-bis-gefunden", 3), 1, 10),
                Math.max(0, Math.min(20, c.getDouble("runde.falscher-treffer-schaden", 2))),
                clamp(c.getInt("runde.verschnaufen-sekunden", 4), 1, 30),
                clamp(c.getInt("hilfen.pfiff-abklingzeit-sekunden", 15), 1, 300),
                clamp(c.getInt("hilfen.pflicht-pfiff-ab-prozent", 50), 0, 100),
                clamp(c.getInt("hilfen.pflicht-pfiff-sekunden", 30), 5, 600),
                clamp(c.getInt("hilfen.sprung-sekunden", 4), 1, 30),
                clamp(c.getInt("hilfen.sprung-abklingzeit-sekunden", 25), 1, 600),
                clamp(c.getInt("hilfen.spuernase-sekunden", 4), 1, 30),
                rewards(c),
                c.getBoolean("rundenladen.an", true),
                helps(c),
                c.getBoolean("laden.an", true),
                prices(c),
                off(c));
    }

    private static Rewards rewards(FileConfiguration c) {
        Rewards d = Rewards.DEFAULTS;
        return new Rewards(
                points(c, "treffer", d.hit()),
                points(c, "fund", d.find()),
                points(c, "erster-fund", d.firstFind()),
                points(c, "gendarmen-sieg", d.seekersWin()),
                points(c, "je-halbe-minute", d.perHalfMinute()),
                clamp(c.getInt("punkte.pfiff-hoechstens", d.whistleMax()), 1, MAX_POINTS),
                clamp(c.getInt("punkte.mutig-nah-bloecke", d.braveNearBlocks()), 1, 64),
                points(c, "mutig-nah", d.braveNear()),
                clamp(c.getInt("punkte.mutig-bloecke", d.braveBlocks()), 1, 64),
                points(c, "mutig", d.brave()),
                points(c, "pfiff-laut", d.loud()),
                points(c, "pfiff-sehr-laut", d.veryLoud()),
                points(c, "letzter-versteckter", d.lastHider()),
                points(c, "bis-zum-schluss", d.survive()),
                points(c, "serie-je-sieg", d.streakPerWin()),
                clamp(c.getInt("punkte.serie-hoechstens", d.streakMaxWins()), 0, 50),
                points(c, "tagesbonus", d.daily()));
    }

    private static int points(FileConfiguration c, String key, int fallback) {
        return clamp(c.getInt("punkte." + key, fallback), 0, MAX_POINTS);
    }

    private static Map<Helps.Help, Helps.Rule> helps(FileConfiguration c) {
        Map<Helps.Help, Helps.Rule> out = new EnumMap<>(Helps.Help.class);
        for (var e : Helps.defaults().entrySet()) {
            String p = "rundenladen." + e.getKey().id() + ".";
            Helps.Rule d = e.getValue();
            out.put(e.getKey(), new Helps.Rule(
                    c.getBoolean(p + "an", d.on()),
                    clamp(c.getInt(p + "preis", d.price()), 0, MAX_POINTS),
                    clamp(c.getInt(p + "abklingzeit-sekunden", d.cooldown()), 0, 600),
                    clamp(c.getInt(p + "je-runde", d.perRound()), 0, 20),
                    clamp(c.getInt(p + "sekunden", Math.max(1, d.seconds())), 1, 30),
                    clamp(c.getInt(p + "reichweite", Math.max(1, d.range())), 1, 48)));
        }
        return Collections.unmodifiableMap(out);
    }

    private static Map<String, Integer> prices(FileConfiguration c) {
        Map<String, Integer> out = new HashMap<>();
        for (Catalog.Item i : Catalog.all()) {
            if (!i.free()) {
                out.put(i.key(), clamp(c.getInt(path(i) + ".preis", i.price()), 0, MAX_PRICE));
            }
        }
        return Collections.unmodifiableMap(out);
    }

    private static Set<String> off(FileConfiguration c) {
        Set<String> out = new HashSet<>();
        for (Catalog.Item i : Catalog.all()) {
            if (!i.free() && !c.getBoolean(path(i) + ".an", true)) {
                out.add(i.key());
            }
        }
        return Collections.unmodifiableSet(out);
    }

    /** Pfad eines Stücks in der config.yml: {@code laden.pfiffe.kuhglocke}. */
    public static String path(Catalog.Item i) {
        return "laden." + i.kind().section() + "." + i.id();
    }

    /** Preis eines Stücks im Kramerladen (frei = 0). */
    public int price(Catalog.Item i) {
        return i.free() ? 0 : prices.getOrDefault(i.key(), i.price());
    }

    /**
     * Gibt es das Stück (Laden an und Stück nicht abgeschaltet)? Abgeschaltetes kann man weder kaufen noch benutzen –
     * wer es schon hat, behält es für später, falls es wieder angeschaltet wird.
     */
    public boolean offered(Catalog.Item i) {
        return i.free() || (shop && !off.contains(i.key()));
    }

    public Helps.Rule help(Helps.Help h) {
        return helps.get(h);
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    /** Für Tests ohne config.yml: alles Vorgabe. */
    static Settings defaults() {
        return load(new org.bukkit.configuration.file.YamlConfiguration());
    }
}
