package net.morni.versteckerl.rules;

/**
 * Abrechnung am Ende einer Runde: Was aus der Runde übrig ist (gesammelte Punkte minus das, was man im Rundenladen
 * für Hilfen ausgegeben hat), dazu der Serien-Bonus und einmal am Tag der Bonus für den ersten Sieg – das kommt aufs
 * Guthaben. Ohne Server.
 *
 * @param round       in der Runde gesammelt (zählt für Rangliste, beste Runde und „Punkte insgesamt“)
 * @param spent       davon in der Runde ausgegeben
 * @param streak      Siege in Folge nach dieser Runde (0 nach einer Niederlage)
 * @param streakBonus Serien-Bonus
 * @param daily       Tagesbonus (erster Sieg des Tages), sonst 0
 * @param credit      aufs Guthaben, nie negativ
 */
public record Settlement(int round, int spent, int streak, int streakBonus, int daily, int credit) {

    /**
     * @param roundPoints  in der Runde gesammelte Punkte
     * @param spent        davon ausgegeben
     * @param won          gewonnen?
     * @param streakBefore Siege in Folge vor dieser Runde
     * @param dailyOpen    Tagesbonus heute noch nicht bekommen?
     */
    public static Settlement of(int roundPoints, int spent, boolean won, int streakBefore, boolean dailyOpen,
            Rewards r) {
        int round = Math.max(0, roundPoints);
        int used = Math.max(0, Math.min(spent, round));
        int streak = won ? Math.max(0, streakBefore) + 1 : 0;
        int streakBonus = won ? Math.max(0, r.streakBonus(streak)) : 0;
        int daily = won && dailyOpen ? Math.max(0, r.daily()) : 0;
        return new Settlement(round, used, streak, streakBonus, daily, round - used + streakBonus + daily);
    }

    /** Boni obendrauf (zählen zu „Punkte insgesamt“, nicht zur Runde). */
    public int bonus() {
        return streakBonus + daily;
    }
}
