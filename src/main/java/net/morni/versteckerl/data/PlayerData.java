package net.morni.versteckerl.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.morni.versteckerl.rules.Catalog;
import net.morni.versteckerl.rules.Settlement;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * Werte einer Person: Runden, Siege als Versteckte und als Gendarm, Funde, überlebte Runden, Pfiffe, beste Runde –
 * und wie oft sie schon Gendarm war (damit alle mal drankommen).
 *
 * <p>Dazu zwei Zähler für Punkte: <b>Punkte insgesamt</b> ({@link #points()}, alles je Verdiente – sinkt nie, für
 * Steckbriefe und Bestenliste) und das <b>Guthaben</b> ({@link #balance()}, zum Ausgeben im Kramerladen, nie
 * negativ). Freigeschaltetes und Ausgerüstetes, die Siegesserie und der Tag des letzten Tagesbonus stehen auch hier.
 */
public final class PlayerData {

    /** Ergebnis einer Runde für eine Person. */
    public record Round(boolean winHider, boolean winSeeker, int found, boolean survived, int whistles, int points) {
    }

    /** Ausgang eines Kaufs. */
    public enum Purchase {
        OK,
        /** hat es schon (es wird nichts abgebucht) */
        OWNED,
        /** zu wenig Guthaben (es wird nichts abgebucht) */
        TOO_POOR,
        /** ungültiger Preis */
        INVALID
    }

    private final UUID uuid;
    private String name;
    private int games;
    private int winsHider;
    private int winsSeeker;
    private int found;
    private int survived;
    private int whistles;
    private long points;
    private int bestPoints;
    private int seekerRounds;
    private long balance;
    private long spent;
    private int helps;
    private int streak;
    private int bestStreak;
    private String dailyDay = "";
    private final Set<String> owned = new LinkedHashSet<>();
    private final Map<String, String> equipped = new LinkedHashMap<>();

    public PlayerData(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
    }

    public UUID uuid() {
        return uuid;
    }

    public String name() {
        return name;
    }

    public void name(String n) {
        if (n != null && !n.isBlank()) {
            name = n;
        }
    }

    public int games() {
        return games;
    }

    public int winsHider() {
        return winsHider;
    }

    public int winsSeeker() {
        return winsSeeker;
    }

    public int wins() {
        return winsHider + winsSeeker;
    }

    public int found() {
        return found;
    }

    public int survived() {
        return survived;
    }

    public int whistles() {
        return whistles;
    }

    /** Punkte insgesamt: alles, was je verdient wurde (Runden und Boni) – sinkt nie, auch nicht beim Einkaufen. */
    public long points() {
        return points;
    }

    public int bestPoints() {
        return bestPoints;
    }

    public int seekerRounds() {
        return seekerRounds;
    }

    /** Guthaben zum Ausgeben im Kramerladen. */
    public long balance() {
        return balance;
    }

    /** Im Kramerladen insgesamt ausgegeben. */
    public long spent() {
        return spent;
    }

    /** Hilfen, die in Runden gekauft wurden. */
    public int helps() {
        return helps;
    }

    /** Siege in Folge (0 nach einer Niederlage). */
    public int streak() {
        return streak;
    }

    public int bestStreak() {
        return bestStreak;
    }

    /** Wurde jemand zu Beginn einer Runde Gendarm. */
    public void becameSeeker() {
        seekerRounds++;
    }

    /** Eine Runde zu Ende: Werte hochzählen; liefert {@code true} bei neuer bester Runde. */
    public boolean addRound(Round r) {
        games++;
        winsHider += r.winHider() ? 1 : 0;
        winsSeeker += r.winSeeker() ? 1 : 0;
        found += Math.max(0, r.found());
        survived += r.survived() ? 1 : 0;
        whistles += Math.max(0, r.whistles());
        points += Math.max(0, r.points());
        if (r.points() > bestPoints) {
            bestPoints = r.points();
            return true;
        }
        return false;
    }

    /**
     * Abrechnung einer Runde übernehmen: Guthaben dazu, Boni zu „Punkte insgesamt“ (die Punkte der Runde selbst zählt
     * schon {@link #addRound}), Serie, Tagesbonus ({@code today} z. B. „2026-11-16“) und gekaufte Hilfen.
     */
    public void settle(Settlement s, String today, int helpsBought) {
        balance += Math.max(0, s.credit());
        points += Math.max(0, s.bonus());
        streak = Math.max(0, s.streak());
        bestStreak = Math.max(bestStreak, streak);
        if (s.daily() > 0 && today != null) {
            dailyDay = today;
        }
        helps += Math.max(0, helpsBought);
    }

    /** Gibt es heute noch den Bonus für den ersten Sieg? */
    public boolean dailyOpen(String today) {
        return today != null && !today.equals(dailyDay);
    }

    /** Tag des letzten Tagesbonus („“ = noch nie). */
    public String dailyDay() {
        return dailyDay;
    }

    /**
     * Guthaben ändern (Verwaltung): plus oder minus, aber nie unter 0. Liefert das neue Guthaben. Zu „Punkte insgesamt“
     * zählt das nicht.
     */
    public long adjust(long delta) {
        balance = Math.max(0, balance + delta);
        return balance;
    }

    /**
     * Kaufen: in einem Schritt prüfen, abbuchen und freischalten. Bei {@link Purchase#OWNED}, {@link Purchase#TOO_POOR}
     * oder {@link Purchase#INVALID} ändert sich nichts.
     */
    public Purchase buy(String key, long price) {
        String k = normal(key);
        if (k.isEmpty() || price < 0) {
            return Purchase.INVALID;
        }
        if (owned.contains(k)) {
            return Purchase.OWNED;
        }
        if (balance < price) {
            return Purchase.TOO_POOR;
        }
        balance -= price;
        spent += price;
        owned.add(k);
        return Purchase.OK;
    }

    /** Schon freigeschaltet (gekauft)? Freie Standard-Stücke zählen nicht – die hat jeder. */
    public boolean owns(String key) {
        return owned.contains(normal(key));
    }

    /** Freigeschaltetes in Kaufreihenfolge. */
    public Set<String> owned() {
        return Collections.unmodifiableSet(owned);
    }

    /** Was in einer Abteilung ausgerüstet ist (Id, z. B. „kuhglocke“) oder null. */
    public String equipped(Catalog.Kind kind) {
        return equipped.get(kind.prefix());
    }

    /** Ausrüsten ({@code id} null = ablegen, also wieder das Standard-Stück). */
    public void equip(Catalog.Kind kind, String id) {
        if (id == null || id.isBlank()) {
            equipped.remove(kind.prefix());
        } else {
            equipped.put(kind.prefix(), id.toLowerCase(Locale.ROOT));
        }
    }

    public Map<String, String> equippedAll() {
        return Collections.unmodifiableMap(equipped);
    }

    /** Wert für eine Bedingung im Laden („erst ab 25 Funden“). */
    public int stat(Catalog.Stat stat) {
        return switch (stat) {
            case ROUNDS -> games;
            case FOUND -> found;
            case SURVIVED -> survived;
            case WHISTLES -> whistles;
            case WINS -> wins();
        };
    }

    private static String normal(String key) {
        return key == null ? "" : key.trim().toLowerCase(Locale.ROOT);
    }

    public String toYaml() {
        YamlConfiguration y = new YamlConfiguration();
        y.set("name", name);
        y.set("runden", games);
        y.set("siege-versteckt", winsHider);
        y.set("siege-gendarm", winsSeeker);
        y.set("gefunden", found);
        y.set("ueberlebt", survived);
        y.set("pfiffe", whistles);
        y.set("punkte", points);
        y.set("beste-runde", bestPoints);
        y.set("gendarm-runden", seekerRounds);
        y.set("guthaben", balance);
        y.set("ausgegeben", spent);
        y.set("hilfen", helps);
        y.set("serie", streak);
        y.set("beste-serie", bestStreak);
        y.set("tagesbonus", dailyDay);
        y.set("freigeschaltet", new ArrayList<>(owned));
        if (!equipped.isEmpty()) {
            y.set("ausgeruestet", new LinkedHashMap<>(equipped));
        }
        return y.saveToString();
    }

    public static PlayerData fromYaml(UUID uuid, String text) {
        YamlConfiguration y = new YamlConfiguration();
        try {
            y.loadFromString(text);
        } catch (InvalidConfigurationException e) {
            throw new IllegalArgumentException(e);
        }
        PlayerData d = new PlayerData(uuid, y.getString("name", "?"));
        d.games = y.getInt("runden");
        d.winsHider = y.getInt("siege-versteckt");
        d.winsSeeker = y.getInt("siege-gendarm");
        d.found = y.getInt("gefunden");
        d.survived = y.getInt("ueberlebt");
        d.whistles = y.getInt("pfiffe");
        d.points = Math.max(0, y.getLong("punkte"));
        d.bestPoints = y.getInt("beste-runde");
        d.seekerRounds = y.getInt("gendarm-runden");
        d.balance = Math.max(0, y.getLong("guthaben"));
        d.spent = Math.max(0, y.getLong("ausgegeben"));
        d.helps = Math.max(0, y.getInt("hilfen"));
        d.streak = Math.max(0, y.getInt("serie"));
        d.bestStreak = Math.max(d.streak, y.getInt("beste-serie"));
        d.dailyDay = y.getString("tagesbonus", "");
        for (String k : y.getStringList("freigeschaltet")) {
            String n = normal(k);
            if (!n.isEmpty()) {
                d.owned.add(n);
            }
        }
        ConfigurationSection eq = y.getConfigurationSection("ausgeruestet");
        if (eq != null) {
            for (String kind : eq.getKeys(false)) {
                String id = eq.getString(kind);
                if (id != null && !id.isBlank() && Catalog.Kind.byPrefix(kind) != null) {
                    d.equipped.put(Catalog.Kind.byPrefix(kind).prefix(), id.toLowerCase(Locale.ROOT));
                }
            }
        }
        return d;
    }
}
