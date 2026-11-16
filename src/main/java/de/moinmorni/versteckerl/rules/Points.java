package de.moinmorni.versteckerl.rules;

/**
 * Vorgaben für die Punkte einer Runde (die gültigen Werte kommen aus der config.yml, siehe {@link Rewards}). Gendarmen:
 * Treffer und Funde; Versteckte: jede halbe Minute unentdeckt, Pfiffe (je später, desto mehr) und das Überleben bis zum
 * Schluss. Wer gewinnt, bekommt noch etwas obendrauf.
 */
public final class Points {

    /** Gendarm: Treffer auf einen Versteckten (auch ohne Fund). */
    public static final int HIT = 2;
    /** Gendarm: Versteckten gefunden. */
    public static final int FIND = 10;
    /** Gendarmen gewinnen: jeder Gendarm am Ende. */
    public static final int SEEKERS_WIN = 5;
    /** Versteckter: bis zum Schluss nicht gefunden. */
    public static final int SURVIVE = 20;
    /** Versteckter: je volle halbe Minute in der Suchzeit unentdeckt. */
    public static final int PER_HALF_MINUTE = 1;
    /** Höchstens so viele Punkte für einen Pfiff. */
    public static final int WHISTLE_MAX = 10;
    private static final int HALF_MINUTE = 30;

    private Points() {
    }

    /** Punkte für einen freiwilligen Pfiff, {@code seconds} Sekunden nach Beginn der Suche: 1, ab 30 s 2, … höchstens 10. */
    public static int whistle(long seconds) {
        if (seconds < 0) {
            return 0;
        }
        return (int) Math.min(WHISTLE_MAX, 1 + seconds / HALF_MINUTE);
    }

    /** Ist nach {@code seconds} Sekunden Suche gerade eine neue halbe Minute voll (Punkt fürs Durchhalten)? */
    public static boolean halfMinute(long seconds) {
        return seconds > 0 && seconds % HALF_MINUTE == 0;
    }
}
