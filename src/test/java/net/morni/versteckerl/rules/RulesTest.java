package net.morni.versteckerl.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RulesTest {

    private static UUID u(int i) {
        return new UUID(0, i);
    }

    @Test
    void aboutAQuarterSeeks() {
        assertEquals(1, Roles.seekerCount(2, 25));
        assertEquals(1, Roles.seekerCount(3, 25));
        assertEquals(1, Roles.seekerCount(4, 25));
        assertEquals(2, Roles.seekerCount(6, 25));
        assertEquals(2, Roles.seekerCount(8, 25));
        assertEquals(3, Roles.seekerCount(12, 25));
        assertEquals(4, Roles.seekerCount(16, 25));
        assertEquals(1, Roles.seekerCount(1, 25), "allein: einer");
        assertEquals(2, Roles.seekerCount(3, 90), "mindestens einer versteckt sich");
    }

    @Test
    void whoSeekedLeastSeeksNext() {
        List<List<UUID>> groups = List.of(List.of(u(1)), List.of(u(2)), List.of(u(3)), List.of(u(4)));
        Map<UUID, Integer> times = new HashMap<>(Map.of(u(1), 3, u(2), 0, u(3), 2, u(4), 1));
        for (int seed = 0; seed < 20; seed++) {
            assertEquals(Set.of(u(2)), Roles.pickSeekers(groups, 1, times, List.of(), new Random(seed)));
        }
        assertEquals(Set.of(u(2), u(4)), Roles.pickSeekers(groups, 2, times, List.of(), new Random(1)));
    }

    @Test
    void preferredFirstAndPartiesStayTogether() {
        List<List<UUID>> groups = List.of(List.of(u(1), u(2)), List.of(u(3)), List.of(u(4)), List.of(u(5)),
                List.of(u(6)), List.of(u(7)), List.of(u(8)));
        assertEquals(Set.of(u(5)), Roles.pickSeekers(groups, 1, Map.of(), List.of(u(5)), new Random(3)));
        assertEquals(Set.of(u(5), u(9)).size() - 1, Roles.pickSeekers(groups, 1, Map.of(), List.of(u(9), u(5)),
                new Random(3)).size(), "Unbekannte werden übersprungen");
        // Zwei Gendarmen: die Party (1, 2) kommt ganz dran, wenn sie am seltensten gesucht hat
        Map<UUID, Integer> times = new HashMap<>();
        for (int i = 3; i <= 8; i++) {
            times.put(u(i), 1);
        }
        assertEquals(Set.of(u(1), u(2)), Roles.pickSeekers(groups, 2, times, List.of(), new Random(7)));
        // Bei einem Platz wird die Party nicht zerrissen, ein Einzelner sucht
        Set<UUID> one = Roles.pickSeekers(groups, 1, Map.of(), List.of(), new Random(11));
        assertEquals(1, one.size());
        assertFalse(one.contains(u(1)) || one.contains(u(2)));
    }

    @Test
    void lateWhistlesAreWorthMore() {
        assertEquals(1, Points.whistle(0));
        assertEquals(1, Points.whistle(29));
        assertEquals(2, Points.whistle(30));
        assertEquals(5, Points.whistle(125));
        assertEquals(10, Points.whistle(900));
        assertEquals(0, Points.whistle(-1));
        assertTrue(Points.halfMinute(30));
        assertFalse(Points.halfMinute(0));
        assertFalse(Points.halfMinute(31));
    }

    @Test
    void whoWins() {
        assertSame(Outcome.Winner.NONE, Outcome.decide(2, 1, false));
        assertSame(Outcome.Winner.HIDERS, Outcome.decide(1, 3, true), "Zeit um, noch jemand versteckt");
        assertSame(Outcome.Winner.SEEKERS, Outcome.decide(0, 3, false), "alle gefunden");
        assertSame(Outcome.Winner.SEEKERS, Outcome.decide(0, 3, true), "alle gefunden, genau zum Schluss");
        assertSame(Outcome.Winner.HIDERS, Outcome.decide(2, 0, false), "alle Gendarmen gegangen");
        assertSame(Outcome.Winner.NONE, Outcome.decide(0, 0, false), "niemand mehr da");
    }

    @Test
    void whistleDuty() {
        long seek = 300 * 20;
        long from = Whistle.mandatoryFrom(seek, 50);
        assertEquals(150 * 20, from);
        long interval = 30 * 20;
        assertEquals(-1, Whistle.dueIn(from - 1, from, -1, interval), "vorher keine Pflicht");
        assertEquals(interval, Whistle.dueIn(from, from, -1, interval), "Pflicht beginnt: volle Frist");
        assertEquals(0, Whistle.dueIn(from + interval, from, -1, interval), "nicht gepfiffen → fällig");
        assertEquals(interval - 20, Whistle.dueIn(from + 20 * 20, from, from + 19 * 20, interval),
                "gepfiffen → Frist ab dem Pfiff");
        assertEquals(0, Whistle.mandatoryFrom(seek, 0));
        assertEquals(seek, Whistle.mandatoryFrom(seek, 200));
    }

    @Test
    void hallCountdown() {
        assertFalse(HallRules.countdown(1, 1, 4), "allein geht's nicht");
        assertFalse(HallRules.countdown(3, 2, 4), "zu wenige, nicht alle bereit");
        assertTrue(HallRules.countdown(3, 3, 4), "zu wenige, aber alle bereit");
        assertTrue(HallRules.countdown(4, 0, 4), "genug Leute");
        assertTrue(HallRules.allReady(2, 2));
        assertFalse(HallRules.allReady(1, 1));
    }

    @Test
    void rayHitsBoxes() {
        // Blick nach Süden (+z) auf einen Block 2 vor dem Auge
        double t = Ray.box(0.5, 1.6, 0.5, 0, 0, 1, 0, 1, 2, 1, 2, 3, 4.5);
        assertEquals(1.5, t, 1e-9);
        assertEquals(Ray.MISS, Ray.box(0.5, 1.6, 0.5, 0, 0, 1, 0, 1, 6, 1, 2, 7, 4.5), "zu weit");
        assertEquals(Ray.MISS, Ray.box(0.5, 1.6, 0.5, 0, 0, -1, 0, 1, 2, 1, 2, 3, 4.5), "falsche Richtung");
        assertEquals(0, Ray.box(0.5, 1.5, 0.5, 1, 0, 0, 0, 1, 0, 1, 2, 1, 4.5), 1e-9, "Auge im Kasten");
        // Blick schräg nach unten auf eine Gestalt, die vor einem steht
        double[] d = Ray.direction(0f, 45f);
        assertEquals(0, d[0], 1e-9);
        assertEquals(-Math.sqrt(0.5), d[1], 1e-9);
        assertEquals(Math.sqrt(0.5), d[2], 1e-9);
        double hit = Ray.shape(0.5, 101.62, 0.5, d[0], d[1], d[2], 0.5, 100, 2.0, 0.1, 4.5);
        assertTrue(hit < 4.5, "Gestalt getroffen");
        assertEquals(Ray.MISS, Ray.shape(0.5, 101.62, 0.5, d[0], d[1], d[2], 3.5, 100, 2.0, 0.1, 4.5), "daneben");
        double[] west = Ray.direction(90f, 0f);
        assertEquals(-1, west[0], 1e-9);
    }

    @Test
    void shapesKnowTheirKind() {
        assertTrue(Shape.BLUMENTOPF.sameKind("potted_poppy"));
        assertTrue(Shape.BLUMENTOPF.sameKind("minecraft:flower_pot"));
        assertFalse(Shape.BLUMENTOPF.sameKind("poppy"));
        assertTrue(Shape.FASS.sameKind("barrel[facing=east]"));
        assertTrue(Shape.BIERFASS.sameKind("barrel"));
        assertTrue(Shape.KESSEL.sameKind("water_cauldron"));
        assertFalse(Shape.HACKSTOCK.sameKind("spruce_log"), "Holzwände zählen nicht als Hackstock");
        assertSame(Shape.KOMPOSTER, Shape.byId("komposter"));
        assertNull(Shape.byId("drache"));
        for (Shape s : Shape.values()) {
            assertTrue(s.sameKind(s.block()), s.id() + " ist seine eigene Art");
        }
    }

    @Test
    void groupsAndVotes() {
        List<List<UUID>> groups = new ArrayList<>(List.of(List.of(u(1), u(2), u(3)), List.of(u(4)), List.of(u(5), u(6))));
        assertEquals(List.of(u(1), u(2), u(3), u(4)), Groups.pick(groups, 4));
        assertEquals(List.of(u(1), u(2), u(3), u(4), u(5), u(6)), Groups.pick(groups, 16));
        enum M { A, B, C }
        Votes<M> v = new Votes<>(M.class);
        assertSame(M.A, v.winner(Set.of(M.A, M.B, M.C)), "keine Stimmen: erste");
        v.played(M.A, 100);
        assertSame(M.B, v.winner(Set.of(M.A, M.B, M.C)), "am längsten nicht gespielt");
        v.vote(u(1), M.C);
        v.vote(u(2), M.C);
        v.vote(u(3), M.A);
        assertSame(M.C, v.winner(Set.of(M.A, M.B, M.C)));
        assertSame(M.A, v.winner(Set.of(M.A, M.B)), "belegte Karte fällt raus");
        v.retain(List.of(u(3)));
        assertEquals(0, v.count(M.C));
    }
}
