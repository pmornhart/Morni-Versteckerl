package net.morni.versteckerl.world;

import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Die Gendarmerie: der Wartebereich auf einer kleinen Alm-Insel. Im Norden das Gendarmeriehaus mit grünen
 * Fensterläden, davor der gepflasterte Hof mit der Anschlagtafel (Steckbriefe = Bestenliste) und drei Schaukästen,
 * einer je Karte, mit den Gestalten der Karte als kleiner Auslage. Ringsum Bänke, Laternen, Brunnen, Fichten.
 * Eigener Entwurf.
 */
public final class HallPlan {

    public static final double SPAWN_X = 0.5;
    public static final double SPAWN_Y = 1;
    public static final double SPAWN_Z = 7.5;
    public static final float SPAWN_YAW = 180f;
    /** Alles innerhalb dieses Abstands zur Mitte gehört zur Gendarmerie. */
    public static final int REACH = 26;
    /** Schaukästen (x, z der Mitte), einer je Karte in der Reihenfolge von {@link MapType}. */
    public static final int[][] STANDS = {{-8, -1}, {0, -1}, {8, -1}};
    /** Anschlagtafel mit den Steckbriefen (Bestenliste), an der Westseite des Hofs, Blick nach Osten. */
    public static final int[] BOARD = {-11, 4};

    private static final int COURT_X = 13;
    private static final int COURT_Z1 = -9;
    private static final int COURT_Z2 = 11;

    private HallPlan() {
    }

    public static Plan build() {
        Plan p = new Plan();
        Random r = new Random(1891);
        Decor.island(p, 24, 24, 16, r);
        court(p, r);
        building(p, r);
        board(p);
        stands(p);
        maypole(p);
        fountain(p);
        benches(p);
        trees(p, r);
        Decor.flowers(p, -26, -26, 26, 26, 4, r, (x, z) -> Math.abs(x) <= COURT_X && z >= COURT_Z1 && z <= COURT_Z2);
        p.connect();
        return p;
    }

    private static void court(Plan p, Random r) {
        for (int x = -COURT_X; x <= COURT_X; x++) {
            for (int z = COURT_Z1; z <= COURT_Z2; z++) {
                boolean rim = Math.abs(x) == COURT_X || z == COURT_Z2;
                p.set(x, 0, z, rim ? "polished_andesite" : Decor.lozenge(x, z, 2) ? "stone_bricks"
                        : Decor.pick(r, "polished_diorite", "polished_diorite", "calcite"));
            }
        }
        // Weg nach Süden
        for (int z = COURT_Z2 + 1; z <= 22; z++) {
            for (int x = -1; x <= 1; x++) {
                p.set(x, 0, z, Decor.pick(r, "dirt_path", "dirt_path", "coarse_dirt"));
            }
        }
        // Niedriger Zaun um den Hof mit Durchgang im Süden
        for (int x = -COURT_X; x <= COURT_X; x++) {
            if (Math.abs(x) > 1) {
                p.set(x, 1, COURT_Z2 + 1, "spruce_fence");
            }
        }
        for (int z = COURT_Z1; z <= COURT_Z2 + 1; z++) {
            p.set(-COURT_X - 1, 1, z, "spruce_fence");
            p.set(COURT_X + 1, 1, z, "spruce_fence");
        }
        for (int[] l : new int[][] {{-COURT_X, COURT_Z2}, {COURT_X, COURT_Z2}, {-COURT_X, -2}, {COURT_X, -2},
            {-2, COURT_Z2}, {2, COURT_Z2}}) {
            Decor.lanternPost(p, l[0], 1, l[1], "spruce_fence");
        }
    }

    /** Gendarmeriehaus mit grünen Läden und Wappen-Fahnen. */
    private static void building(Plan p, Random r) {
        Plan h = new Plan();
        Kit.house(h, new Kit.House(-10, -19, 10, -10, 's', 0, "calcite", "spruce", true, true,
                "spruce_planks"), r);
        // Grüne Fensterläden statt Holz
        java.util.Map<Long, String> green = new java.util.HashMap<>();
        h.forEach((k, s) -> {
            if (s.startsWith("spruce_trapdoor") && s.contains("open=true")) {
                green.put(k, Blocks.with("waxed_oxidized_copper_trapdoor" + s.substring("spruce_trapdoor".length()),
                        Map.of()));
            }
        });
        green.forEach((k, s) -> h.set(Plan.keyX(k), Plan.keyY(k), Plan.keyZ(k), s));
        p.paste(h, 0, 0, 0);
        // Wappen-Fahnen links und rechts der Tür
        for (int x : new int[] {-3, 3}) {
            p.banner(x, 4, -9, "green_wall_banner[facing=south]", Decor.gendarm());
        }
        // Wachhäusl neben der Tür (grün-weiß gestreift)
        for (int y = 1; y <= 3; y++) {
            p.set(-6, y, -8, y % 2 == 0 ? "green_concrete" : "white_concrete");
            p.set(-8, y, -8, y % 2 == 0 ? "green_concrete" : "white_concrete");
            p.set(-7, y, -9, y % 2 == 0 ? "white_concrete" : "green_concrete");
        }
        p.set(-7, 4, -8, "spruce_slab[type=bottom]");
        p.set(-6, 4, -8, "spruce_slab[type=bottom]");
        p.set(-8, 4, -8, "spruce_slab[type=bottom]");
        p.set(-7, 4, -9, "spruce_slab[type=bottom]");
        p.set(-7, 3, -8, "lantern[hanging=true,waterlogged=false]");
        // Blumen vor dem Haus
        for (int x = -9; x <= 9; x++) {
            if (Math.abs(x) > 2 && Math.abs(x + 7) > 1) {
                p.set(x, 0, -9, "grass_block[snowy=false]");
                p.set(x, 1, -9, Decor.pick(r, "red_tulip", "poppy", "cornflower", "oxeye_daisy", "short_grass"));
            }
        }
    }

    /** Anschlagtafel für die Steckbriefe (Bestenliste schwebt davor), steht an der Westseite und schaut nach Osten. */
    private static void board(Plan p) {
        int x = BOARD[0];
        int z = BOARD[1];
        for (int dz : new int[] {-3, 3}) {
            for (int y = 1; y <= 4; y++) {
                p.set(x, y, z + dz, "spruce_log[axis=y]");
            }
        }
        for (int dz = -2; dz <= 2; dz++) {
            for (int y = 2; y <= 4; y++) {
                p.set(x, y, z + dz, "dark_oak_planks");
            }
            p.set(x, 1, z + dz, "spruce_planks");
        }
        for (int dz = -4; dz <= 4; dz++) {
            p.set(x, 5, z + dz, "spruce_slab[type=bottom]");
            p.set(x - 1, 5, z + dz, "spruce_slab[type=bottom]");
            p.set(x + 1, 5, z + dz, "spruce_slab[type=bottom]");
        }
        p.set(x + 1, 1, z - 3, "potted_red_tulip");
        p.set(x + 1, 1, z + 3, "potted_cornflower");
        p.set(x + 1, 4, z - 3, "lantern[hanging=false,waterlogged=false]");
        p.set(x + 1, 4, z + 3, "lantern[hanging=false,waterlogged=false]");
    }

    /** Schaukästen: Sockel aus Stein, darauf die Gestalten der Karte, darüber ein kleines Vordach auf Pfosten. */
    private static void stands(Plan p) {
        MapType[] maps = MapType.values();
        String[] roofs = {"spruce", "dark_oak", "oak"};
        for (int i = 0; i < maps.length && i < STANDS.length; i++) {
            int x = STANDS[i][0];
            int z = STANDS[i][1];
            List<net.morni.versteckerl.rules.Shape> shapes = maps[i].shapes();
            for (int dx = -1; dx <= 1; dx++) {
                p.set(x + dx, 0, z, "chiseled_stone_bricks");
                p.set(x + dx, 1, z, "polished_andesite");
                p.set(x + dx, 2, z, shapes.get((dx + 1) * 2 % shapes.size()).block());
            }
            for (int dx : new int[] {-2, 2}) {
                for (int y = 1; y <= 4; y++) {
                    p.set(x + dx, y, z, roofs[i] + "_fence");
                }
            }
            for (int dx = -3; dx <= 3; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    p.set(x + dx, 5, z + dz, roofs[i] + "_slab[type=bottom]");
                }
            }
            p.set(x - 2, 5, z, roofs[i] + "_planks");
            p.set(x + 2, 5, z, roofs[i] + "_planks");
            p.set(x - 3, 4, z + 1, "lantern[hanging=true,waterlogged=false]");
            p.set(x + 3, 4, z + 1, "lantern[hanging=true,waterlogged=false]");
        }
    }

    /** Kleiner Maibaum mit Blumenrondell vorn im Hof. */
    private static void maypole(Plan p) {
        int x = -9;
        int z = 8;
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                double d = Math.hypot(dx, dz);
                if (d <= 2.3 && d > 0.5) {
                    p.set(x + dx, 0, z + dz, d > 1.6 ? "polished_andesite" : "grass_block[snowy=false]");
                    if (d <= 1.6) {
                        p.set(x + dx, 1, z + dz, (dx + dz) % 2 == 0 ? "red_tulip" : "cornflower");
                    }
                }
            }
        }
        p.set(x, 0, z, "polished_andesite");
        for (int y = 1; y <= 11; y++) {
            p.set(x, y, z, (y / 2) % 2 == 0 ? "white_concrete" : "light_blue_concrete");
        }
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx != 0 || dz != 0) {
                    p.set(x + dx, 9, z + dz, "spruce_leaves[persistent=true]");
                }
            }
        }
        p.set(x, 12, z, "spruce_leaves[persistent=true]");
        p.set(x, 13, z, "spruce_leaves[persistent=true]");
        p.set(x + 1, 6, z, "spruce_fence");
        p.banner(x + 2, 6, z, "white_wall_banner[facing=east]", Decor.lozenges());
        p.set(x - 1, 7, z, "spruce_fence");
        p.banner(x - 2, 7, z, "white_wall_banner[facing=west]", Decor.lozenges());
    }

    /** Kleiner Brunnen im Südosten. */
    private static void fountain(Plan p) {
        int cx = 9;
        int cz = 7;
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                boolean rim = Math.abs(dx) == 2 || Math.abs(dz) == 2;
                if (Math.abs(dx) == 2 && Math.abs(dz) == 2) {
                    continue;
                }
                p.set(cx + dx, 0, cz + dz, rim ? "stone_bricks" : "water[level=0]");
                if (rim) {
                    p.set(cx + dx, 1, cz + dz, "stone_brick_slab[type=bottom]");
                } else {
                    p.set(cx + dx, -1, cz + dz, "stone_bricks");
                }
            }
        }
        p.set(cx, 0, cz, "stone_bricks");
        p.set(cx, 1, cz, "stone_brick_wall");
        p.set(cx, 2, cz, "stone_brick_wall");
        p.set(cx, 3, cz, "lantern[hanging=false,waterlogged=false]");
    }

    private static void benches(Plan p) {
        for (int z = 0; z <= 3; z++) {
            p.set(COURT_X - 1, 1, z, "spruce_stairs[facing=east,half=bottom,shape=straight]");
        }
        for (int x = 4; x <= 6; x++) {
            p.set(x, 1, 10, "spruce_stairs[facing=south,half=bottom,shape=straight]");
            p.set(-x, 1, 10, "spruce_stairs[facing=south,half=bottom,shape=straight]");
        }
        p.set(COURT_X - 1, 1, -1, "barrel[facing=up,open=false]");
        p.set(COURT_X - 1, 2, -1, "potted_red_tulip");
        p.set(-COURT_X + 1, 1, 11, "hay_block[axis=y]");
        p.set(-COURT_X + 2, 1, 11, "barrel[facing=up,open=false]");
        p.set(12, 1, -6, "barrel[facing=up,open=false]");
        p.set(11, 1, -7, "pumpkin");
    }

    private static void trees(Plan p, Random r) {
        int[][] spots = {{-18, -14}, {17, -15}, {-20, 4}, {19, 3}, {-15, 17}, {14, 18}, {-22, -4}, {21, -6},
            {-8, 19}, {7, 20}};
        for (int[] s : spots) {
            if (p.get(s[0], 0, s[1]) != null && p.get(s[0], 1, s[1]) == null) {
                Decor.spruce(p, s[0], 1, s[1], 4 + r.nextInt(3), r);
            }
        }
        Decor.broadTree(p, -17, 1, 10, "oak_log", "oak_leaves", 4, 3, r);
        Decor.broadTree(p, 17, 1, 11, "birch_log", "birch_leaves", 4, 3, r);
    }
}
