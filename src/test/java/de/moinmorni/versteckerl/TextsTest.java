package de.moinmorni.versteckerl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.moinmorni.versteckerl.data.Leaderboard;
import de.moinmorni.versteckerl.rules.Shape;
import de.moinmorni.versteckerl.world.MapType;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.junit.jupiter.api.Test;

class TextsTest {

    /** Formatierungen, die keine Platzhalter sind. */
    private static final Set<String> STYLES = Set.of("gray", "white", "em", "au", "bl", "red", "green", "dark_gray",
            "b", "bold", "gold", "yellow");
    // Tags wie <name> oder </gray> – aber nicht \<spieler> (maskiert).
    // https://regex101.com/?flavor=java&regex=(%3F%3C!%5C%5C)%3C%2F%3F(%5Ba-z_%5D%2B)%3E&testString=%3Cgray%3EHi%20%3Cname%3E%3C%2Fgray%3E%20%5C%3Cspieler%3E
    private static final Pattern TAG = Pattern.compile("(?<!\\\\)</?([a-z_]+)>");
    /** Schlüssel in den Quelltexten: "hall.ready", "whistle.done" … */
    // https://regex101.com/?flavor=java&regex=%22(%5Ba-z%5D%5Ba-z0-9-%5D*(%3F%3A%5C.%5Ba-z0-9-%5D%2B)%2B)%22&testString=Texts.msg(lang%2C%20%22hall.ready%22)
    private static final Pattern KEY = Pattern.compile("\"([a-z][a-z0-9-]*(?:\\.[a-z0-9-]+)+)\"");

    private static Set<String> placeholders(String text) {
        Set<String> out = new TreeSet<>();
        Matcher m = TAG.matcher(text);
        while (m.find()) {
            if (!STYLES.contains(m.group(1))) {
                out.add(m.group(1));
            }
        }
        return out;
    }

    @Test
    void everyTextExistsInBothLanguagesWithTheSamePlaceholders() {
        for (String key : Texts.keys()) {
            String de = Texts.raw("de", key);
            String en = Texts.raw("en", key);
            assertFalse(de.isBlank(), key);
            assertFalse(en.isBlank(), key);
            assertEquals(placeholders(de), placeholders(en), "Platzhalter in " + key);
        }
    }

    @Test
    void everyTextParsesStrictly() {
        MiniMessage strict = MiniMessage.builder().strict(true)
                .editTags(t -> t.resolvers(
                        TagResolver.resolver("em", Tag.styling(TextColor.color(Texts.EMERALD))),
                        TagResolver.resolver("au", Tag.styling(TextColor.color(Texts.GOLD))),
                        TagResolver.resolver("bl", Tag.styling(TextColor.color(Texts.BLUE)))))
                .build();
        for (String key : Texts.keys()) {
            for (String lang : new String[] {"de", "en"}) {
                String text = Texts.raw(lang, key);
                TagResolver.Builder resolvers = TagResolver.builder();
                placeholders(text).forEach(p -> resolvers.resolver(Placeholder.unparsed(p, "x")));
                TagResolver all = resolvers.build();
                for (String line : text.split("\n")) {
                    assertDoesNotThrow(() -> strict.deserialize(line, all), lang + " " + key + ": " + line);
                }
            }
        }
    }

    /** Minecraft zeigt nur Zeichen der „Basic Multilingual Plane“ – Emoji wie 🏆 wären ein leeres Kästchen. */
    @Test
    void onlyCharactersMinecraftCanShow() {
        for (String key : Texts.keys()) {
            for (String lang : new String[] {"de", "en"}) {
                String text = Texts.raw(lang, key);
                assertEquals(text.length(), text.codePointCount(0, text.length()), lang + " " + key);
            }
        }
    }

    @Test
    void englishIsReallyEnglish() {
        assertNotEquals(Texts.raw("de", "over.hiders"), Texts.raw("en", "over.hiders"));
        assertEquals(Texts.raw("de", "map.wirtshaus"), Texts.raw("fr", "map.wirtshaus"), "unbekannte Sprache → Deutsch");
        assertEquals("missing.key", Texts.raw("de", "missing.key"));
        assertEquals("Bierfass", Texts.plain("de", "shape.bierfass"));
        assertEquals("Beer keg", Texts.plain("en", "shape.bierfass"));
    }

    /** Jede Gestalt, jede Karte und jede Bestenliste hat Namen und Beschreibungen in beiden Sprachen. */
    @Test
    void everyGameThingHasAName() {
        for (Shape s : Shape.values()) {
            assertTrue(Texts.keys().contains("shape." + s.id()), s.id());
            assertTrue(Texts.keys().contains("shape." + s.id() + ".hint"), s.id());
        }
        for (MapType m : MapType.values()) {
            assertTrue(Texts.keys().contains("map." + m.id()), m.id());
            assertTrue(Texts.keys().contains("map." + m.id() + ".desc"), m.id());
            assertTrue(Texts.keys().contains("board.stand.desc." + m.id()), m.id());
        }
        for (Leaderboard.Board b : Leaderboard.Board.values()) {
            assertTrue(Texts.keys().contains("board." + b.id()), b.id());
        }
    }

    /** Jeder Schlüssel, der im Quelltext steht, hat einen Text (Tippfehler fallen sofort auf). */
    @Test
    void everyKeyInTheSourceExists() throws IOException {
        Set<String> missing = new TreeSet<>();
        try (Stream<Path> files = Files.walk(Path.of("src/main/java"))) {
            for (Path f : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                Matcher m = KEY.matcher(Files.readString(f));
                while (m.find()) {
                    String k = m.group(1);
                    boolean code = k.startsWith("minecraft.") || k.startsWith("moinmorni.") || k.startsWith("de.")
                            || k.endsWith(".yml") || k.endsWith(".txt") || k.endsWith(".json") || k.endsWith(".log")
                            || k.endsWith(".net") || k.startsWith("block.") || k.startsWith("entity.")
                            || k.startsWith("item.book") || k.startsWith("ui.") || k.startsWith("gendarmerie.")
                            || k.startsWith("runde.") || k.startsWith("hilfen.") || k.endsWith(".");
                    if (!code && !Texts.keys().contains(k)) {
                        missing.add(k + " (" + f.getFileName() + ")");
                    }
                }
            }
        }
        assertTrue(missing.isEmpty(), "Texte fehlen: " + missing);
    }

    @Test
    void ownColourTagsWinOverStandardTags() {
        var c = Texts.MM.deserialize("<em>x</em>");
        assertEquals(TextColor.color(Texts.EMERALD), c.children().isEmpty() ? c.color() : c.children().get(0).color());
    }

    @Test
    void numbersAndBars() {
        assertEquals("▮▮▮▯▯", Ui.bar(0.6, 5));
        assertEquals("▯▯▯", Ui.bar(-1, 3));
        assertEquals("1:05", Ui.clock(65));
        assertEquals("0:00", Ui.clock(-3));
        assertEquals("5:00", Ui.clock(300));
    }
}
