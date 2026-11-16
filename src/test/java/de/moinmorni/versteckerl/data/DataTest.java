package de.moinmorni.versteckerl.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class DataTest {

    @Test
    void roundsAddUp() {
        PlayerData d = new PlayerData(UUID.randomUUID(), "Anna");
        assertTrue(d.addRound(new PlayerData.Round(true, false, 0, true, 3, 41)));
        assertFalse(d.addRound(new PlayerData.Round(false, true, 2, false, 0, 27)), "keine neue Bestleistung");
        d.becameSeeker();
        assertEquals(2, d.games());
        assertEquals(1, d.winsHider());
        assertEquals(1, d.winsSeeker());
        assertEquals(2, d.found());
        assertEquals(1, d.survived());
        assertEquals(3, d.whistles());
        assertEquals(68, d.points());
        assertEquals(41, d.bestPoints());
        PlayerData back = PlayerData.fromYaml(d.uuid(), d.toYaml());
        assertEquals(d.toYaml(), back.toYaml());
        assertEquals(1, back.seekerRounds());
    }

    @Test
    void leaderboardKeepsTheBestPerName() {
        Leaderboard b = new Leaderboard();
        assertEquals(1, b.put(Leaderboard.Board.POINTS, "Anna", 30, 1));
        assertEquals(2, b.put(Leaderboard.Board.POINTS, "Bert", 20, 2));
        assertEquals(1, b.put(Leaderboard.Board.POINTS, "Bert", 50, 3), "besser → vorrücken, alter Eintrag weg");
        assertEquals(2, b.put(Leaderboard.Board.POINTS, "anna", 10, 4), "schlechter → bleibt beim alten Wert");
        assertEquals(2, b.top(Leaderboard.Board.POINTS).size());
        assertEquals("Bert", b.top(Leaderboard.Board.POINTS).get(0).name());
        assertEquals(0, b.put(Leaderboard.Board.FOUND, "Carl", 0, 5), "0 zählt nicht");
        for (int i = 0; i < 12; i++) {
            b.put(Leaderboard.Board.FOUND, "P" + i, 100 - i, i);
        }
        assertEquals(Leaderboard.SIZE, b.top(Leaderboard.Board.FOUND).size());
        assertEquals(0, b.put(Leaderboard.Board.FOUND, "Langsam", 1, 99));
        Leaderboard back = Leaderboard.fromYaml(b.toYaml());
        assertEquals(b.top(Leaderboard.Board.POINTS), back.top(Leaderboard.Board.POINTS));
        assertEquals(b.top(Leaderboard.Board.FOUND), back.top(Leaderboard.Board.FOUND));
        assertTrue(back.top(Leaderboard.Board.SURVIVED).isEmpty());
    }
}
