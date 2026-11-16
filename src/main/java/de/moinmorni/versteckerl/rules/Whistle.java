package de.moinmorni.versteckerl.rules;

/**
 * Pfiff-Pflicht: Ab einem Anteil der Suchzeit muss jeder Versteckte regelmäßig pfeifen. Wer zu lange still ist, pfeift
 * von selbst – ohne Punkte. Alles in Ticks (20 pro Sekunde).
 */
public final class Whistle {

    private Whistle() {
    }

    /** Ab welchem Tick der Suchzeit gilt die Pflicht? */
    public static long mandatoryFrom(long seekTicks, int percent) {
        return seekTicks * Math.max(0, Math.min(100, percent)) / 100;
    }

    /**
     * Ticks bis zum nächsten Pflicht-Pfiff (0 = jetzt fällig), oder -1, wenn noch keine Pflicht gilt.
     *
     * @param elapsed       Ticks seit Beginn der Suche
     * @param from          Pflicht ab diesem Tick ({@link #mandatoryFrom})
     * @param lastWhistle   Tick (der Suche) des letzten Pfiffs, -1 = noch nie
     * @param interval      so oft muss gepfiffen werden
     */
    public static long dueIn(long elapsed, long from, long lastWhistle, long interval) {
        if (elapsed < from) {
            return -1;
        }
        long since = Math.max(from, lastWhistle);
        return Math.max(0, since + interval - elapsed);
    }
}
