package net.morni.versteckerl.rules;

/**
 * Wofür es Punkte gibt – alle Werte aus der config.yml ({@code punkte.*}), Vorgaben in {@link #DEFAULTS}.
 *
 * <p>In der Runde: Gendarmen für Treffer, Funde, den ersten Fund der Runde und den Sieg; Versteckte für jede halbe
 * Minute unentdeckt, für Pfiffe (je später, je lauter und je näher ein Gendarm steht, desto mehr), als Letzter noch
 * versteckt und fürs Durchhalten bis zum Schluss. Nach der Runde kommt aufs Guthaben, was übrig ist, dazu der
 * Serien-Bonus (mehrere Siege in Folge) und einmal am Tag der Bonus für den ersten Sieg ({@link Settlement}).
 *
 * @param braveNearBlocks Gendarm höchstens so weit weg → {@code braveNear} Punkte Mut-Bonus für den Pfiff
 * @param braveBlocks     Gendarm höchstens so weit weg → {@code brave} Punkte (wenn nicht schon ganz nah)
 * @param loud            Extrapunkte für einen lauten Pfiff-Klang (Stufe 2)
 * @param veryLoud        Extrapunkte für einen sehr lauten Pfiff-Klang (Stufe 3)
 * @param streakPerWin    Serien-Bonus je weiterem Sieg in Folge …
 * @param streakMaxWins   … aber höchstens für so viele weitere Siege
 */
public record Rewards(int hit, int find, int firstFind, int seekersWin, int perHalfMinute, int whistleMax,
        int braveNearBlocks, int braveNear, int braveBlocks, int brave, int loud, int veryLoud, int lastHider,
        int survive, int streakPerWin, int streakMaxWins, int daily) {

    public static final Rewards DEFAULTS = new Rewards(Points.HIT, Points.FIND, 5, Points.SEEKERS_WIN,
            Points.PER_HALF_MINUTE, Points.WHISTLE_MAX, 6, 4, 12, 2, 2, 4, 10, Points.SURVIVE, 3, 5, 25);

    private static final int HALF_MINUTE = 30;

    /** Grundwert eines freiwilligen Pfiffs {@code seconds} Sekunden nach Beginn der Suche: 1, ab 30 s 2 … */
    public int whistleBase(long seconds) {
        if (seconds < 0) {
            return 0;
        }
        return (int) Math.max(1, Math.min(whistleMax, 1 + seconds / HALF_MINUTE));
    }

    /** Extrapunkte für die Lautstärke des Pfiff-Klangs (1 leise, 2 laut, 3 sehr laut). */
    public int loudness(int level) {
        return level >= Catalog.VERY_LOUD ? veryLoud : level == Catalog.LOUD ? loud : 0;
    }

    /** Mut-Bonus: wie weit der nächste Gendarm weg ist (in Blöcken; unendlich = keiner da). */
    public int courage(double nearestSeeker) {
        if (nearestSeeker <= braveNearBlocks) {
            return braveNear;
        }
        return nearestSeeker <= braveBlocks ? brave : 0;
    }

    /** Ganzer Pfiff: Grundwert + Lautstärke + Mut (0, wenn die Suche noch nicht läuft). */
    public int whistle(long seconds, int loudness, double nearestSeeker) {
        if (seconds < 0) {
            return 0;
        }
        return whistleBase(seconds) + loudness(loudness) + courage(nearestSeeker);
    }

    /** Serien-Bonus beim {@code streak}-ten Sieg in Folge (der erste Sieg zählt noch nicht als Serie). */
    public int streakBonus(int streak) {
        if (streak <= 1) {
            return 0;
        }
        return streakPerWin * Math.min(streak - 1, Math.max(0, streakMaxWins));
    }
}
