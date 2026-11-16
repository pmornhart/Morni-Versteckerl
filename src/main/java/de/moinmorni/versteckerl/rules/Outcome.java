package de.moinmorni.versteckerl.rules;

/** Wer hat gewonnen? Versteckte, wenn bei Ablauf noch jemand versteckt ist; Gendarmen, wenn alle gefunden sind. */
public final class Outcome {

    public enum Winner {
        /** Noch keine Entscheidung. */
        NONE,
        HIDERS,
        SEEKERS
    }

    private Outcome() {
    }

    /**
     * @param hidersLeft    noch versteckt (und noch da)
     * @param seekersLeft   Gendarmen, die noch da sind
     * @param timeUp        Suchzeit abgelaufen
     */
    public static Winner decide(int hidersLeft, int seekersLeft, boolean timeUp) {
        if (hidersLeft <= 0) {
            return seekersLeft > 0 ? Winner.SEEKERS : Winner.NONE;
        }
        if (seekersLeft <= 0 || timeUp) {
            return Winner.HIDERS;
        }
        return Winner.NONE;
    }
}
