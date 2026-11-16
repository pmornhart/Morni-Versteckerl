package net.morni.versteckerl.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.morni.versteckerl.rules.Shape;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Baupläne ohne Server prüfen: Attrappen, freie Startpunkte, dichte Gendarmenstube, Blocknamen von 26.x. */
class MapPlanTest {

    /** Wie viele Attrappen jeder Gestalt mindestens auf ihrer Karte stehen. */
    private static final int MIN_DECOYS = 6;
    /** Alte Blocknamen, die es in 26.x nicht mehr gibt (Block fehlte sonst still). */
    private static final Set<String> OLD_NAMES = Set.of("chain", "grass", "grass_path", "sign", "wall_sign");
    // Blockname mit optionalen Eigenschaften: barrel[facing=up,open=false]
    // https://regex101.com/?flavor=java&regex=%5Ba-z0-9_%5D%2B(%5C%5B%5Ba-z_%5D%2B%3D%5Ba-z0-9_%5D%2B(%2C%5Ba-z_%5D%2B%3D%5Ba-z0-9_%5D%2B)*%5C%5D)%3F&testString=barrel%5Bfacing%3Dup%2Copen%3Dfalse%5D
    private static final Pattern SPEC = Pattern.compile("[a-z0-9_]+(\\[[a-z_]+=[a-z0-9_]+(,[a-z_]+=[a-z0-9_]+)*\\])?");

    private static final Map<MapType, Plan> PLANS = new EnumMap<>(MapType.class);
    private static Plan hall;

    @BeforeAll
    static void build() {
        for (MapType m : MapType.values()) {
            PLANS.put(m, m.plan());
        }
        hall = HallPlan.build();
    }

    @Test
    void everyShapeHasEnoughDecoysOnItsMap() {
        for (MapType m : MapType.values()) {
            Plan p = PLANS.get(m);
            assertTrue(m.shapes().size() >= 6, m + ": zu wenige Gestalten");
            for (Shape s : m.shapes()) {
                int n = p.count(spec -> s.sameKind(spec));
                assertTrue(n >= MIN_DECOYS, m + ": nur " + n + " Attrappen für " + s.id());
            }
        }
    }

    /**
     * Gestalten aus dem Kramerladen: genau so aussehende Attrappen (gleicher Block) – sonst fiele ein Kuchen zwischen
     * lauter Festtagskuchen auf. Und jede steht auf mindestens einer Karte.
     */
    @Test
    void shopShapesHaveLookAlikes() {
        for (Shape s : Shape.values()) {
            if (s.free()) {
                continue;
            }
            boolean somewhere = false;
            for (MapType m : MapType.values()) {
                if (!m.shapes().contains(s)) {
                    continue;
                }
                somewhere = true;
                String base = Blocks.base(s.block());
                int n = PLANS.get(m).count(spec -> Blocks.base(spec).equals(base));
                assertTrue(n >= MIN_DECOYS, m + ": nur " + n + " echte " + base + " für " + s.id());
            }
            assertTrue(somewhere, s.id() + " gibt es auf keiner Karte");
        }
    }

    @Test
    void startPointsAreFree() {
        for (MapType m : MapType.values()) {
            Plan p = PLANS.get(m);
            MapLayout l = m.layout();
            for (var pt : new MapLayout.Point[] {l.hiders(), l.seekers(), l.booth()}) {
                assertTrue(p.standable(pt.bx(), pt.by(), pt.bz()), m + ": Punkt " + pt + " ist nicht frei: "
                        + p.get(pt.bx(), pt.by() - 1, pt.bz()) + " / " + p.get(pt.bx(), pt.by(), pt.bz()) + " / "
                        + p.get(pt.bx(), pt.by() + 1, pt.bz()));
            }
            assertTrue(l.inside(l.hiders().x(), l.hiders().z()), m + ": Versteckte starten außerhalb");
            assertTrue(l.inside(l.seekers().x(), l.seekers().z()), m + ": Gendarmen starten außerhalb");
        }
        assertTrue(hall.standable((int) Math.floor(HallPlan.SPAWN_X), (int) HallPlan.SPAWN_Y,
                (int) Math.floor(HallPlan.SPAWN_Z)), "Ankunft in der Gendarmerie ist nicht frei");
    }

    /** In der Gendarmenstube kommt niemand heraus: Flutfüllung durch nicht feste Blöcke bleibt drinnen. */
    @Test
    void boothIsClosed() {
        for (MapType m : MapType.values()) {
            Plan p = PLANS.get(m);
            MapLayout.Point b = m.layout().booth();
            Deque<int[]> todo = new ArrayDeque<>();
            Set<Long> seen = new HashSet<>();
            todo.add(new int[] {b.bx(), b.by(), b.bz()});
            int[][] dirs = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};
            while (!todo.isEmpty()) {
                int[] c = todo.poll();
                if (!seen.add(Plan.key(c[0], c[1], c[2]))) {
                    continue;
                }
                assertTrue(seen.size() < 60, m + ": Gendarmenstube ist offen");
                for (int[] d : dirs) {
                    int x = c[0] + d[0];
                    int y = c[1] + d[1];
                    int z = c[2] + d[2];
                    if (!p.solid(x, y, z) || Blocks.passable(p.get(x, y, z))) {
                        if (!p.solid(x, y, z)) {
                            todo.add(new int[] {x, y, z});
                        }
                    }
                }
            }
        }
    }

    @Test
    void invisibleWallAroundThePlayground() {
        for (MapType m : MapType.values()) {
            Plan p = PLANS.get(m);
            int w = m.layout().half() + 1;
            for (int y = 1; y <= m.layout().ceiling(); y++) {
                for (int i = -w; i <= w; i++) {
                    for (int[] c : new int[][] {{i, -w}, {i, w}, {-w, i}, {w, i}}) {
                        assertEquals("barrier", p.get(c[0], y, c[1]), m + " Rand bei " + c[0] + " " + y + " " + c[1]);
                    }
                }
            }
        }
    }

    @Test
    void onlyNamesOfTodaysMinecraft() {
        for (Plan p : allPlans()) {
            p.forEach((k, s) -> {
                assertTrue(SPEC.matcher(s).matches(), "Blockzustand falsch geschrieben: " + s);
                assertFalse(OLD_NAMES.contains(Blocks.base(s)), "alter Blockname: " + s);
            });
        }
    }

    @Test
    void hallStandsShowTheShapesOfEachMap() {
        MapType[] maps = MapType.values();
        for (int i = 0; i < maps.length; i++) {
            String top = hall.get(HallPlan.STANDS[i][0], 2, HallPlan.STANDS[i][1]);
            assertNotNull(top);
            String finalTop = top;
            assertTrue(maps[i].freeShapes().stream().anyMatch(s -> s.sameKind(finalTop)), maps[i] + ": " + top);
        }
    }

    @Test
    void fencesAndPanesConnect() {
        Plan p = new Plan();
        p.set(0, 1, 0, "spruce_fence");
        p.set(1, 1, 0, "spruce_fence");
        p.set(0, 1, 1, "stone_bricks");
        p.set(-1, 1, 0, "pumpkin");
        p.set(5, 1, 0, "cobblestone_wall");
        p.set(5, 1, 1, "cobblestone_wall");
        p.set(5, 1, -1, "cobblestone_wall");
        p.connect();
        assertEquals("spruce_fence[north=false,east=true,south=true,west=false]", p.get(0, 1, 0));
        assertEquals("cobblestone_wall[north=low,east=none,south=low,west=none,up=false]", p.get(5, 1, 0));
        assertEquals("barrel[facing=up,open=true]", Blocks.with("barrel[facing=up,open=false]", Map.of("open", "true")));
        assertTrue(Blocks.passable("short_grass"));
        assertFalse(Blocks.passable("barrel[facing=up]"));
        assertTrue(Blocks.full("light_blue_concrete"));
        assertFalse(Blocks.full("spruce_stairs[facing=north]"));
    }

    private static Iterable<Plan> allPlans() {
        java.util.List<Plan> all = new java.util.ArrayList<>(PLANS.values());
        all.add(hall);
        return all;
    }
}
