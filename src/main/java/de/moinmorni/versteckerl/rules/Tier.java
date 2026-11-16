package de.moinmorni.versteckerl.rules;

/**
 * Seltenheit im Kramerladen: was frei ist, hat jeder; alles andere kauft man mit Punkten. Je seltener, desto teurer –
 * spielerisch hilft nichts davon mehr als das Freie (nur anders aussehen und anders klingen).
 */
public enum Tier {

    /** Hat jeder von Anfang an. */
    FREE("frei"),
    COMMON("gewoehnlich"),
    RARE("selten"),
    EPIC("praechtig");

    private final String id;

    Tier(String id) {
        this.id = id;
    }

    /** Schlüssel für Texte ({@code tier.selten}). */
    public String id() {
        return id;
    }
}
