package net.morni.versteckerl.data;

import java.util.UUID;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * Werte einer Person: Runden, Siege als Versteckte und als Gendarm, Funde, überlebte Runden, Pfiffe, Punkte, beste
 * Runde – und wie oft sie schon Gendarm war (damit alle mal drankommen).
 */
public final class PlayerData {

    /** Ergebnis einer Runde für eine Person. */
    public record Round(boolean winHider, boolean winSeeker, int found, boolean survived, int whistles, int points) {
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

    public int found() {
        return found;
    }

    public int survived() {
        return survived;
    }

    public int whistles() {
        return whistles;
    }

    public long points() {
        return points;
    }

    public int bestPoints() {
        return bestPoints;
    }

    public int seekerRounds() {
        return seekerRounds;
    }

    /** Wurde zu Beginn einer Runde Gendarm. */
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
        d.points = y.getLong("punkte");
        d.bestPoints = y.getInt("beste-runde");
        d.seekerRounds = y.getInt("gendarm-runden");
        return d;
    }
}
