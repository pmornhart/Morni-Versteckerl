package de.moinmorni.versteckerl.world;

import de.moinmorni.versteckerl.rules.Shape;
import de.moinmorni.versteckerl.world.MapLayout.Point;
import java.util.List;

/**
 * Die Karten. Jede liegt an ihrem eigenen Platz in der Welt (Mittelpunkt {@code originX}/{@code originZ}, Boden auf
 * {@link #FLOOR_Y}); auf jeder Karte läuft höchstens eine Runde gleichzeitig. Die Karte bestimmt, in welche Gestalten
 * man sich verwandeln kann – und von genau diesen Dingen stehen überall echte Attrappen herum. Gestalten aus dem
 * Kramerladen gibt es nur auf den Karten, auf denen sie stehen (und nur für die, die sie freigeschaltet haben).
 */
public enum MapType {

    /** Dorfplatz: Bauerndorf mit Brunnen, Maibaum, Wirtshaus, Stadel, Kramerladen und Schmiede. */
    DORFPLATZ("dorfplatz", 1000, 0, List.of(Shape.HEUBALLEN, Shape.FASS, Shape.BLUMENTOPF, Shape.KUERBIS,
            Shape.AMBOSS, Shape.BIENENSTOCK, Shape.LATERNE, Shape.HACKSTOCK, Shape.KESSEL, Shape.WERKBANK,
            Shape.SCHLEIFSTEIN, Shape.WEBSTUHL, Shape.BUSCH, Shape.KUCHEN, Shape.KUERBISLATERNE),
            new MapLayout(new Point(0.5, 1, 5.5), 180f, new Point(0.5, 1, 27.5), 180f, new Point(0.5, -7, 25.5),
                    new Point(0.5, 22, 30.5), 30, 22)),
    /** Wirtshaus: großes Gasthaus von innen – Gaststube, Küche, Stube, Bierkeller und Saal. */
    WIRTSHAUS("wirtshaus", 2000, 0, List.of(Shape.FASS, Shape.BIERFASS, Shape.BLUMENTOPF, Shape.LATERNE,
            Shape.KESSEL, Shape.OFEN, Shape.BUECHERREGAL, Shape.BANK, Shape.WERKBANK, Shape.BUSCH, Shape.LESEPULT,
            Shape.KUCHEN, Shape.FESTTAGSKUCHEN),
            new MapLayout(new Point(-9.5, 1, 5.5), 180f, new Point(-9.5, 1, 23.5), 180f, new Point(-9.5, -14, 24.5),
                    new Point(0.5, 20, 28.5), 30, 24)),
    /** Heustadel: Bauernhof mit großem Stadel, Heuboden, Stall, Gemüsegarten, Bienen und Obstwiese. */
    HEUSTADEL("heustadel", 3000, 0, List.of(Shape.HEUBALLEN, Shape.FASS, Shape.KUERBIS, Shape.MELONE,
            Shape.BIENENSTOCK, Shape.KOMPOSTER, Shape.HACKSTOCK, Shape.LATERNE, Shape.KESSEL, Shape.WERKBANK,
            Shape.SCHLEIFSTEIN, Shape.WEBSTUHL, Shape.BUSCH, Shape.KUERBISLATERNE),
            new MapLayout(new Point(0.5, 1, 4.5), 180f, new Point(0.5, 1, 27.5), 180f, new Point(0.5, -7, 25.5),
                    new Point(0.5, 24, 30.5), 30, 22));

    /** Höhe der obersten Bodenschicht in der Welt (man steht eins darüber). */
    public static final int FLOOR_Y = 100;

    private final String id;
    private final int originX;
    private final int originZ;
    private final List<Shape> shapes;
    private final MapLayout layout;

    MapType(String id, int originX, int originZ, List<Shape> shapes, MapLayout layout) {
        this.id = id;
        this.originX = originX;
        this.originZ = originZ;
        this.shapes = shapes;
        this.layout = layout;
    }

    public String id() {
        return id;
    }

    public int originX() {
        return originX;
    }

    public int originZ() {
        return originZ;
    }

    /** Alle Gestalten dieser Karte (freie und aus dem Kramerladen) – von allen stehen hier echte Attrappen. */
    public List<Shape> shapes() {
        return shapes;
    }

    /** Die Gestalten, die hier jeder hat. */
    public List<Shape> freeShapes() {
        return shapes.stream().filter(Shape::free).toList();
    }

    /** Gestalten aus dem Kramerladen, die es auf dieser Karte gibt. */
    public List<Shape> shopShapes() {
        return shapes.stream().filter(s -> !s.free()).toList();
    }

    public MapLayout layout() {
        return layout;
    }

    /** Ist dieser Block (Name) die Art einer Gestalt dieser Karte – also eine mögliche Attrappe? */
    public boolean decoy(String blockName) {
        for (Shape s : shapes) {
            if (s.sameKind(blockName)) {
                return true;
            }
        }
        return false;
    }

    /** Bauplan der Karte (reiner Text, ohne Server). */
    public Plan plan() {
        return switch (this) {
            case DORFPLATZ -> DorfplatzPlan.build(layout);
            case WIRTSHAUS -> WirtshausPlan.build(layout);
            case HEUSTADEL -> HeustadelPlan.build(layout);
        };
    }

    public static MapType byId(String id) {
        for (MapType m : values()) {
            if (m.id.equalsIgnoreCase(id) || m.name().equalsIgnoreCase(id)) {
                return m;
            }
        }
        return null;
    }
}
