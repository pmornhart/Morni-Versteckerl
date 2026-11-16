package de.moinmorni.versteckerl.rules;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/**
 * Wer wird Dorfgendarm (Sucher), wer versteckt sich? Etwa ein Viertel sucht (mindestens einer, mindestens einer
 * versteckt sich). Gesucht hat zuerst, wer bisher am seltensten Gendarm war; Partys landen möglichst auf derselben
 * Seite.
 */
public final class Roles {

    private Roles() {
    }

    /** Zahl der Gendarmen bei {@code players} Leuten und {@code percent} Prozent Anteil (kaufmännisch gerundet). */
    public static int seekerCount(int players, int percent) {
        if (players < 2) {
            return players <= 0 ? 0 : 1;
        }
        int n = (int) Math.round(players * Math.max(1, Math.min(90, percent)) / 100.0);
        return Math.max(1, Math.min(players - 1, n));
    }

    /**
     * Gendarmen aussuchen.
     *
     * @param groups    alle Mitspieler in Gruppen (eine Party = eine Gruppe, sonst einzeln), in Ankunfts-Reihenfolge
     * @param count     so viele Gendarmen
     * @param times     wie oft jemand schon Gendarm war (fehlt = nie)
     * @param preferred diese Leute zuerst (Verwaltung/Test: {@code /versteckerladmin sucher})
     * @param random    Zufall bei Gleichstand
     */
    public static Set<UUID> pickSeekers(List<List<UUID>> groups, int count, Map<UUID, Integer> times,
            Collection<UUID> preferred, Random random) {
        Set<UUID> out = new LinkedHashSet<>();
        List<UUID> all = new ArrayList<>();
        groups.forEach(all::addAll);
        for (UUID u : preferred) {
            if (out.size() < count && all.contains(u)) {
                out.add(u);
            }
        }
        if (out.size() >= count) {
            return out;
        }
        List<List<UUID>> shuffled = new ArrayList<>();
        for (List<UUID> g : groups) {
            List<UUID> rest = new ArrayList<>(g);
            rest.removeAll(out);
            if (!rest.isEmpty()) {
                shuffled.add(rest);
            }
        }
        java.util.Collections.shuffle(shuffled, random);
        shuffled.sort(Comparator.comparingDouble(g -> average(g, times)));
        // Ganze Gruppen, die noch passen …
        List<List<UUID>> leftover = new ArrayList<>();
        for (List<UUID> g : shuffled) {
            if (out.size() + g.size() <= count) {
                out.addAll(g);
            } else {
                leftover.add(g);
            }
        }
        // … dann Einzelne (am seltensten gesucht zuerst).
        List<UUID> singles = new ArrayList<>();
        leftover.forEach(singles::addAll);
        java.util.Collections.shuffle(singles, random);
        singles.sort(Comparator.comparingInt(u -> times.getOrDefault(u, 0)));
        for (UUID u : singles) {
            if (out.size() >= count) {
                break;
            }
            out.add(u);
        }
        return out;
    }

    private static double average(List<UUID> g, Map<UUID, Integer> times) {
        return g.stream().mapToInt(u -> times.getOrDefault(u, 0)).average().orElse(0);
    }
}
