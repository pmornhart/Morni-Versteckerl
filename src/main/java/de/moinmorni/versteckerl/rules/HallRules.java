package de.moinmorni.versteckerl.rules;

/** Wann läuft in der Gendarmerie der Countdown für die nächste Runde? */
public final class HallRules {

    /** Verstecken braucht mindestens zwei: einer sucht, einer versteckt sich. */
    public static final int ABSOLUTE_MIN = 2;

    private HallRules() {
    }

    /**
     * Ab {@code min} Leuten läuft der Countdown von selbst; mit weniger (mindestens zwei) nur, wenn alle bereit sind.
     */
    public static boolean countdown(int waiting, int ready, int min) {
        if (waiting < ABSOLUTE_MIN) {
            return false;
        }
        return waiting >= Math.max(ABSOLUTE_MIN, min) || ready >= waiting;
    }

    /** Sind alle bereit? Dann geht es gleich los. */
    public static boolean allReady(int waiting, int ready) {
        return waiting >= ABSOLUTE_MIN && ready >= waiting;
    }
}
