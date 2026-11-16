package net.morni.versteckerl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import net.morni.versteckerl.rules.Catalog;
import net.morni.versteckerl.rules.Helps;
import net.morni.versteckerl.rules.Rewards;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

/** Die mitgelieferte config.yml: jede Zahl steht drin („Alles im Panel“) und entspricht den Vorgaben im Code. */
class SettingsTest {

    private static YamlConfiguration bundled() {
        return YamlConfiguration.loadConfiguration(new File("src/main/resources/config.yml"));
    }

    @Test
    void bundledConfigMatchesTheDefaults() {
        YamlConfiguration c = bundled();
        Settings s = Settings.load(c);
        assertEquals(Rewards.DEFAULTS, s.rewards(), "punkte.* in der config.yml = Vorgaben im Code");
        assertEquals(Settings.defaults().rewards(), s.rewards());
        assertTrue(s.shop());
        assertTrue(s.roundShop());
        for (var e : Helps.defaults().entrySet()) {
            Helps.Rule d = e.getValue();
            Helps.Rule got = s.help(e.getKey());
            String p = "rundenladen." + e.getKey().id();
            assertTrue(c.contains(p + ".preis"), p + " fehlt in der config.yml");
            assertEquals(d.on(), got.on(), p);
            assertEquals(d.price(), got.price(), p);
            assertEquals(d.cooldown(), got.cooldown(), p);
            assertEquals(d.perRound(), got.perRound(), p);
        }
        for (Catalog.Item i : Catalog.all()) {
            if (i.free()) {
                continue;
            }
            assertTrue(c.contains(Settings.path(i) + ".preis"), Settings.path(i) + " fehlt in der config.yml");
            assertTrue(c.contains(Settings.path(i) + ".an"), Settings.path(i) + ".an fehlt in der config.yml");
            assertEquals(i.price(), s.price(i), Settings.path(i));
            assertTrue(s.offered(i), Settings.path(i));
        }
    }

    @Test
    void pricesAndSwitchesComeFromTheConfig() {
        YamlConfiguration c = bundled();
        c.set("laden.pfiffe.kuhglocke.preis", 5);
        c.set("laden.titel.dorfkind.an", false);
        c.set("punkte.fund", 12);
        c.set("punkte.treffer", -3);
        c.set("rundenladen.laterne.preis", 2);
        c.set("rundenladen.lockpfiff.an", false);
        Settings s = Settings.load(c);
        assertEquals(5, s.price(Catalog.get("pfiff:kuhglocke")));
        assertFalse(s.offered(Catalog.get("titel:dorfkind")));
        assertTrue(s.offered(Catalog.get("titel:keiner")), "das freie Standard-Stück gibt es immer");
        assertEquals(0, s.price(Catalog.get("titel:keiner")));
        assertEquals(12, s.rewards().find());
        assertEquals(0, s.rewards().hit(), "nie negativ");
        assertEquals(2, s.help(Helps.Help.LATERNE).price());
        assertFalse(s.help(Helps.Help.LOCKPFIFF).on());
        c.set("laden.an", false);
        assertFalse(Settings.load(c).offered(Catalog.get("pfiff:kuhglocke")), "Laden zu: nichts zu haben");
    }
}
