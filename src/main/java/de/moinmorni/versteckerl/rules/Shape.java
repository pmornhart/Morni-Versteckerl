package de.moinmorni.versteckerl.rules;

import java.util.List;
import java.util.Locale;

/**
 * Die Gestalten, in die sich Versteckte verwandeln können – nur Gegenstände, die es auf den Karten auch wirklich als
 * Attrappen gibt. Ohne Bukkit (Blockzustände als Text), damit Baupläne und Regeln ohne Server prüfbar sind.
 *
 * <p>{@code kinds}: Blocknamen, die als „dieselbe Art“ zählen (für den falschen Treffer: Wer ein echtes Fass schlägt,
 * obwohl Fässer eine Gestalt der Karte sind, verliert Leben). Ein {@code *} am Ende heißt „beginnt mit“.
 *
 * <p>{@code tier}: die ersten fünfzehn hat jeder ({@link Tier#FREE}); die übrigen schaltet man im Kramerladen der
 * Gendarmerie frei. Auf welchen Karten es eine Gestalt gibt, legt {@code MapType} fest – nur dort, wo genug echte
 * Attrappen stehen.
 */
public enum Shape {

    HEUBALLEN("heuballen", "hay_block[axis=y]", "HAY_BLOCK", List.of("hay_block")),
    FASS("fass", "barrel[facing=up,open=false]", "BARREL", List.of("barrel")),
    BIERFASS("bierfass", "barrel[facing=east,open=false]", "BARREL", List.of("barrel")),
    BLUMENTOPF("blumentopf", "potted_red_tulip", "FLOWER_POT", List.of("flower_pot", "potted_*")),
    KUERBIS("kuerbis", "carved_pumpkin[facing=south]", "CARVED_PUMPKIN", List.of("pumpkin", "carved_pumpkin", "jack_o_lantern")),
    MELONE("melone", "melon", "MELON", List.of("melon")),
    AMBOSS("amboss", "anvil[facing=north]", "ANVIL", List.of("anvil", "chipped_anvil", "damaged_anvil")),
    BIENENSTOCK("bienenstock", "bee_nest[facing=south,honey_level=0]", "BEE_NEST", List.of("bee_nest", "beehive")),
    LATERNE("laterne", "lantern[hanging=false,waterlogged=false]", "LANTERN", List.of("lantern", "soul_lantern")),
    HACKSTOCK("hackstock", "stripped_spruce_log[axis=y]", "STRIPPED_SPRUCE_LOG", List.of("stripped_spruce_log")),
    KESSEL("kessel", "water_cauldron[level=3]", "CAULDRON", List.of("cauldron", "water_cauldron")),
    OFEN("ofen", "furnace[facing=south,lit=false]", "FURNACE", List.of("furnace", "smoker", "blast_furnace")),
    BUECHERREGAL("buecherregal", "bookshelf", "BOOKSHELF", List.of("bookshelf", "chiseled_bookshelf")),
    BANK("bank", "spruce_stairs[facing=south,half=bottom,shape=straight,waterlogged=false]", "SPRUCE_STAIRS",
            List.of("spruce_stairs")),
    KOMPOSTER("komposter", "composter[level=0]", "COMPOSTER", List.of("composter")),

    // ---------- im Kramerladen freischalten ----------
    WERKBANK("werkbank", "crafting_table", "CRAFTING_TABLE", List.of("crafting_table"), Tier.COMMON),
    SCHLEIFSTEIN("schleifstein", "grindstone[face=floor,facing=north]", "GRINDSTONE", List.of("grindstone"),
            Tier.COMMON),
    WEBSTUHL("webstuhl", "loom[facing=south]", "LOOM", List.of("loom"), Tier.COMMON),
    BUSCH("busch", "flowering_azalea", "FLOWERING_AZALEA", List.of("azalea", "flowering_azalea"), Tier.RARE),
    LESEPULT("lesepult", "lectern[facing=south,has_book=false,powered=false]", "LECTERN", List.of("lectern"),
            Tier.RARE),
    KUCHEN("kuchen", "cake[bites=0]", "CAKE", List.of("cake", "candle_cake"), Tier.RARE),
    KUERBISLATERNE("kuerbislaterne", "jack_o_lantern[facing=south]", "JACK_O_LANTERN",
            List.of("pumpkin", "carved_pumpkin", "jack_o_lantern"), Tier.EPIC),
    FESTTAGSKUCHEN("festtagskuchen", "candle_cake[lit=true]", "CANDLE", List.of("cake", "candle_cake"), Tier.EPIC);

    private final String id;
    private final String block;
    private final String icon;
    private final List<String> kinds;
    private final Tier tier;

    Shape(String id, String block, String icon, List<String> kinds) {
        this(id, block, icon, kinds, Tier.FREE);
    }

    Shape(String id, String block, String icon, List<String> kinds, Tier tier) {
        this.id = id;
        this.block = block;
        this.icon = icon;
        this.kinds = kinds;
        this.tier = tier;
    }

    /** Frei für alle oder im Kramerladen freizuschalten (und wie selten). */
    public Tier tier() {
        return tier;
    }

    public boolean free() {
        return tier == Tier.FREE;
    }

    public String id() {
        return id;
    }

    /** Blockzustand als Text, z. B. {@code barrel[facing=up,open=false]}. */
    public String block() {
        return block;
    }

    /** Name des Gegenstands fürs Menü (Bukkit-{@code Material}). */
    public String icon() {
        return icon;
    }

    public List<String> kinds() {
        return kinds;
    }

    /** Gehört ein Block (Name ohne Zustand, z. B. {@code potted_poppy}) zu dieser Art? */
    public boolean sameKind(String blockName) {
        String name = baseName(blockName);
        for (String k : kinds) {
            if (k.endsWith("*") ? name.startsWith(k.substring(0, k.length() - 1)) : name.equals(k)) {
                return true;
            }
        }
        return false;
    }

    /** {@code minecraft:barrel[facing=up]} → {@code barrel}. */
    public static String baseName(String spec) {
        String s = spec.toLowerCase(Locale.ROOT);
        int colon = s.indexOf(':');
        if (colon >= 0) {
            s = s.substring(colon + 1);
        }
        int bracket = s.indexOf('[');
        return bracket >= 0 ? s.substring(0, bracket) : s;
    }

    public static Shape byId(String id) {
        for (Shape s : values()) {
            if (s.id.equalsIgnoreCase(id) || s.name().equalsIgnoreCase(id)) {
                return s;
            }
        }
        return null;
    }
}
