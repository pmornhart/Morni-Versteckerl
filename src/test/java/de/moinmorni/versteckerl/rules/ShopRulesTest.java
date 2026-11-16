package de.moinmorni.versteckerl.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import de.moinmorni.versteckerl.world.MapType;
import org.junit.jupiter.api.Test;

/** Punkte, Abrechnung, Rundenladen und Katalog – ohne Server. */
class ShopRulesTest {

    private static final Rewards R = Rewards.DEFAULTS;

    @Test
    void whistlesPayMoreWhenLaterLouderAndBraver() {
        assertEquals(1, R.whistleBase(0));
        assertEquals(2, R.whistleBase(30));
        assertEquals(10, R.whistleBase(9999), "höchstens pfiff-hoechstens");
        assertEquals(0, R.whistle(-1, Catalog.VERY_LOUD, 1), "vor der Suche nichts");
        assertEquals(0, R.loudness(Catalog.QUIET));
        assertEquals(2, R.loudness(Catalog.LOUD));
        assertEquals(4, R.loudness(Catalog.VERY_LOUD));
        assertEquals(4, R.courage(3), "Gendarm ganz nah");
        assertEquals(4, R.courage(6));
        assertEquals(2, R.courage(6.5), "Gendarm in der Nähe");
        assertEquals(2, R.courage(12));
        assertEquals(0, R.courage(12.1));
        assertEquals(0, R.courage(Double.POSITIVE_INFINITY), "kein Gendarm da");
        assertEquals(1, R.whistle(0, Catalog.QUIET, 40), "Flöte, früh, niemand nah");
        assertEquals(3, R.whistle(0, Catalog.LOUD, 40), "Kuhglocke: +2");
        assertEquals(3 + 4 + 4, R.whistle(65, Catalog.VERY_LOUD, 5), "spät, sehr laut, mutig");
    }

    @Test
    void streakBonusGrowsButHasALimit() {
        assertEquals(0, R.streakBonus(0));
        assertEquals(0, R.streakBonus(1), "ein Sieg ist noch keine Serie");
        assertEquals(3, R.streakBonus(2));
        assertEquals(6, R.streakBonus(3));
        assertEquals(15, R.streakBonus(6));
        assertEquals(15, R.streakBonus(40), "höchstens serie-hoechstens weitere Siege");
    }

    @Test
    void settlementPutsTheRestOnTheBalance() {
        Settlement win = Settlement.of(37, 8, true, 2, true, R);
        assertEquals(3, win.streak());
        assertEquals(6, win.streakBonus());
        assertEquals(25, win.daily());
        assertEquals(37 - 8 + 6 + 25, win.credit());
        assertEquals(31, win.bonus());
        Settlement again = Settlement.of(20, 0, true, 3, false, R);
        assertEquals(0, again.daily(), "Tagesbonus nur einmal am Tag");
        assertEquals(20 + 9, again.credit());
        Settlement loss = Settlement.of(12, 4, false, 5, true, R);
        assertEquals(0, loss.streak(), "Niederlage beendet die Serie");
        assertEquals(0, loss.bonus(), "ohne Sieg kein Bonus");
        assertEquals(8, loss.credit());
        Settlement odd = Settlement.of(5, 9, false, 0, true, R);
        assertEquals(5, odd.spent(), "ausgegeben höchstens, was da war");
        assertEquals(0, odd.credit(), "nie negativ");
        assertEquals(0, Settlement.of(-3, 0, false, 0, false, R).credit());
    }

    @Test
    void helpsAreLimitedAndInTheRightOrder() {
        Helps.Rule lock = Helps.defaults().get(Helps.Help.LOCKPFIFF);
        Helps.Help h = Helps.Help.LOCKPFIFF;
        assertSame(Helps.Verdict.OK, Helps.check(h, lock, true, true, true, 6, 0, 0));
        assertSame(Helps.Verdict.TOO_POOR, Helps.check(h, lock, true, true, true, 5, 0, 0));
        assertSame(Helps.Verdict.COOLDOWN, Helps.check(h, lock, true, true, true, 50, 1, 20));
        assertSame(Helps.Verdict.USED_UP, Helps.check(h, lock, true, true, true, 50, 2, 0), "höchstens 2 je Runde");
        assertSame(Helps.Verdict.NOT_YET, Helps.check(h, lock, true, false, true, 50, 0, 0), "erst in der Suche");
        assertSame(Helps.Verdict.WRONG_ROLE, Helps.check(h, lock, false, true, true, 50, 0, 0), "nicht für Gendarmen");
        assertSame(Helps.Verdict.OFF, Helps.check(h, new Helps.Rule(false, 1, 0, 9, 1, 1), true, true, true, 50, 0, 0));
        assertSame(Helps.Verdict.OFF, Helps.check(h, null, true, true, true, 50, 0, 0));
        Helps.Rule change = Helps.defaults().get(Helps.Help.GESTALTWECHSEL);
        assertSame(Helps.Verdict.NOT_NEEDED, Helps.check(Helps.Help.GESTALTWECHSEL, change, true, true, false, 50, 0, 0),
                "der eine Wechsel ist noch frei");
        Helps.Rule lantern = Helps.defaults().get(Helps.Help.LATERNE);
        assertSame(Helps.Verdict.OK, Helps.check(Helps.Help.LATERNE, lantern, false, true, true, 8, 0, 0));
        // Jede Hilfe hat ein Gegenmittel auf der anderen Seite und kostet etwas.
        boolean hiders = false;
        boolean seekers = false;
        for (Helps.Help x : Helps.Help.values()) {
            Helps.Rule r = Helps.defaults().get(x);
            assertNotNull(r, x.id());
            assertTrue(r.price() > 0 && r.perRound() > 0, x.id());
            hiders |= x.forHiders();
            seekers |= !x.forHiders();
            assertSame(x, Helps.Help.byId(x.id()));
        }
        assertTrue(hiders && seekers);
    }

    @Test
    void catalogIsComplete() {
        Set<String> keys = new HashSet<>();
        for (Catalog.Item i : Catalog.all()) {
            assertTrue(keys.add(i.key()), "doppelt: " + i.key());
            assertSame(i, Catalog.get(i.key()));
            if (i.free()) {
                assertEquals(0, i.price(), i.key());
            } else {
                assertTrue(i.price() > 0, i.key() + " braucht einen Preis");
            }
            if (i.kind() == Catalog.Kind.WHISTLE) {
                assertTrue(i.loudness() >= Catalog.QUIET && i.loudness() <= Catalog.VERY_LOUD, i.key());
            }
            if (i.kind() == Catalog.Kind.TITLE && !i.free()) {
                assertFalse(i.symbol().isBlank(), i.key() + " braucht ein Zeichen");
            }
        }
        for (Catalog.Kind k : Catalog.Kind.values()) {
            long free = Catalog.of(k).stream().filter(Catalog.Item::free).count();
            assertEquals(k == Catalog.Kind.SHAPE ? 0 : 1, free, k + ": genau ein freies Standard-Stück");
            assertTrue(Catalog.of(k).size() >= 5, k + ": zu wenig Auswahl");
        }
        assertNull(Catalog.standard(Catalog.Kind.SHAPE));
        assertEquals("floete", Catalog.standard(Catalog.Kind.WHISTLE).id());
        assertEquals(Catalog.Kind.WHISTLE, Catalog.Kind.byPrefix("pfiff"));
        assertEquals(Catalog.Kind.FIND, Catalog.Kind.byPrefix("fund-effekte"));
        assertNull(Catalog.get("pfiff:gibtsnicht"));
        // Seltenere Stücke kosten mehr (je Abteilung)
        for (Catalog.Kind k : Catalog.Kind.values()) {
            int maxCommon = Catalog.of(k).stream().filter(i -> i.tier() == Tier.COMMON).mapToInt(Catalog.Item::price)
                    .max().orElse(0);
            int minEpic = Catalog.of(k).stream().filter(i -> i.tier() == Tier.EPIC).mapToInt(Catalog.Item::price)
                    .min().orElse(Integer.MAX_VALUE);
            assertTrue(maxCommon < minEpic, k + ": Prächtiges muss mehr kosten als Gewöhnliches");
        }
    }

    @Test
    void shopShapesAreInTheCatalogAndOnMaps() {
        for (Shape s : Shape.values()) {
            Catalog.Item i = Catalog.get(Catalog.Kind.SHAPE, s.id());
            if (s.free()) {
                assertNull(i, s.id() + " ist frei und gehört nicht in den Laden");
            } else {
                assertNotNull(i, s.id());
                assertSame(s, Catalog.shape(i));
                assertSame(s.tier(), i.tier());
            }
        }
        for (MapType m : MapType.values()) {
            assertTrue(m.shapes().size() <= 21, m + ": passt nicht ins Gestalt-Menü");
            assertTrue(m.freeShapes().size() >= 8, m + ": zu wenig freie Gestalten");
            assertFalse(m.shopShapes().isEmpty(), m + ": keine Gestalt aus dem Laden");
        }
        assertEquals(9, MapType.DORFPLATZ.freeShapes().size(), "freie Gestalten bleiben wie bisher");
        assertEquals(8, MapType.WIRTSHAUS.freeShapes().size());
        assertEquals(9, MapType.HEUSTADEL.freeShapes().size());
        // Ein echter Festtagskuchen ist für einen falschen Treffer so gut wie ein Kuchen (und umgekehrt).
        assertTrue(Shape.KUCHEN.sameKind("candle_cake[lit=true]") && Shape.FESTTAGSKUCHEN.sameKind("cake[bites=0]"));
        assertTrue(Shape.KUERBISLATERNE.sameKind("carved_pumpkin") && Shape.KUERBIS.sameKind("jack_o_lantern"));
        assertTrue(Shape.BUSCH.sameKind("azalea") && !Shape.BUSCH.sameKind("azalea_leaves"));
    }
}
