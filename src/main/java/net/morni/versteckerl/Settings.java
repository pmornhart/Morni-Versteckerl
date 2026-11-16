package net.morni.versteckerl;

import org.bukkit.configuration.file.FileConfiguration;

/** Werte aus der config.yml (Erklärungen stehen dort). */
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
        int sniffSeconds) {

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
                clamp(c.getInt("hilfen.spuernase-sekunden", 4), 1, 30));
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }
}
