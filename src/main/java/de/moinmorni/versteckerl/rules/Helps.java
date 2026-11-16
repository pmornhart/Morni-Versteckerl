package de.moinmorni.versteckerl.rules;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * Der kleine Laden während der Runde: Hilfen für Punkte aus der laufenden Runde. Jede Hilfe ist begrenzt (Preis,
 * Abklingzeit, wie oft pro Runde) und hat ein Gegenmittel der anderen Seite. Ohne Server.
 *
 * <ul>
 *   <li>Versteckte: <b>Lockpfiff</b> (der eigene Pfiff erklingt woanders – keine Punkte, zählt nicht als Pflicht-Pfiff),
 *       <b>Nebelwolke</b> (kurz Nebel um einen; Gendarmen darin sehen kurz nichts und werden langsam),
 *       <b>Gestaltwechsel</b> (noch einmal die Gestalt wechseln, wenn der eine Wechsel schon weg ist).</li>
 *   <li>Gendarmen: <b>Laterne</b> (zeigt ein paar Sekunden, wer sich in der Nähe bewegt – Eingerastete nicht),
 *       <b>Spürnase</b> (noch einmal schnuppern, wenn die eine schon benutzt ist).</li>
 * </ul>
 */
public final class Helps {

    /** Die Hilfen; {@code hider} = für Versteckte, sonst für Gendarmen. */
    public enum Help {
        LOCKPFIFF("lockpfiff", true),
        NEBELWOLKE("nebelwolke", true),
        GESTALTWECHSEL("gestaltwechsel", true),
        LATERNE("laterne", false),
        SPUERNASE("spuernase", false);

        private final String id;
        private final boolean hider;

        Help(String id, boolean hider) {
            this.id = id;
            this.hider = hider;
        }

        public String id() {
            return id;
        }

        public boolean forHiders() {
            return hider;
        }

        public static Help byId(String id) {
            for (Help h : values()) {
                if (h.id.equalsIgnoreCase(id) || h.name().equalsIgnoreCase(id)) {
                    return h;
                }
            }
            return null;
        }
    }

    /**
     * Regeln einer Hilfe (aus der config.yml {@code rundenladen.<id>.*}).
     *
     * @param price    Preis in Punkten der Runde
     * @param cooldown Sekunden bis zum nächsten Mal
     * @param perRound höchstens so oft pro Runde
     * @param seconds  wie lange sie wirkt (Nebelwolke, Laterne)
     * @param range    Reichweite in Blöcken (Nebelwolke, Laterne)
     */
    public record Rule(boolean on, int price, int cooldown, int perRound, int seconds, int range) {
    }

    /** Warum es (nicht) geht. */
    public enum Verdict {
        OK,
        /** abgeschaltet */
        OFF,
        /** erst, wenn die Suche läuft */
        NOT_YET,
        /** nicht für diese Seite */
        WRONG_ROLE,
        /** wird gerade nicht gebraucht (z. B. der eine Gestaltwechsel ist noch frei) */
        NOT_NEEDED,
        /** schon so oft wie erlaubt */
        USED_UP,
        /** Abklingzeit läuft noch */
        COOLDOWN,
        /** zu wenig Punkte übrig */
        TOO_POOR
    }

    private static final Map<Help, Rule> DEFAULTS;

    static {
        Map<Help, Rule> m = new EnumMap<>(Help.class);
        m.put(Help.LOCKPFIFF, new Rule(true, 6, 30, 2, 0, 12));
        m.put(Help.NEBELWOLKE, new Rule(true, 8, 45, 1, 4, 4));
        m.put(Help.GESTALTWECHSEL, new Rule(true, 10, 0, 1, 0, 0));
        m.put(Help.LATERNE, new Rule(true, 8, 40, 2, 5, 16));
        m.put(Help.SPUERNASE, new Rule(true, 12, 0, 1, 0, 0));
        DEFAULTS = Collections.unmodifiableMap(m);
    }

    private Helps() {
    }

    /** Vorgaben aller Hilfen. */
    public static Map<Help, Rule> defaults() {
        return DEFAULTS;
    }

    /**
     * Darf jemand die Hilfe jetzt kaufen (und gleich benutzen)? Reihenfolge der Gründe: abgeschaltet, falsche Seite,
     * zu früh, nicht nötig, aufgebraucht, Abklingzeit, zu wenig Punkte.
     *
     * @param hider         spielt gerade als Versteckter
     * @param seeking       die Suche läuft
     * @param needed        wird die Hilfe gerade gebraucht?
     * @param available     Punkte der Runde, die noch übrig sind
     * @param used          so oft schon in dieser Runde gekauft
     * @param cooldownTicks Ticks bis zum nächsten Mal (0 = bereit)
     */
    public static Verdict check(Help help, Rule rule, boolean hider, boolean seeking, boolean needed, int available,
            int used, long cooldownTicks) {
        if (rule == null || !rule.on()) {
            return Verdict.OFF;
        }
        if (help.forHiders() != hider) {
            return Verdict.WRONG_ROLE;
        }
        if (!seeking) {
            return Verdict.NOT_YET;
        }
        if (!needed) {
            return Verdict.NOT_NEEDED;
        }
        if (used >= rule.perRound()) {
            return Verdict.USED_UP;
        }
        if (cooldownTicks > 0) {
            return Verdict.COOLDOWN;
        }
        if (available < rule.price()) {
            return Verdict.TOO_POOR;
        }
        return Verdict.OK;
    }
}
