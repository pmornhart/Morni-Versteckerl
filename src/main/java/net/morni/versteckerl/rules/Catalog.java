package net.morni.versteckerl.rules;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Alles, was es im Kramerladen der Gendarmerie gibt – ohne Bukkit (Namen kommen aus den Texten, Klang und Optik aus
 * {@code game/Flair}). Sechs Abteilungen: Gestalten, Pfiff-Klänge, Fund-Effekte, Spuren, Sieg-Effekte und Titel. In
 * jeder Abteilung außer den Gestalten gibt es ein freies Standard-Stück, das jeder hat.
 *
 * <p>Preise hier sind die Vorgaben; die {@code config.yml} kann jeden Preis ändern und jedes Stück abschalten
 * ({@code laden.<abteilung>.<id>.preis} bzw. {@code .an}). Kaufen geht nur mit Punkten aus dem Spiel – nie mit echtem
 * Geld.
 */
public final class Catalog {

    /** Die Abteilungen: {@code section} ist der Name in der config.yml, {@code prefix} steht in der Spielerdatei. */
    public enum Kind {
        SHAPE("gestalten", "gestalt"),
        WHISTLE("pfiffe", "pfiff"),
        FIND("fund-effekte", "fund"),
        TRAIL("spuren", "spur"),
        WIN("sieg-effekte", "sieg"),
        TITLE("titel", "titel");

        private final String section;
        private final String prefix;

        Kind(String section, String prefix) {
            this.section = section;
            this.prefix = prefix;
        }

        public String section() {
            return section;
        }

        public String prefix() {
            return prefix;
        }

        public static Kind byPrefix(String prefix) {
            for (Kind k : values()) {
                if (k.prefix.equals(prefix) || k.section.equals(prefix)) {
                    return k;
                }
            }
            return null;
        }
    }

    /** Werte einer Person, an die ein Titel geknüpft sein kann („erst ab 25 Funden“). */
    public enum Stat {
        ROUNDS("runden"),
        FOUND("funde"),
        SURVIVED("unentdeckt"),
        WHISTLES("pfiffe"),
        WINS("siege");

        private final String id;

        Stat(String id) {
            this.id = id;
        }

        public String id() {
            return id;
        }
    }

    /** Bedingung zum Kaufen: mindestens {@code min} von {@code stat}. */
    public record Need(Stat stat, int min) {
    }

    /**
     * Ein Stück im Laden.
     *
     * @param loudness nur Pfiff-Klänge: 1 leise, 2 laut, 3 sehr laut (lautere geben mehr Punkte, hört man aber weiter)
     * @param symbol   nur Titel: Zeichen vor dem Titel
     * @param need     nur manche Titel: Bedingung, sonst null
     */
    public record Item(Kind kind, String id, Tier tier, int price, int loudness, String symbol, Need need) {

        /** Eindeutiger Schlüssel, so steht er in der Spielerdatei: {@code pfiff:kuhglocke}. */
        public String key() {
            return Catalog.key(kind, id);
        }

        public boolean free() {
            return tier == Tier.FREE;
        }
    }

    /** Vorgabe-Preis je Seltenheit (Gestalten, Effekte …). */
    public static final int COMMON_PRICE = 150;
    public static final int RARE_PRICE = 300;
    public static final int EPIC_PRICE = 600;
    public static final int QUIET = 1;
    public static final int LOUD = 2;
    public static final int VERY_LOUD = 3;

    private static final Map<String, Item> ITEMS = new LinkedHashMap<>();

    static {
        for (Shape s : Shape.values()) {
            if (!s.free()) {
                add(new Item(Kind.SHAPE, s.id(), s.tier(), price(s.tier()), 0, "", null));
            }
        }
        whistle("floete", Tier.FREE, 0, QUIET);
        whistle("kuckuck", Tier.COMMON, 80, QUIET);
        whistle("henne", Tier.COMMON, 80, QUIET);
        whistle("murmeltier", Tier.RARE, 160, LOUD);
        whistle("kuhglocke", Tier.RARE, 160, LOUD);
        whistle("jodler", Tier.EPIC, 280, VERY_LOUD);
        whistle("trompete", Tier.EPIC, 280, VERY_LOUD);
        whistle("alphorn", Tier.EPIC, 280, VERY_LOUD);
        effect(Kind.FIND, "sternchen", Tier.FREE, 0);
        effect(Kind.FIND, "herzerl", Tier.COMMON, 100);
        effect(Kind.FIND, "rauchwolke", Tier.COMMON, 100);
        effect(Kind.FIND, "konfetti", Tier.RARE, 200);
        effect(Kind.FIND, "blueten", Tier.RARE, 200);
        effect(Kind.FIND, "weissblau", Tier.EPIC, 350);
        effect(Kind.FIND, "goldregen", Tier.EPIC, 350);
        effect(Kind.TRAIL, "keine", Tier.FREE, 0);
        effect(Kind.TRAIL, "noten", Tier.COMMON, 100);
        effect(Kind.TRAIL, "herzerl", Tier.COMMON, 100);
        effect(Kind.TRAIL, "schneeflocken", Tier.RARE, 200);
        effect(Kind.TRAIL, "blueten", Tier.RARE, 200);
        effect(Kind.TRAIL, "funken", Tier.EPIC, 350);
        effect(Kind.TRAIL, "gluehwuermchen", Tier.EPIC, 350);
        effect(Kind.WIN, "keiner", Tier.FREE, 0);
        effect(Kind.WIN, "glocken", Tier.COMMON, 120);
        effect(Kind.WIN, "konfetti", Tier.RARE, 220);
        effect(Kind.WIN, "goldregen", Tier.RARE, 220);
        effect(Kind.WIN, "feuerwerk", Tier.EPIC, 400);
        effect(Kind.WIN, "weissblau", Tier.EPIC, 400);
        title("keiner", Tier.FREE, 0, "", null);
        title("dorfkind", Tier.COMMON, 60, "☘", null);
        title("heustadl-held", Tier.COMMON, 120, "✿", null);
        title("pfeifkoenig", Tier.RARE, 200, "♪", new Need(Stat.WHISTLES, 50));
        title("spuernase", Tier.RARE, 250, "✪", new Need(Stat.FOUND, 25));
        title("meister-der-tarnung", Tier.RARE, 300, "❀", new Need(Stat.SURVIVED, 15));
        title("oberwachtmeister", Tier.EPIC, 500, "⚑", new Need(Stat.FOUND, 75));
        title("dorflegende", Tier.EPIC, 800, "★", new Need(Stat.ROUNDS, 100));
    }

    private Catalog() {
    }

    private static int price(Tier t) {
        return switch (t) {
            case FREE -> 0;
            case COMMON -> COMMON_PRICE;
            case RARE -> RARE_PRICE;
            case EPIC -> EPIC_PRICE;
        };
    }

    private static void add(Item item) {
        if (ITEMS.put(item.key(), item) != null) {
            throw new IllegalStateException("Doppelt im Laden: " + item.key());
        }
    }

    private static void whistle(String id, Tier tier, int price, int loudness) {
        add(new Item(Kind.WHISTLE, id, tier, price, loudness, "", null));
    }

    private static void effect(Kind kind, String id, Tier tier, int price) {
        add(new Item(kind, id, tier, price, 0, "", null));
    }

    private static void title(String id, Tier tier, int price, String symbol, Need need) {
        add(new Item(Kind.TITLE, id, tier, price, 0, symbol, need));
    }

    public static String key(Kind kind, String id) {
        return kind.prefix() + ":" + id.toLowerCase(Locale.ROOT);
    }

    /** Alle Stücke in Ladenreihenfolge. */
    public static List<Item> all() {
        return Collections.unmodifiableList(new ArrayList<>(ITEMS.values()));
    }

    /** Alle Stücke einer Abteilung (das freie Standard-Stück zuerst). */
    public static List<Item> of(Kind kind) {
        return ITEMS.values().stream().filter(i -> i.kind() == kind).toList();
    }

    /** Stück nach Schlüssel ({@code pfiff:kuhglocke}) oder null. */
    public static Item get(String key) {
        return key == null ? null : ITEMS.get(key.toLowerCase(Locale.ROOT));
    }

    public static Item get(Kind kind, String id) {
        return id == null ? null : ITEMS.get(key(kind, id));
    }

    /** Das freie Standard-Stück einer Abteilung (Gestalten: keins). */
    public static Item standard(Kind kind) {
        return ITEMS.values().stream().filter(i -> i.kind() == kind && i.free()).findFirst().orElse(null);
    }

    /** Gestalt zu einem Laden-Stück der Abteilung Gestalten. */
    public static Shape shape(Item item) {
        return item.kind() == Kind.SHAPE ? Shape.byId(item.id()) : null;
    }
}
