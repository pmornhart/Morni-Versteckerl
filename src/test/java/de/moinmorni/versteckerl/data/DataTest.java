package de.moinmorni.versteckerl.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import de.moinmorni.versteckerl.rules.Catalog;
import de.moinmorni.versteckerl.rules.Rewards;
import de.moinmorni.versteckerl.rules.Settlement;
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

    /** Kaufen ist ein Schritt: prüfen, abbuchen, freischalten – oder gar nichts. Das Guthaben wird nie negativ. */
    @Test
    void walletBuysAtomicallyAndNeverGoesNegative() {
        PlayerData d = new PlayerData(UUID.randomUUID(), "Bert");
        assertEquals(0, d.balance());
        assertEquals(PlayerData.Purchase.TOO_POOR, d.buy("pfiff:kuhglocke", 160));
        assertEquals(0, d.balance(), "nichts abgebucht");
        assertFalse(d.owns("pfiff:kuhglocke"));
        d.adjust(200);
        assertEquals(PlayerData.Purchase.OK, d.buy("pfiff:kuhglocke", 160));
        assertEquals(40, d.balance());
        assertEquals(160, d.spent());
        assertTrue(d.owns("PFIFF:Kuhglocke"), "Groß/Klein egal");
        assertEquals(PlayerData.Purchase.OWNED, d.buy("pfiff:kuhglocke", 160), "nicht doppelt kaufen");
        assertEquals(40, d.balance(), "zweiter Klick bucht nichts ab");
        assertEquals(PlayerData.Purchase.TOO_POOR, d.buy("gestalt:werkbank", 150));
        assertEquals(40, d.balance());
        assertFalse(d.owns("gestalt:werkbank"));
        assertEquals(PlayerData.Purchase.INVALID, d.buy("titel:dorfkind", -5));
        assertEquals(PlayerData.Purchase.INVALID, d.buy(" ", 1));
        assertEquals(PlayerData.Purchase.OK, d.buy("titel:dorfkind", 40), "genau passend geht");
        assertEquals(0, d.balance());
        assertEquals(0, d.adjust(-1000), "Verwaltung kann nicht unter 0");
        assertEquals(25, d.adjust(25));
        assertEquals(0, d.points(), "Guthaben von der Verwaltung zählt nicht zu Punkte insgesamt");
    }

    @Test
    void settlementFillsTheBalanceAndKeepsTheTotal() {
        PlayerData d = new PlayerData(UUID.randomUUID(), "Carl");
        Rewards r = Rewards.DEFAULTS;
        d.addRound(new PlayerData.Round(true, false, 0, true, 2, 40));
        Settlement first = Settlement.of(40, 6, true, d.streak(), d.dailyOpen("2026-11-16"), r);
        d.settle(first, "2026-11-16", 1);
        assertEquals(40 - 6 + 25, d.balance(), "Rest der Runde + Tagesbonus");
        assertEquals(40 + 25, d.points(), "Punkte insgesamt: alles Verdiente, Ausgaben ziehen nichts ab");
        assertEquals(1, d.streak());
        assertFalse(d.dailyOpen("2026-11-16"), "Tagesbonus heute schon geholt");
        assertTrue(d.dailyOpen("2026-11-17"), "morgen wieder");
        d.addRound(new PlayerData.Round(false, true, 2, false, 0, 30));
        d.settle(Settlement.of(30, 0, true, d.streak(), d.dailyOpen("2026-11-16"), r), "2026-11-16", 0);
        assertEquals(2, d.streak());
        assertEquals(59 + 30 + 3, d.balance(), "zweiter Sieg in Folge: +3");
        d.adjust(-50);
        d.buy("gestalt:busch", 10);
        long total = d.points();
        d.addRound(new PlayerData.Round(false, false, 0, false, 0, 4));
        d.settle(Settlement.of(4, 0, false, d.streak(), true, r), "2026-11-16", 0);
        assertEquals(0, d.streak(), "verloren: Serie vorbei");
        assertEquals(2, d.bestStreak(), "beste Serie bleibt");
        assertEquals(total + 4, d.points(), "Punkte insgesamt sinken nie");
        assertEquals(1, d.helps());
    }

    @Test
    void walletSurvivesTheFile() {
        PlayerData d = new PlayerData(UUID.randomUUID(), "Dora");
        d.adjust(500);
        d.buy("gestalt:werkbank", 150);
        d.buy("pfiff:kuhglocke", 160);
        d.equip(Catalog.Kind.WHISTLE, "kuhglocke");
        d.equip(Catalog.Kind.SHAPE, "werkbank");
        d.addRound(new PlayerData.Round(true, false, 1, true, 3, 33));
        d.settle(Settlement.of(33, 0, true, 0, true, Rewards.DEFAULTS), "2026-11-16", 2);
        PlayerData back = PlayerData.fromYaml(d.uuid(), d.toYaml());
        assertEquals(d.toYaml(), back.toYaml());
        assertEquals(d.balance(), back.balance());
        assertEquals("kuhglocke", back.equipped(Catalog.Kind.WHISTLE));
        assertEquals("werkbank", back.equipped(Catalog.Kind.SHAPE));
        assertTrue(back.owns("gestalt:werkbank") && back.owns("pfiff:kuhglocke"));
        assertEquals("2026-11-16", back.dailyDay());
        assertEquals(1, back.streak());
        back.equip(Catalog.Kind.WHISTLE, null);
        assertNull(back.equipped(Catalog.Kind.WHISTLE), "ablegen = wieder Standard");
        // Datei von Version 0.1 (ohne Guthaben): alles 0, nichts kaputt
        PlayerData old = PlayerData.fromYaml(UUID.randomUUID(), "name: Emil\nrunden: 3\npunkte: 50\nbeste-runde: 22\n");
        assertEquals(0, old.balance());
        assertEquals(50, old.points());
        assertTrue(old.owned().isEmpty());
        assertTrue(old.dailyOpen("2026-11-16"));
        // Kaputte Werte werden nicht übernommen
        PlayerData odd = PlayerData.fromYaml(UUID.randomUUID(), "guthaben: -40\nausgeruestet:\n  quatsch: x\n  pfiff: kuckuck\n");
        assertEquals(0, odd.balance());
        assertEquals("kuckuck", odd.equipped(Catalog.Kind.WHISTLE));
        assertEquals(1, odd.equippedAll().size());
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
        b.put(Leaderboard.Board.TOTAL, "Anna", 120, 6);
        b.put(Leaderboard.Board.TOTAL, "Anna", 180, 7);
        assertEquals(180, Leaderboard.fromYaml(b.toYaml()).top(Leaderboard.Board.TOTAL).get(0).value(),
                "Punkte insgesamt wachsen");
    }
}
