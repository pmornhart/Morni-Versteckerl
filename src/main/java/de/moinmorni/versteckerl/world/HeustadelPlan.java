package de.moinmorni.versteckerl.world;

import java.util.Random;

/**
 * „Heustadel“: ein Bauernhof auf der Alm. In der Mitte der große Stadel mit Tenne, Ställen und zwei Heuböden (über
 * Leitern zu erreichen), daneben das Bauernhaus, vorn der Hof mit Ziehbrunnen und Heuwagen. Im Südosten der
 * Gemüsegarten mit Kürbissen und Melonen und einer Vogelscheuche, im Südwesten die Bienenwiese mit Bienenhaus, im
 * Westen die Kompostecke, im Norden Hühnerstall, Obstwiese und Holzlege. Eigener Entwurf.
 */
final class HeustadelPlan {

    private static final int HALF = 30;
    private static final int BX1 = -14;
    private static final int BX2 = 14;
    private static final int BZ1 = -22;
    private static final int BZ2 = -2;
    private static final int BARN_TOP = 7;
    private static final int LOFT = 5;
    private static final String LANTERN = "lantern[hanging=false,waterlogged=false]";
    private static final String BARREL_UP = "barrel[facing=up,open=false]";
    private static final String STUMP = "stripped_spruce_log[axis=y]";

    private HeustadelPlan() {
    }

    static Plan build(MapLayout layout) {
        Plan p = new Plan();
        Random r = new Random(1871);
        Decor.island(p, 44, 44, 24, r);
        mountains(p, r);
        yard(p, r);
        barn(p, r);
        farmhouse(p, r);
        garden(p, r);
        bees(p, r);
        compost(p, r);
        coop(p, r);
        orchard(p, r);
        woodshed(p, r);
        gate(p, r);
        DorfplatzPlan.booth(p, layout);
        Decor.flowers(p, -HALF, -HALF, HALF, HALF, 5, r, (x, z) -> Math.abs(x) <= 1 && z > 0);
        Decor.barrierBox(p, HALF, layout.ceiling());
        p.connect();
        return p;
    }

    private static void mountains(Plan p, Random r) {
        int[][] peaks = {{-38, -40, 15, 30}, {4, -44, 13, 34}, {40, -36, 14, 26}, {-44, 0, 11, 22},
            {44, 4, 11, 20}, {-34, 40, 10, 14}, {36, 40, 10, 16}};
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

    /** Hof vor dem Stadel: festgetretene Erde, Ziehbrunnen, Heuwagen, Fässer, Laternenpfähle. */
    private static void yard(Plan p, Random r) {
        for (int x = -16; x <= 16; x++) {
            for (int z = -1; z <= 7; z++) {
                p.set(x, 0, z, Decor.pick(r, "coarse_dirt", "dirt_path", "coarse_dirt", "gravel", "packed_mud"));
            }
        }
        for (int z = 8; z <= HALF; z++) {
            for (int x = -1; x <= 1; x++) {
                p.set(x, 0, z, Decor.pick(r, "dirt_path", "dirt_path", "coarse_dirt"));
            }
        }
        // Ziehbrunnen: Steinring mit Wasser, Dach auf Pfosten, Kessel daneben
        int wx = -8;
        int wz = 4;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                p.set(wx + dx, 0, wz + dz, dx == 0 && dz == 0 ? "water[level=0]" : "cobblestone");
                if (dx != 0 || dz != 0) {
                    p.set(wx + dx, 1, wz + dz, "cobblestone_wall");
                }
            }
        }
        p.set(wx, -1, wz, "cobblestone");
        for (int dx : new int[] {-1, 1}) {
            p.set(wx + dx, 2, wz, "spruce_fence");
            p.set(wx + dx, 3, wz, "spruce_fence");
        }
        for (int dx = -2; dx <= 2; dx++) {
            p.set(wx + dx, 4, wz - 1, "spruce_stairs[facing=south,half=bottom,shape=straight]");
            p.set(wx + dx, 4, wz + 1, "spruce_stairs[facing=north,half=bottom,shape=straight]");
            p.set(wx + dx, 4, wz, "spruce_slab[type=bottom]");
        }
        p.set(wx, 3, wz, "iron_chain[axis=y]");
        p.set(wx + 2, 1, wz + 1, "water_cauldron[level=3]");
        p.set(wx - 2, 1, wz - 1, "cauldron");
        // Heuwagen
        for (int x = 6; x <= 9; x++) {
            for (int z = 2; z <= 4; z++) {
                p.set(x, 1, z, "spruce_slab[type=bottom]");
                p.set(x, 2, z, "hay_block[axis=x]");
            }
            p.set(x, 1, 1, "spruce_trapdoor[facing=north,half=bottom,open=true]");
            p.set(x, 1, 5, "spruce_trapdoor[facing=south,half=bottom,open=true]");
        }
        p.set(7, 3, 3, "hay_block[axis=z]");
        p.set(8, 3, 3, "hay_block[axis=z]");
        p.set(10, 1, 3, "spruce_fence");
        p.set(11, 1, 3, "spruce_fence");
        // Fässer, Ballen, Kürbisse im Hof
        for (int[] b : new int[][] {{-15, 0}, {-14, 6}, {13, 6}, {15, 0}}) {
            p.set(b[0], 1, b[1], BARREL_UP);
        }
        p.set(4, 1, 6, "hay_block[axis=y]");
        p.set(-4, 1, 0, "hay_block[axis=x]");
        p.set(-12, 1, 2, "carved_pumpkin[facing=south]");
        p.set(12, 1, 0, "composter[level=4]");
        for (int[] l : new int[][] {{-3, 7}, {3, 7}, {-16, 7}, {16, 7}}) {
            Decor.lanternPost(p, l[0], 1, l[1], "spruce_fence");
        }
    }

    /** Der große Stadel: Tenne in der Mitte, Ställe im Westen, Lager im Osten, darüber zwei Heuböden. */
    private static void barn(Plan p, Random r) {
        for (int x = BX1; x <= BX2; x++) {
            for (int z = BZ1; z <= BZ2; z++) {
                boolean ex = x == BX1 || x == BX2;
                boolean ez = z == BZ1 || z == BZ2;
                boolean edge = ex || ez;
                p.set(x, 0, z, edge ? "cobblestone" : Math.abs(x) <= 3 ? "spruce_planks" : Decor.pick(r,
                        "coarse_dirt", "packed_mud", "coarse_dirt"));
                for (int y = 1; y <= BARN_TOP; y++) {
                    if (!edge) {
                        p.clear(x, y, z);
                        continue;
                    }
                    boolean post = (ex && ez) || (ez && Math.floorMod(x - BX1, 4) == 0)
                            || (ex && Math.floorMod(z - BZ1, 4) == 0);
                    p.set(x, y, z, post ? "spruce_log[axis=y]" : y == 1 ? "cobblestone" : "spruce_planks");
                }
            }
        }
        // Tore der Tenne (Süd und Nord, 7 breit, 5 hoch)
        for (int z : new int[] {BZ1, BZ2}) {
            for (int x = -3; x <= 3; x++) {
                for (int y = 1; y <= 5; y++) {
                    p.clear(x, y, z);
                }
                p.set(x, 0, z, "spruce_planks");
            }
            // offene Torflügel
            for (int y = 1; y <= 4; y++) {
                int out = z == BZ2 ? 1 : -1;
                p.set(-4, y, z + out, "spruce_trapdoor[facing=" + (out > 0 ? "south" : "north") + ",half=bottom,open=true]");
                p.set(4, y, z + out, "spruce_trapdoor[facing=" + (out > 0 ? "south" : "north") + ",half=bottom,open=true]");
            }
        }
        // Lüftungsschlitze
        for (int x = BX1 + 2; x <= BX2 - 2; x += 4) {
            if (Math.abs(x) > 4) {
                p.set(x, 4, BZ2, "spruce_fence");
                p.set(x, 4, BZ1, "spruce_fence");
            }
        }
        Kit.roof(p, BX1, BZ1, BX2, BZ2, BARN_TOP + 1, "spruce", false);
        // Heuböden über beiden Seiten
        for (int x = BX1 + 1; x < BX2; x++) {
            if (Math.abs(x) <= 4) {
                continue;
            }
            for (int z = BZ1 + 1; z < BZ2; z++) {
                p.set(x, LOFT, z, "spruce_planks");
            }
        }
        for (int z = BZ1 + 1; z < BZ2; z++) {
            p.set(-5, LOFT + 1, z, z == -12 ? "air" : "spruce_fence");
            p.set(5, LOFT + 1, z, z == -12 ? "air" : "spruce_fence");
        }
        // Leitern hinauf (an Pfosten)
        for (int y = 1; y <= LOFT - 1; y++) {
            p.set(-5, y, -12, "spruce_log[axis=y]");
            p.set(5, y, -12, "spruce_log[axis=y]");
        }
        for (int y = 1; y <= LOFT + 1; y++) {
            p.set(-4, y, -12, "ladder[facing=east,waterlogged=false]");
            p.set(4, y, -12, "ladder[facing=west,waterlogged=false]");
        }
        // Heu auf den Böden (Haufen mit Gängen)
        for (int x = BX1 + 1; x < BX2; x++) {
            if (Math.abs(x) <= 5) {
                continue;
            }
            for (int z = BZ1 + 1; z < BZ2; z++) {
                if (Math.floorMod(z, 4) == 0 || Math.floorMod(x, 5) == 0 || z == -12 || z == -11 || z == -13) {
                    continue;
                }
                int h = 1 + Math.floorMod(x * 5 + z * 3, 3);
                for (int y = LOFT + 1; y <= LOFT + h; y++) {
                    p.set(x, y, z, "hay_block[axis=" + (Math.floorMod(x + z, 2) == 0 ? "x" : "y") + "]");
                }
            }
        }
        // einzelne Ballen auf den Böden (Verstecke)
        for (int[] b : new int[][] {{-8, -12}, {9, -11}, {-12, -16}, {11, -8}}) {
            p.set(b[0], LOFT + 1, b[1], "hay_block[axis=y]");
        }
        Decor.hangingLantern(p, -9, BARN_TOP, -12, 1);
        Decor.hangingLantern(p, 9, BARN_TOP, -12, 1);
        // Ställe im Westen: Boxen mit Zaun-Trennwänden, Heu, Tröge (Kessel), Komposter
        for (int z = BZ1 + 1; z < BZ2; z++) {
            p.set(-5, 1, z, (Math.floorMod(z, 5) == 0 || z == -12) ? "air" : "spruce_fence");
            if (Math.floorMod(z, 5) == 2) {
                for (int x = BX1 + 1; x <= -6; x++) {
                    p.set(x, 1, z, "spruce_fence");
                }
            }
        }
        for (int[] c : new int[][] {{-13, -20}, {-13, -15}, {-13, -10}, {-13, -5}}) {
            p.set(c[0], 1, c[1], "water_cauldron[level=2]");
        }
        for (int[] h : new int[][] {{-9, -20}, {-11, -16}, {-8, -14}, {-10, -10}, {-7, -6}, {-12, -4}}) {
            p.set(h[0], 1, h[1], "hay_block[axis=y]");
        }
        p.set(-7, 1, -19, "composter[level=3]");
        p.set(-9, 1, -4, LANTERN);
        // Lager im Osten: Fässer, Kürbisse, Melonen, Heu, Hackstöcke
        for (int z = BZ1 + 1; z < BZ2; z++) {
            p.set(13, 1, z, Math.floorMod(z, 3) == 0 ? "melon" : Math.floorMod(z, 3) == 1 ? BARREL_UP : "hay_block[axis=y]");
            if (Math.floorMod(z, 2) == 0) {
                p.set(13, 2, z, "hay_block[axis=x]");
            }
        }
        for (int[] b : new int[][] {{8, -19}, {10, -15}, {7, -8}, {11, -5}}) {
            p.set(b[0], 1, b[1], BARREL_UP);
        }
        p.set(9, 1, -18, "pumpkin");
        p.set(8, 1, -4, "carved_pumpkin[facing=west]");
        p.set(10, 1, -10, "melon");
        p.set(6, 1, -15, STUMP);
        p.set(6, 1, -7, LANTERN);
        // Tenne: Dreschflegel-Ecke (Fässer, ein Ballen)
        p.set(-2, 1, -20, "hay_block[axis=x]");
        p.set(2, 1, -20, BARREL_UP);
        Decor.hangingLantern(p, 0, BARN_TOP, -8, 1);
        Decor.hangingLantern(p, 0, BARN_TOP, -16, 1);
        p.set(-5, 3, BZ2 + 1, "lantern[hanging=false,waterlogged=false]");
    }

    /** Bauernhaus im Osten (Tür nach Westen zum Hof). */
    private static void farmhouse(Plan p, Random r) {
        Kit.House h = new Kit.House(17, -22, 28, -8, 'w', -15, "white_terracotta", "spruce", true, true,
                "spruce_planks");
        Kit.house(p, h, r);
        Kit.roundTable(p, 22, 1, -15, "spruce", LANTERN);
        p.set(27, 1, -21, "furnace[facing=west,lit=false]");
        p.set(26, 1, -21, "water_cauldron[level=3]");
        p.set(18, 1, -21, BARREL_UP);
        p.set(27, 1, -9, BARREL_UP);
        p.set(18, 1, -9, "potted_red_tulip");
        p.set(25, 1, -12, "bookshelf");
        Decor.hangingLantern(p, 22, Kit.GROUND_TOP, -18, 0);
        // Bank vor dem Haus
        for (int z = -19; z <= -17; z++) {
            p.set(16, 1, z, "spruce_stairs[facing=east,half=bottom,shape=straight]");
        }
        p.set(16, 1, -12, LANTERN);
    }

    /** Gemüsegarten: Reihen mit Ranken, Kürbissen und Melonen, Wege dazwischen, Vogelscheuche. */
    private static void garden(Plan p, Random r) {
        int x1 = 5;
        int x2 = 27;
        int z1 = 9;
        int z2 = 25;
        for (int x = x1; x <= x2; x++) {
            for (int z = z1; z <= z2; z++) {
                boolean edge = x == x1 || x == x2 || z == z1 || z == z2;
                if (edge) {
                    p.set(x, 1, z, x == x1 && z == 17 ? "oak_fence_gate[facing=west,open=true,in_wall=false]" : "oak_fence");
                    continue;
                }
                int row = Math.floorMod(z - z1, 4);
                if (row == 1) {
                    p.set(x, 0, z, "farmland[moisture=7]");
                    p.set(x, 1, z, Math.floorMod(x, 2) == 0 ? "pumpkin_stem[age=7]" : "melon_stem[age=7]");
                } else if (row == 2) {
                    p.set(x, 0, z, "coarse_dirt");
                    int pick = Math.floorMod(x * 7 + z, 5);
                    if (pick == 0 || pick == 3) {
                        p.set(x, 1, z, Math.floorMod(x, 2) == 0 ? "pumpkin" : "melon");
                    }
                } else {
                    p.set(x, 0, z, "dirt_path");
                }
            }
        }
        // Vogelscheuche
        p.set(16, 1, 17, "spruce_fence");
        p.set(16, 2, 17, "hay_block[axis=y]");
        p.set(15, 2, 17, "spruce_fence");
        p.set(17, 2, 17, "spruce_fence");
        p.set(16, 3, 17, "carved_pumpkin[facing=south]");
        // Ecke mit Gerät
        p.set(26, 1, 24, "composter[level=6]");
        p.set(25, 1, 24, BARREL_UP);
        p.set(26, 1, 23, "water_cauldron[level=1]");
        p.set(6, 1, 24, LANTERN);
        p.set(6, 1, 10, "pumpkin");
        p.set(26, 1, 10, "melon");
    }

    /** Bienenwiese: Blumen, Bienenstöcke auf Hackstöcken, ein offenes Bienenhaus. */
    private static void bees(Plan p, Random r) {
        for (int x = -28; x <= -6; x++) {
            for (int z = 9; z <= 26; z++) {
                if (p.get(x, 1, z) == null && r.nextInt(3) == 0) {
                    p.set(x, 1, z, Decor.pick(r, "dandelion", "poppy", "cornflower", "oxeye_daisy", "azure_bluet",
                            "allium", "short_grass", "blue_orchid"));
                }
            }
        }
        int[][] hives = {{-24, 11}, {-19, 11}, {-14, 11}, {-9, 12}, {-26, 15}, {-12, 16}, {-8, 20}, {-16, 21},
            {-23, 24}};
        for (int[] h : hives) {
            p.set(h[0], 1, h[1], STUMP);
            p.set(h[0], 2, h[1], "bee_nest[facing=" + (h[1] % 2 == 0 ? "south" : "east") + ",honey_level=0]");
        }
        for (int[] h : new int[][] {{-21, 14}, {-17, 18}, {-11, 24}, {-27, 21}, {-7, 15}}) {
            p.set(h[0], 1, h[1], "beehive[facing=south,honey_level=0]");
        }
        // offenes Bienenhaus: zwei Regale mit Stöcken unter einem Pultdach
        int bx1 = -24;
        int bx2 = -18;
        int bz = 19;
        for (int x = bx1; x <= bx2; x++) {
            p.set(x, 1, bz, "spruce_planks");
            p.set(x, 2, bz, "bee_nest[facing=south,honey_level=0]");
            p.set(x, 3, bz, "spruce_slab[type=bottom]");
            p.set(x, 4, bz, x % 2 == 0 ? "bee_nest[facing=south,honey_level=0]" : "beehive[facing=south,honey_level=0]");
            p.set(x, 5, bz, "spruce_planks");
            p.set(x, 5, bz + 1, "spruce_slab[type=bottom]");
            p.set(x, 6, bz - 1, "spruce_slab[type=bottom]");
        }
        for (int y = 1; y <= 5; y++) {
            p.set(bx1 - 1, y, bz, "spruce_log[axis=y]");
            p.set(bx2 + 1, y, bz, "spruce_log[axis=y]");
        }
        p.set(-21, 1, 21, LANTERN);
        p.set(-15, 1, 13, STUMP);
        p.set(-25, 1, 17, STUMP);
    }

    /** Kompostecke im Westen: Komposter-Reihe, Erdhaufen, Holzstoß, Hackstöcke, Fässer. */
    private static void compost(Plan p, Random r) {
        for (int x = -28; x <= -17; x++) {
            for (int z = -10; z <= 4; z++) {
                p.set(x, 0, z, Decor.pick(r, "coarse_dirt", "podzol", "dirt", "rooted_dirt"));
            }
        }
        for (int z = -9; z <= 3; z += 2) {
            p.set(-27, 1, z, "composter[level=" + Math.floorMod(z, 8) + "]");
        }
        for (int[] c : new int[][] {{-22, -6}, {-19, 1}, {-24, 3}}) {
            p.set(c[0], 1, c[1], "composter[level=7]");
        }
        Kit.woodpile(p, -27, 1, -10, 1, 2, true);
        Kit.woodpile(p, -26, 1, -10, 8, 2, true);
        for (int[] s : new int[][] {{-20, -8}, {-23, -2}, {-18, -4}, {-25, 0}}) {
            p.set(s[0], 1, s[1], STUMP);
        }
        p.set(-21, 1, -1, BARREL_UP);
        p.set(-18, 1, -9, BARREL_UP);
        p.set(-24, 1, -5, "water_cauldron[level=1]");
        p.set(-17, 1, 4, LANTERN);
    }

    /** Hühnerstall im Nordwesten mit Auslauf. */
    private static void coop(Plan p, Random r) {
        int x1 = -28;
        int x2 = -20;
        int z1 = -28;
        int z2 = -22;
        for (int x = x1; x <= x2; x++) {
            for (int z = z1; z <= z2; z++) {
                boolean edge = x == x1 || x == x2 || z == z1 || z == z2;
                p.set(x, 0, z, edge ? "cobblestone" : "spruce_planks");
                for (int y = 1; y <= 3; y++) {
                    if (edge) {
                        boolean corner = (x == x1 || x == x2) && (z == z1 || z == z2);
                        p.set(x, y, z, corner ? "spruce_log[axis=y]" : "spruce_planks");
                    } else {
                        p.clear(x, y, z);
                    }
                }
            }
        }
        p.clear(-24, 1, z2, -23, 2, z2);
        Kit.roof(p, x1, z1, x2, z2, 4, "spruce", false);
        for (int x = x1 + 1; x < x2; x++) {
            p.set(x, 1, z1 + 1, "hay_block[axis=x]");
        }
        p.set(-22, 1, -25, "hay_block[axis=y]");
        p.set(-27, 1, -24, "composter[level=2]");
        p.set(-26, 1, -23, LANTERN);
        // Auslauf
        for (int x = x1; x <= x2; x++) {
            p.set(x, 1, -16, "oak_fence");
        }
        for (int z = -21; z <= -16; z++) {
            p.set(x1, 1, z, "oak_fence");
            p.set(x2, 1, z, z == -18 ? "oak_fence_gate[facing=east,open=true,in_wall=false]" : "oak_fence");
        }
        p.set(-22, 1, -19, "water_cauldron[level=2]");
    }

    /** Obstwiese hinter dem Stadel. */
    private static void orchard(Plan p, Random r) {
        for (int x = -16; x <= 14; x += 6) {
            Decor.fruitTree(p, x, 1, -27, r);
        }
        p.set(-3, 1, -24, BARREL_UP);
        p.set(5, 1, -25, "hay_block[axis=y]");
        p.set(-10, 1, -24, "beehive[facing=south,honey_level=0]");
        p.set(11, 1, -24, "melon");
        p.set(-6, 1, -29, "pumpkin");
    }

    /** Holzlege im Nordosten. */
    private static void woodshed(Plan p, Random r) {
        int x1 = 17;
        int x2 = 28;
        int z1 = -29;
        int z2 = -25;
        for (int[] post : new int[][] {{x1, z1}, {x2, z1}, {x1, z2}, {x2, z2}}) {
            for (int y = 1; y <= 3; y++) {
                p.set(post[0], y, post[1], "spruce_log[axis=y]");
            }
        }
        for (int x = x1 - 1; x <= x2 + 1; x++) {
            for (int z = z1 - 1; z <= z2 + 1; z++) {
                p.set(x, 4, z, "spruce_slab[type=bottom]");
            }
        }
        Kit.woodpile(p, x1 + 1, 1, z1, x2 - x1 - 1, 3, true);
        for (int[] s : new int[][] {{19, -24}, {23, -23}, {27, -23}, {21, -27}, {25, -27}}) {
            p.set(s[0], 1, s[1], STUMP);
        }
        p.set(22, 1, -26, BARREL_UP);
        p.set(18, 1, -23, LANTERN);
    }

    /** Hoftor im Süden, wo die Gendarmen losgehen: Zaun mit Tor, Bank, Marterl. */
    private static void gate(Plan p, Random r) {
        for (int x = -HALF; x <= HALF; x++) {
            if (Math.abs(x) <= 2) {
                continue;
            }
            if (x >= 5 && x <= 27) {
                continue;
            }
            p.set(x, 1, 26, "spruce_fence");
        }
        for (int y = 1; y <= 3; y++) {
            p.set(-3, y, 26, "spruce_log[axis=y]");
            p.set(3, y, 26, "spruce_log[axis=y]");
        }
        for (int x = -3; x <= 3; x++) {
            p.set(x, 4, 26, "spruce_slab[type=bottom]");
        }
        p.set(-3, 3, 27, LANTERN);
        p.set(3, 3, 27, LANTERN);
        p.set(-5, 1, 28, "spruce_stairs[facing=west,half=bottom,shape=straight]");
        p.set(-5, 1, 29, "spruce_stairs[facing=west,half=bottom,shape=straight]");
        p.set(5, 1, 28, "pumpkin");
        p.set(6, 1, 29, "hay_block[axis=y]");
        for (int[] t : new int[][] {{-29, 29}, {29, 28}, {-29, -14}, {29, 2}, {-16, 27}}) {
            if (p.get(t[0], 1, t[1]) == null) {
                Decor.spruce(p, t[0], 1, t[1], 5, r);
            }
        }
    }
}
