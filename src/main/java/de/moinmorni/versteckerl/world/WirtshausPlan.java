package de.moinmorni.versteckerl.world;

import java.util.Random;

/**
 * „Wirtshaus“: ein großes Gasthaus am Berghang, fast alles drinnen. Im Erdgeschoss die Gaststube mit Schänke,
 * Bierfass-Regal, Stammtisch und Kachelofen, die Küche mit Herden und Kesseln und die gemütliche Stube mit
 * Bücherwand. Darunter der Bierkeller mit Fass-Gewölbe, darüber der Tanzsaal und zwei Gästezimmer. Draußen der
 * Biergarten unter Kastanien, die Kegelbahn und der Hinterhof. Eigener Entwurf.
 */
final class WirtshausPlan {

    private static final int HALF = 30;
    static final int X1 = -22;
    static final int X2 = 22;
    static final int Z1 = -18;
    static final int Z2 = 8;
    /** Innenwand zwischen Gaststube (West) und Küche/Stube (Ost). */
    private static final int WALL_X = 3;
    /** Innenwand zwischen Küche (Nord) und Stube (Süd). */
    private static final int WALL_Z = -6;
    private static final int CELLAR_FLOOR = -5;
    private static final String LANTERN = "lantern[hanging=false,waterlogged=false]";
    private static final String BARREL_UP = "barrel[facing=up,open=false]";

    private WirtshausPlan() {
    }

    static Plan build(MapLayout layout) {
        Plan p = new Plan();
        Random r = new Random(1810);
        Decor.island(p, 42, 42, 26, r);
        mountains(p, r);
        building(p, r);
        cellar(p, r);
        taproom(p, r);
        kitchen(p, r);
        parlour(p, r);
        upstairs(p, r);
        beerGarden(p, r);
        skittles(p, r);
        backyard(p, r);
        DorfplatzPlan.booth(p, layout);
        Decor.flowers(p, -HALF, -HALF, HALF, HALF, 6, r, (x, z) -> Math.abs(x + 9) <= 1 && z > Z2);
        Decor.barrierBox(p, HALF, layout.ceiling());
        p.connect();
        return p;
    }

    private static void mountains(Plan p, Random r) {
        int[][] peaks = {{-36, -38, 14, 30}, {2, -42, 12, 34}, {36, -36, 14, 28}, {-42, 6, 10, 18},
            {42, 0, 10, 20}};
        for (int[] pk : peaks) {
            Plan m = new Plan();
            Decor.mountain(m, pk[0], pk[1], 0, pk[2], pk[3], r);
            m.forEach((k, s) -> {
                int x = Plan.keyX(k);
                int y = Plan.keyY(k);
                int z = Plan.keyZ(k);
                if ((Math.abs(x) <= HALF + 2 && Math.abs(z) <= HALF + 2) || y < 0 || p.get(x, 0, z) == null) {
                    return;
                }
                p.set(x, y, z, s);
            });
        }
    }

    /** Hülle: Alpenhaus mit zwei Geschossen, Innenwände mit Durchgängen, Treppen hinauf und hinunter. */
    private static void building(Plan p, Random r) {
        Kit.House h = new Kit.House(X1, Z1, X2, Z2, 's', -9, "calcite", "dark_oak", true, true, "spruce_planks");
        Kit.house(p, h, r);
        // Großes Alpenhaus: flaches Dach statt des steilen aus Kit.house
        p.clear(X1 - 1, Kit.UPPER_TOP + 1, Z1 - 1, X2 + 1, Kit.UPPER_TOP + 30, Z2 + 1);
        Kit.flatRoof(p, X1, Z1, X2, Z2, Kit.UPPER_TOP + 1, "dark_oak", false);
        for (int y = Kit.UPPER_TOP + 1; y <= Kit.UPPER_TOP + 9; y++) {
            p.set(X2 - 4, y, Z1 + 3, "bricks");
            p.set(X1 + 6, y, Z1 + 4, "bricks");
        }
        // Dielenboden im Erdgeschoss und oben
        for (int x = X1 + 1; x < X2; x++) {
            for (int z = Z1 + 1; z < Z2; z++) {
                p.set(x, 0, z, (x + z) % 7 == 0 ? "stripped_dark_oak_wood[axis=y]" : "spruce_planks");
                p.set(x, Kit.GROUND_TOP + 1, z, "dark_oak_planks");
            }
        }
        // Innenwände (beide Geschosse)
        for (int y = 1; y <= Kit.UPPER_TOP; y++) {
            if (y == Kit.GROUND_TOP + 1) {
                continue;
            }
            String wall = y <= Kit.GROUND_TOP ? "white_terracotta" : "spruce_planks";
            for (int z = Z1 + 1; z < Z2; z++) {
                p.set(WALL_X, y, z, wall);
            }
            for (int x = WALL_X + 1; x < X2; x++) {
                p.set(x, y, WALL_Z, wall);
            }
        }
        // Durchgänge (2 hoch, 2 breit)
        for (int base : new int[] {1, Kit.GROUND_TOP + 2}) {
            for (int y = base; y <= base + 1; y++) {
                for (int d = 0; d <= 1; d++) {
                    p.clear(WALL_X, y, -12 + d);
                    p.clear(WALL_X, y, 2 + d);
                    p.clear(12 + d, y, WALL_Z);
                }
            }
        }
        // Treppe hinauf an der Westwand (zwei breit): von z = 7 (unten) nach z = 3 (oben)
        for (int i = 0; i < 5; i++) {
            for (int x = X1 + 1; x <= X1 + 2; x++) {
                p.set(x, 1 + i, 7 - i, "dark_oak_stairs[facing=north,half=bottom,shape=straight]");
                for (int y = 2 + i; y <= Kit.GROUND_TOP + 2; y++) {
                    p.clear(x, y, 7 - i);
                }
            }
        }
        for (int z = 3; z <= 7; z++) {
            p.set(X1 + 3, Kit.GROUND_TOP + 2, z, "dark_oak_fence");
        }
        // Hintertür der Küche zum Hinterhof
        p.set(14, 1, Z1, "spruce_door[facing=north,half=lower,hinge=left,open=true,powered=false]");
        p.set(14, 2, Z1, "spruce_door[facing=north,half=upper,hinge=left,open=true,powered=false]");
    }

    /** Gaststube: Schänke mit Bierfass-Regal, drei lange Tische, Stammtisch, Kachelofen mit Ofenbank. */
    private static void taproom(Plan p, Random r) {
        // Schänke: Theke bei z = -13, dahinter liegende Fässer (Bierfässer) zwei hoch, ein Spülkessel
        for (int x = -21; x <= -11; x++) {
            p.set(x, 1, -13, x == -11 ? "dark_oak_fence_gate[facing=east,open=true,in_wall=false]"
                    : "dark_oak_slab[type=top]");
            if (x % 2 == 0 && x > -21) {
                p.set(x, 2, -13, x % 4 == 0 ? LANTERN : "potted_red_tulip");
            }
            p.set(x, 1, -17, "barrel[facing=east,open=false]");
            p.set(x, 2, -17, "barrel[facing=east,open=false]");
            p.set(x, 3, -17, x % 3 == 0 ? "potted_cornflower" : BARREL_UP);
        }
        p.set(-15, 1, -15, "water_cauldron[level=3]");
        p.set(-19, 1, -15, BARREL_UP);
        p.set(-12, 1, -16, "barrel[facing=west,open=false]");
        // Lange Tische mit Bänken
        for (int z : new int[] {-8, -3, 2}) {
            Kit.longTable(p, -10, 1, z, 7, true, "dark_oak", true, r, LANTERN, "potted_poppy", "potted_azure_bluet");
        }
        // Stammtisch in der Ecke (rund, mit Laterne) und Wandbank
        Kit.roundTable(p, -17, 1, 2, "dark_oak", LANTERN);
        for (int z = -6; z <= 0; z++) {
            p.set(-21, 1, z, "spruce_stairs[facing=west,half=bottom,shape=straight]");
        }
        p.set(-21, 1, -7, BARREL_UP);
        // Kachelofen mit Ofenbank
        Kit.tiledStove(p, -1, 1, -17, true);
        for (int x = -2; x <= 2; x++) {
            p.set(x, 1, -15, "spruce_stairs[facing=south,half=bottom,shape=straight]");
        }
        // Freistehende Fässer, Laternen am Boden, Hängelaternen
        for (int[] b : new int[][] {{-1, 6}, {1, -10}, {-20, -10}, {0, 0}}) {
            p.set(b[0], 1, b[1], BARREL_UP);
        }
        p.set(-12, 1, 6, LANTERN);
        p.set(2, 1, -4, LANTERN);
        for (int x = -14; x <= -2; x += 6) {
            for (int z = -8; z <= 2; z += 5) {
                Decor.hangingLantern(p, x, Kit.GROUND_TOP, z, 0);
            }
        }
        p.set(-13, 1, 7, "potted_red_tulip");
    }

    /** Küche: Herde und Räucheröfen an der Nordwand, Arbeitsplatte, Kessel, Fässer, Kellerabgang. */
    private static void kitchen(Plan p, Random r) {
        for (int x = 4; x <= 13; x++) {
            p.set(x, 1, -17, switch (Math.floorMod(x, 3)) {
                case 0 -> "furnace[facing=south,lit=false]";
                case 1 -> "smoker[facing=south,lit=false]";
                default -> "furnace[facing=south,lit=true]";
            });
            p.set(x, 2, -17, "polished_andesite");
            p.set(x, 3, -17, "polished_andesite_slab[type=bottom]");
        }
        for (int x = 5; x <= 11; x++) {
            p.set(x, 4, -16, "stone_brick_slab[type=top]");
        }
        // Freistehender Herd in der Mitte (Verstecke zwischen den Öfen)
        p.set(14, 1, -10, "furnace[facing=west,lit=false]");
        p.set(14, 1, -9, "smoker[facing=west,lit=false]");
        // Arbeitsplatte an der Innenwand
        for (int z = -16; z <= -8; z++) {
            if (z == -12 || z == -11) {
                continue;
            }
            p.set(4, 1, z, Math.floorMod(z, 3) == 0 ? "crafting_table" : "spruce_slab[type=top]");
            if (Math.floorMod(z, 4) == 0) {
                p.set(4, 2, z, "potted_dandelion");
            }
        }
        // Kessel und Fässer
        for (int[] c : new int[][] {{8, -12}, {16, -9}, {17, -16}}) {
            p.set(c[0], 1, c[1], "water_cauldron[level=" + (1 + Math.floorMod(c[0] + c[1], 3)) + "]");
        }
        p.set(10, 1, -9, "cauldron");
        for (int[] b : new int[][] {{16, -13}, {13, -14}, {6, -8}, {18, -8}, {11, -16}}) {
            p.set(b[0], 1, b[1], BARREL_UP);
        }
        // Großer Küchentisch
        Kit.longTable(p, 10, 1, -14, 4, false, "spruce", false, r, "potted_poppy", LANTERN);
        // Kellerabgang an der Ostwand: Öffnung im Boden, Treppe nach Süden hinab
        for (int i = 0; i < 5; i++) {
            for (int x = 20; x <= 21; x++) {
                int z = -15 + i;
                p.set(x, -i, z, "dark_oak_stairs[facing=north,half=bottom,shape=straight]");
                for (int y = -i + 1; y <= 0; y++) {
                    p.clear(x, y, z);
                }
            }
        }
        for (int x = 20; x <= 21; x++) {
            for (int y = -4; y <= 0; y++) {
                p.clear(x, y, -10);
            }
        }
        for (int z = -15; z <= -10; z++) {
            p.set(19, 1, z, "spruce_fence");
        }
        Decor.hangingLantern(p, 8, Kit.GROUND_TOP, -12, 0);
        Decor.hangingLantern(p, 15, Kit.GROUND_TOP, -12, 0);
    }

    /** Stube: Bücherwand, Lehnstühle, Teppich, Kachelofen, runder Tisch. */
    private static void parlour(Plan p, Random r) {
        for (int z = -4; z <= 6; z++) {
            for (int y = 1; y <= 3; y++) {
                if (z == 1 && y <= 2) {
                    continue;
                }
                p.set(21, y, z, "bookshelf");
            }
        }
        p.set(21, 1, 1, "potted_allium");
        // freistehende Regale (1 hoch) zum Verstecken
        for (int[] b : new int[][] {{17, -2}, {17, 4}, {8, 6}, {14, 6}}) {
            p.set(b[0], 1, b[1], "bookshelf");
        }
        // Teppich
        for (int x = 9; x <= 15; x++) {
            for (int z = -2; z <= 3; z++) {
                p.set(x, 1, z, (x == 9 || x == 15 || z == -2 || z == 3) ? "red_carpet" : "brown_carpet");
            }
        }
        Kit.roundTable(p, 12, 1, 0, "dark_oak", "potted_cornflower");
        // Lehnstühle (Treppen) an den Wänden
        for (int[] s : new int[][] {{5, -4, 'w'}, {5, 0, 'w'}, {5, 5, 'w'}, {19, 6, 's'}, {11, 6, 's'}}) {
            p.set(s[0], 1, s[1], "spruce_stairs[facing=" + Kit.facing((char) s[2]) + ",half=bottom,shape=straight]");
        }
        Kit.tiledStove(p, 18, 1, -5, true);
        p.set(17, 1, -3, "spruce_stairs[facing=west,half=bottom,shape=straight]");
        p.set(6, 1, 2, LANTERN);
        p.set(20, 1, 6, BARREL_UP);
        Decor.hangingLantern(p, 12, Kit.GROUND_TOP, -3, 0);
    }

    /** Bierkeller: Gewölbe mit Säulen, Fass-Regale (liegende Bierfässer), stehende Fässer, Kessel, Laternen. */
    private static void cellar(Plan p, Random r) {
        for (int x = X1; x <= X2; x++) {
            for (int z = Z1; z <= Z2; z++) {
                boolean edge = x == X1 || x == X2 || z == Z1 || z == Z2;
                p.set(x, CELLAR_FLOOR, z, edge ? "stone_bricks" : Decor.pick(r, "cobblestone", "stone_bricks",
                        "stone_bricks", "mossy_stone_bricks"));
                for (int y = CELLAR_FLOOR + 1; y <= -1; y++) {
                    if (edge) {
                        p.set(x, y, z, Decor.pick(r, "stone_bricks", "stone_bricks", "cracked_stone_bricks",
                                "mossy_stone_bricks"));
                    } else {
                        p.clear(x, y, z);
                    }
                }
            }
        }
        // Säulen mit Bögen
        for (int x = -16; x <= 16; x += 8) {
            for (int z = -11; z <= 3; z += 7) {
                for (int y = CELLAR_FLOOR + 1; y <= -1; y++) {
                    p.set(x, y, z, "stone_bricks");
                }
                p.set(x + 1, -1, z, "stone_brick_stairs[facing=west,half=top,shape=straight]");
                p.set(x - 1, -1, z, "stone_brick_stairs[facing=east,half=top,shape=straight]");
                p.set(x, -1, z + 1, "stone_brick_stairs[facing=north,half=top,shape=straight]");
                p.set(x, -1, z - 1, "stone_brick_stairs[facing=south,half=top,shape=straight]");
            }
        }
        // Fass-Regale: liegende Fässer, zwei hoch, an der Nord- und Südwand
        for (int x = -21; x <= 17; x++) {
            if (Math.floorMod(x, 6) == 0) {
                continue;
            }
            p.set(x, -4, -17, "barrel[facing=south,open=false]");
            p.set(x, -3, -17, "barrel[facing=south,open=false]");
            if (x < 12) {
                p.set(x, -4, 7, "barrel[facing=north,open=false]");
                if (x % 2 == 0) {
                    p.set(x, -3, 7, "barrel[facing=north,open=false]");
                }
            }
        }
        // Reihe großer Fässer in der Mitte (liegend, mit Lücken zum Verstecken)
        for (int x = -19; x <= 13; x += 4) {
            p.set(x, -4, -4, "barrel[facing=east,open=false]");
            p.set(x + 1, -4, -4, "barrel[facing=west,open=false]");
            p.set(x, -3, -4, BARREL_UP);
        }
        // Stehende Fässer, Kessel, Laternen
        for (int[] b : new int[][] {{-20, 0}, {-14, -8}, {-6, 4}, {4, -9}, {10, 1}, {16, 5}, {-2, -13}}) {
            p.set(b[0], -4, b[1], BARREL_UP);
        }
        for (int[] c : new int[][] {{-18, 4}, {6, -14}, {14, -6}}) {
            p.set(c[0], -4, c[1], "water_cauldron[level=3]");
        }
        for (int[] l : new int[][] {{-10, -8}, {2, 4}, {12, -12}, {-20, -12}, {7, 0}}) {
            p.set(l[0], -4, l[1], LANTERN);
        }
        for (int[] l : new int[][] {{-12, -1}, {0, -8}, {12, 4}, {-4, 5}}) {
            Decor.hangingLantern(p, l[0], -1, l[1], 0);
        }
        for (int[] c : new int[][] {{-21, -16}, {21, 6}, {-21, 6}}) {
            p.set(c[0], -2, c[1], "cobweb");
        }
    }

    /** Oben: Tanzsaal mit Bänken und Tischen, zwei Gästezimmer. */
    private static void upstairs(Plan p, Random r) {
        int y = Kit.GROUND_TOP + 2;
        // Saal: lange Tische, Bühne im Norden
        for (int x = -16; x <= -4; x += 6) {
            Kit.longTable(p, x, y, -9, 5, false, "spruce", true, r, LANTERN, "potted_poppy");
        }
        for (int x = -21; x <= 2; x++) {
            for (int z = -17; z <= -15; z++) {
                p.set(x, y, z, "spruce_slab[type=bottom]");
            }
        }
        p.set(-12, y, -16, "note_block");
        p.set(-8, y + 1, -17, "potted_white_tulip");
        p.set(-14, y + 1, -16, LANTERN);
        for (int[] b : new int[][] {{1, 6}, {-20, -2}, {1, -4}, {-6, 6}}) {
            p.set(b[0], y, b[1], BARREL_UP);
        }
        for (int x = -15; x <= -3; x += 6) {
            Decor.hangingLantern(p, x, Kit.UPPER_TOP, -2, 0);
        }
        // Gästezimmer (Nord und Süd)
        for (int[] room : new int[][] {{-16, -8}, {-4, 6}}) {
            int z1 = room[0];
            int z2 = room[1];
            int mid = (z1 + z2) / 2;
            p.set(20, y, z1, "red_bed[facing=east,part=foot,occupied=false]");
            p.set(21, y, z1, "red_bed[facing=east,part=head,occupied=false]");
            p.set(20, y, z1 + 2, "blue_bed[facing=east,part=foot,occupied=false]");
            p.set(21, y, z1 + 2, "blue_bed[facing=east,part=head,occupied=false]");
            p.set(21, y, z1 + 1, "barrel[facing=west,open=false]");
            p.set(21, y + 1, z1 + 1, "potted_azure_bluet");
            p.set(5, y, z2, "bookshelf");
            p.set(5, y + 1, z2, "bookshelf");
            p.set(6, y, z2, BARREL_UP);
            p.set(9, y, mid, "spruce_stairs[facing=west,half=bottom,shape=straight]");
            p.set(10, y, mid, "dark_oak_fence");
            p.set(10, y + 1, mid, LANTERN);
            p.set(16, y, z2, "water_cauldron[level=3]");
            p.set(13, y, z1 + 1, "bookshelf");
            for (int x = 13; x <= 17; x++) {
                for (int z = mid - 1; z <= mid + 1; z++) {
                    p.setIfEmpty(x, y, z, "white_carpet");
                }
            }
            Decor.hangingLantern(p, 12, Kit.UPPER_TOP, mid, 0);
        }
    }

    /** Biergarten vor dem Haus: Kies, Kastanien, lange Tische mit Bänken, Fässer, Laternenpfähle. */
    private static void beerGarden(Plan p, Random r) {
        for (int x = -24; x <= 20; x++) {
            for (int z = Z2 + 1; z <= 20; z++) {
                p.set(x, 0, z, Math.abs(x + 9) <= 1 ? "dirt_path" : Decor.pick(r, "gravel", "gravel", "coarse_dirt"));
            }
        }
        for (int z = 21; z <= HALF; z++) {
            for (int x = -10; x <= -8; x++) {
                p.set(x, 0, z, "dirt_path");
            }
        }
        for (int x : new int[] {-21, -2, 9}) {
            Kit.longTable(p, x, 1, 12, 6, true, "spruce", true, r, LANTERN, "potted_cornflower", "potted_poppy");
            Kit.longTable(p, x, 1, 17, 6, true, "spruce", true, r, LANTERN, "potted_red_tulip");
        }
        for (int[] t : new int[][] {{-14, 15}, {5, 15}, {18, 15}}) {
            Decor.broadTree(p, t[0], 1, t[1], "dark_oak_log", "dark_oak_leaves", 5, 3, r);
        }
        for (int[] b : new int[][] {{-24, 9}, {-24, 20}, {20, 9}, {-12, 20}, {3, 20}, {16, 10}, {-4, 9}}) {
            p.set(b[0], 1, b[1], BARREL_UP);
        }
        p.set(-23, 1, 9, "water_cauldron[level=3]");
        for (int[] l : new int[][] {{-7, 10}, {-11, 10}, {-7, 20}, {-11, 20}}) {
            Decor.lanternPost(p, l[0], 1, l[1], "dark_oak_fence");
        }
        // Zaun mit Blumentöpfen
        for (int x = -25; x <= 21; x++) {
            if (Math.abs(x + 9) > 1) {
                p.set(x, 1, 21, "spruce_fence");
                if (Math.floorMod(x, 4) == 0) {
                    p.set(x, 2, 21, Decor.POTS[r.nextInt(Decor.POTS.length)]);
                }
            }
        }
    }

    /** Kegelbahn an der Ostseite unter einem Pultdach. */
    private static void skittles(Plan p, Random r) {
        for (int z = -14; z <= 18; z++) {
            for (int x = 24; x <= 28; x++) {
                p.set(x, 0, z, x == 26 ? "smooth_stone" : "spruce_planks");
            }
            if (Math.floorMod(z, 4) == 0) {
                for (int y = 1; y <= 3; y++) {
                    p.set(24, y, z, "spruce_fence");
                    p.set(28, y, z, "spruce_fence");
                }
            }
            for (int x = 23; x <= 29; x++) {
                p.set(x, 4, z, "spruce_slab[type=bottom]");
            }
        }
        for (int[] pin : new int[][] {{26, -13}, {25, -12}, {27, -12}, {26, -11}}) {
            p.set(pin[0], 1, pin[1], "white_candle[candles=1,lit=false,waterlogged=false]");
        }
        p.set(25, 1, 17, BARREL_UP);
        p.set(27, 1, 17, "spruce_stairs[facing=south,half=bottom,shape=straight]");
        p.set(27, 1, 10, LANTERN);
        p.set(25, 1, 2, "water_cauldron[level=2]");
    }

    /** Hinterhof im Norden: Kräuterbeet, Holzstoß, Waschkessel, Fässer. */
    private static void backyard(Plan p, Random r) {
        for (int x = 8; x <= 20; x++) {
            for (int z = -26; z <= Z1 - 1; z++) {
                p.set(x, 0, z, Decor.pick(r, "coarse_dirt", "gravel", "dirt"));
            }
        }
        Kit.woodpile(p, -20, 1, -27, 12, 2, true);
        for (int[] b : new int[][] {{10, -21}, {12, -24}, {18, -22}, {-4, -22}}) {
            p.set(b[0], 1, b[1], BARREL_UP);
        }
        p.set(15, 1, -23, "water_cauldron[level=3]");
        p.set(16, 1, -20, "cauldron");
        p.set(8, 1, -20, LANTERN);
        for (int x = -16; x <= -8; x++) {
            p.set(x, 0, -22, "farmland[moisture=7]");
            p.set(x, 1, -22, Decor.pick(r, "carrots[age=7]", "potatoes[age=7]", "beetroots[age=3]"));
        }
        p.set(-6, 1, -21, "potted_red_tulip");
        p.set(-18, 1, -21, "potted_orange_tulip");
        for (int[] t : new int[][] {{-27, -26}, {26, -27}, {-27, 0}, {27, 26}, {-27, 26}}) {
            Decor.spruce(p, t[0], 1, t[1], 5, r);
        }
    }
}
