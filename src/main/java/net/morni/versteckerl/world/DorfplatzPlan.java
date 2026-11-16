package net.morni.versteckerl.world;

import java.util.Random;

/**
 * „Dorfplatz“: ein Bauerndorf auf der Alm. In der Mitte der Dorfbrunnen und der weiß-blaue Maibaum, im Norden das
 * Wirtshaus mit Biergarten, im Osten der Stadel voller Heu, im Westen der Kramerladen, im Süden die Schmiede; dazu
 * Obstwiese mit Bienenstöcken, Holzlege, Gärtchen und Heumandl. Überall stehen echte Attrappen der Gestalten herum.
 * Eigener Entwurf.
 */
final class DorfplatzPlan {

    private static final int HALF = 30;
    private static final int PLAZA = 9;

    private DorfplatzPlan() {
    }

    static Plan build(MapLayout layout) {
        Plan p = new Plan();
        Random r = new Random(1902);
        Decor.island(p, 44, 44, 24, r);
        mountains(p, r);
        roads(p, r);
        plaza(p);
        fountain(p);
        maypole(p);
        inn(p, r);
        barn(p, r);
        shop(p, r);
        smithy(p, r);
        orchard(p, r);
        woodshed(p, r);
        garden(p, r);
        hayfield(p, r);
        southGate(p, r);
        trees(p, r);
        Decor.greenery(p, -HALF, 22, -8, HALF, 5, r, DorfplatzPlan::road);
        Decor.greenery(p, 18, 10, HALF, HALF, 4, r, DorfplatzPlan::road);
        Decor.greenery(p, -HALF, -12, -14, -10, 2, r, DorfplatzPlan::road);
        booth(p, layout);
        Decor.flowers(p, -HALF, -HALF, HALF, HALF, 5, r, DorfplatzPlan::road);
        shopDecoys(p);
        Decor.barrierBox(p, HALF, layout.ceiling());
        p.connect();
        return p;
    }

    /** Wege bleiben frei von Blumen. */
    private static boolean road(int x, int z) {
        return (Math.abs(x) <= 1 && Math.abs(z) <= HALF) || (Math.abs(z) <= 1 && Math.abs(x) <= HALF)
                || Math.hypot(x, z) <= PLAZA + 1;
    }

    private static void mountains(Plan p, Random r) {
        int[][] peaks = {{-38, -38, 15, 28}, {0, -42, 13, 32}, {38, -37, 15, 26}, {-42, 2, 11, 20}, {42, 6, 11, 22},
            {-30, 40, 10, 16}, {34, 40, 10, 15}};
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

    /** Feldwege: ein Kreuz durchs Dorf und Abzweige zu jedem Hof. */
    private static void roads(Plan p, Random r) {
        for (int i = -HALF; i <= HALF; i++) {
            for (int w = -1; w <= 1; w++) {
                p.set(w, 0, i, Decor.pick(r, "dirt_path", "dirt_path", "dirt_path", "coarse_dirt"));
                p.set(i, 0, w, Decor.pick(r, "dirt_path", "dirt_path", "dirt_path", "coarse_dirt"));
            }
        }
    }

    private static void plaza(Plan p) {
        for (int x = -PLAZA; x <= PLAZA; x++) {
            for (int z = -PLAZA; z <= PLAZA; z++) {
                double d = Math.hypot(x, z);
                if (d <= PLAZA + 0.4) {
                    p.set(x, 0, z, d > PLAZA - 0.8 ? "polished_andesite"
                            : Decor.lozenge(x, z, 2) ? "stone_bricks" : "polished_diorite");
                }
            }
        }
        // Bänke am Platzrand mit Laternen daneben (Attrappen in Reichweite)
        int[][] benches = {{5, 6, 'n'}, {-5, 6, 'n'}, {6, -5, 's'}, {-6, 4, 'e'}};
        for (int[] b : benches) {
            char face = (char) b[2];
            boolean alongX = face == 'n' || face == 's';
            // Die Lehne zeigt vom Brunnen weg: wer nach Norden schaut, hat die Lehne im Süden.
            char back = face == 'n' ? 's' : face == 's' ? 'n' : face == 'e' ? 'w' : 'e';
            for (int i = -1; i <= 1; i++) {
                p.set(alongX ? b[0] + i : b[0], 1, alongX ? b[1] : b[1] + i,
                        "spruce_stairs[facing=" + Kit.facing(back) + ",half=bottom,shape=straight]");
            }
            p.set(alongX ? b[0] + 2 : b[0], 1, alongX ? b[1] : b[1] + 2, "lantern[hanging=false,waterlogged=false]");
        }
    }

    private static void fountain(Plan p) {
        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {
                double d = Math.hypot(x, z);
                if (d <= 2.3) {
                    p.set(x, 0, z, "water[level=0]");
                    p.set(x, -1, z, "stone_bricks");
                } else if (d <= 3.3) {
                    p.set(x, 0, z, "stone_bricks");
                    p.set(x, 1, z, (x + z) % 2 == 0 ? "stone_brick_slab[type=bottom]" : "polished_andesite_slab[type=bottom]");
                }
            }
        }
        // Säule mit Schale und Laterne
        p.set(0, 0, 0, "stone_bricks");
        p.set(0, 1, 0, "chiseled_stone_bricks");
        p.set(0, 2, 0, "stone_brick_wall");
        for (int[] d : new int[][] {{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
            p.set(d[0], 2, d[1], "stone_brick_slab[type=top]");
        }
        p.set(0, 3, 0, "polished_andesite");
        p.set(0, 4, 0, "lantern[hanging=false,waterlogged=false]");
        // Brunnenkübel daneben (Kessel-Attrappen)
        p.set(4, 1, 2, "water_cauldron[level=3]");
        p.set(-4, 1, -2, "cauldron");
    }

    /** Maibaum: weiß-blau gestreift, oben Kranz und Wipfel, seitlich Zunftzeichen-Fahnen. */
    private static void maypole(Plan p) {
        int x = -6;
        int z = -5;
        p.set(x, 0, z, "polished_andesite");
        for (int y = 1; y <= 17; y++) {
            p.set(x, y, z, (y / 2) % 2 == 0 ? "white_concrete" : "light_blue_concrete");
        }
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx != 0 || dz != 0) {
                    p.set(x + dx, 14, z + dz, "spruce_leaves[persistent=true]");
                }
            }
        }
        for (int y = 18; y <= 20; y++) {
            p.set(x, y, z, "spruce_leaves[persistent=true]");
        }
        p.set(x + 1, 19, z, "spruce_leaves[persistent=true]");
        p.set(x - 1, 19, z, "spruce_leaves[persistent=true]");
        p.set(x, 19, z + 1, "spruce_leaves[persistent=true]");
        p.set(x, 19, z - 1, "spruce_leaves[persistent=true]");
        // Arme mit Fahnen
        int[][] arms = {{1, 0, 8}, {-1, 0, 10}, {0, 1, 12}, {0, -1, 6}};
        for (int[] a : arms) {
            p.set(x + a[0], a[2], z + a[1], "spruce_fence");
            String face = a[0] == 1 ? "east" : a[0] == -1 ? "west" : a[1] == 1 ? "south" : "north";
            p.banner(x + a[0] * 2, a[2], z + a[1] * 2, "white_wall_banner[facing=" + face + "]", Decor.lozenges());
        }
    }

    /** Wirtshaus „Zum Morni“ im Norden, davor der Biergarten unter Kastanien. */
    private static void inn(Plan p, Random r) {
        Kit.House h = new Kit.House(-12, -29, 8, -19, 's', -2, "calcite", "dark_oak", true, true, "spruce_planks");
        Kit.house(p, h, r);
        // Gaststube: Schänke an der Nordwand, Fässer dahinter, Tische mit Laternen und Töpfen
        for (int x = -10; x <= 6; x++) {
            p.set(x, 1, -26, "dark_oak_slab[type=top]");
            if (x % 3 == 0) {
                p.set(x, 1, -28, "barrel[facing=up,open=false]");
                p.set(x, 2, -28, "barrel[facing=south,open=false]");
            }
        }
        p.set(-1, 2, -26, "lantern[hanging=false,waterlogged=false]");
        for (int x : new int[] {-8, -2, 4}) {
            Kit.roundTable(p, x, 1, -22, "dark_oak", x == -2 ? "potted_red_tulip" : "lantern[hanging=false,waterlogged=false]");
        }
        p.set(-11, 1, -20, "barrel[facing=up,open=false]");
        p.set(7, 1, -20, "barrel[facing=up,open=false]");
        Decor.hangingLantern(p, -5, 4, -24, 1);
        Decor.hangingLantern(p, 1, 4, -24, 1);
        // Biergarten
        for (int x = -12; x <= 8; x++) {
            for (int z = -18; z <= -12; z++) {
                if (Math.abs(x + 2) > 1) {
                    p.set(x, 0, z, Decor.pick(r, "gravel", "gravel", "coarse_dirt"));
                }
            }
        }
        Kit.longTable(p, -10, 1, -15, 5, true, "spruce", true, r, "potted_cornflower", "lantern[hanging=false,waterlogged=false]");
        Kit.longTable(p, 2, 1, -15, 5, true, "spruce", true, r, "potted_poppy", "lantern[hanging=false,waterlogged=false]");
        for (int[] b : new int[][] {{-12, -18}, {-12, -12}, {8, -18}, {8, -13}, {-4, -12}, {0, -18}}) {
            p.set(b[0], 1, b[1], "barrel[facing=up,open=false]");
        }
        Decor.broadTree(p, -12, 1, -16, "dark_oak_log", "dark_oak_leaves", 5, 3, r);
        Decor.broadTree(p, 8, 1, -16, "dark_oak_log", "dark_oak_leaves", 5, 3, r);
        Decor.fence(p, -13, -18, -13, -12, 1, "spruce_fence");
        Decor.fence(p, 9, -18, 9, -12, 1, "spruce_fence");
        Decor.lanternPost(p, -3, 1, -12, "dark_oak_fence");
        Decor.lanternPost(p, -1, 1, -12, "dark_oak_fence");
    }

    /** Stadel im Osten: Holzscheune voller Heu, Tor nach Westen, Heuwagen davor. */
    private static void barn(Plan p, Random r) {
        int x1 = 15;
        int x2 = 28;
        int z1 = -9;
        int z2 = 9;
        for (int x = x1; x <= x2; x++) {
            for (int z = z1; z <= z2; z++) {
                boolean edge = x == x1 || x == x2 || z == z1 || z == z2;
                p.set(x, 0, z, edge ? "cobblestone" : Decor.pick(r, "spruce_planks", "coarse_dirt", "spruce_planks"));
                for (int y = 1; y <= 5; y++) {
                    if (!edge) {
                        p.clear(x, y, z);
                        continue;
                    }
                    boolean corner = (x == x1 || x == x2) && (z == z1 || z == z2);
                    boolean post = corner || (z == z1 || z == z2) && (x - x1) % 4 == 0 || (x == x1 || x == x2) && (z - z1) % 4 == 0;
                    p.set(x, y, z, post ? "spruce_log[axis=y]" : y == 1 ? "cobblestone" : "spruce_planks");
                }
            }
        }
        // Tor (5 breit, 4 hoch) nach Westen
        p.clear(x1, 1, -2, x1, 4, 2);
        for (int z = -2; z <= 2; z++) {
            p.set(x1, 0, z, "spruce_planks");
        }
        // Lüftungsschlitze
        for (int z = z1 + 2; z <= z2 - 2; z += 4) {
            p.set(x2, 3, z, "spruce_fence");
        }
        Kit.roof(p, x1, z1, x2, z2, 6, "spruce", true);
        // Heu: Stapel an den Wänden (2–3 hoch), Gänge dazwischen zum Verstecken
        for (int z = z1 + 1; z <= z2 - 1; z++) {
            for (int x = x2 - 3; x <= x2 - 1; x++) {
                if (z % 5 == 0) {
                    continue;
                }
                int h = 1 + Math.floorMod(x * 7 + z * 3, 3);
                for (int y = 1; y <= h; y++) {
                    p.set(x, y, z, "hay_block[axis=" + ((x + z) % 2 == 0 ? "x" : "y") + "]");
                }
            }
        }
        for (int x = x1 + 3; x <= x2 - 5; x++) {
            p.set(x, 1, z1 + 1, "hay_block[axis=y]");
            p.set(x, 1, z2 - 1, "hay_block[axis=y]");
            if (x % 2 == 0) {
                p.set(x, 2, z1 + 1, "hay_block[axis=x]");
            }
        }
        // einzelne Ballen frei im Stadel (gute Verstecke)
        for (int[] b : new int[][] {{19, -4}, {21, 3}, {18, 5}, {22, -6}, {20, 0}}) {
            p.set(b[0], 1, b[1], "hay_block[axis=y]");
        }
        // Tränke (Kessel) und Laternen
        p.set(17, 1, -8, "water_cauldron[level=3]");
        p.set(17, 1, 8, "water_cauldron[level=2]");
        Decor.hangingLantern(p, 21, 5, -3, 1);
        Decor.hangingLantern(p, 21, 5, 3, 1);
        p.set(14, 1, -4, "lantern[hanging=false,waterlogged=false]");
        p.set(14, 1, 4, "lantern[hanging=false,waterlogged=false]");
        // Heuwagen
        for (int x = 9; x <= 12; x++) {
            for (int z = -8; z <= -6; z++) {
                p.set(x, 1, z, "spruce_slab[type=bottom]");
                p.set(x, 2, z, "hay_block[axis=x]");
            }
            p.set(x, 1, -9, "spruce_trapdoor[facing=north,half=bottom,open=true]");
            p.set(x, 1, -5, "spruce_trapdoor[facing=south,half=bottom,open=true]");
        }
        p.set(10, 3, -7, "hay_block[axis=z]");
        p.set(11, 3, -7, "hay_block[axis=z]");
        p.set(8, 1, -7, "spruce_fence");
        p.set(7, 1, -7, "spruce_fence");
        p.set(13, 1, 6, "anvil[facing=west]");
    }

    /** Kramerladen im Westen: Auslage mit Fässern, Kürbissen und Töpfen unter einer gestreiften Markise. */
    private static void shop(Plan p, Random r) {
        Kit.House h = new Kit.House(-27, -8, -15, 4, 'e', -2, "smooth_quartz", "spruce", true, false, "oak_planks");
        Kit.house(p, h, r);
        // Markise
        for (int z = -6; z <= 2; z++) {
            p.set(-14, 4, z, z % 2 == 0 ? "red_wool" : "white_wool");
            p.set(-13, 4, z, z % 2 == 0 ? "red_carpet" : "white_carpet");
        }
        // Auslage vor dem Laden (Attrappen in Reichweite)
        int[][] stand = {{-13, -6, 0}, {-13, -5, 1}, {-13, 1, 0}, {-13, 2, 2}, {-12, -6, 3}, {-12, 2, 1}};
        String[] goods = {"barrel[facing=up,open=false]", "carved_pumpkin[facing=east]", "pumpkin",
            "potted_azure_bluet"};
        for (int[] s : stand) {
            p.set(s[0], 1, s[1], goods[s[2]]);
        }
        p.set(-12, 1, -4, "barrel[facing=up,open=false]");
        p.set(-12, 2, -4, "potted_poppy");
        // Innen: Theke, Regale (Fässer), Kürbisse, Laternen
        for (int z = -6; z <= 2; z++) {
            p.set(-19, 1, z, z == -2 ? "spruce_fence_gate[facing=east,open=true,in_wall=false]" : "spruce_slab[type=top]");
        }
        p.set(-19, 2, 0, "lantern[hanging=false,waterlogged=false]");
        for (int z = -7; z <= 3; z++) {
            p.set(-26, 1, z, z % 3 == 0 ? "carved_pumpkin[facing=east]" : "barrel[facing=east,open=false]");
            p.set(-26, 2, z, "barrel[facing=east,open=false]");
            p.set(-26, 3, z, z % 2 == 0 ? "potted_cornflower" : "bookshelf");
        }
        p.set(-24, 1, -7, "barrel[facing=up,open=false]");
        p.set(-22, 1, 3, "pumpkin");
        p.set(-24, 1, 3, "barrel[facing=up,open=false]");
        Decor.hangingLantern(p, -21, 4, -2, 1);
    }

    /** Offene Schmiede im Süden: Ambosse, Esse, Löschtröge (Kessel), Hackstöcke. */
    private static void smithy(Plan p, Random r) {
        int x1 = 7;
        int x2 = 18;
        int z1 = 13;
        int z2 = 22;
        for (int x = x1; x <= x2; x++) {
            for (int z = z1; z <= z2; z++) {
                p.set(x, 0, z, Decor.pick(r, "stone", "andesite", "cobblestone", "gravel"));
            }
        }
        for (int[] post : new int[][] {{x1, z1}, {x2, z1}, {x1, z2}, {x2, z2}, {x1 + 5, z1}, {x1 + 5, z2}}) {
            for (int y = 1; y <= 4; y++) {
                p.set(post[0], y, post[1], "spruce_log[axis=y]");
            }
        }
        Kit.roof(p, x1, z1, x2, z2, 5, "spruce", false);
        // Esse aus Ziegeln an der Rückwand mit Glut
        for (int x = 14; x <= 16; x++) {
            p.set(x, 1, z2, "bricks");
            p.set(x, 2, z2, x == 15 ? "campfire[lit=true,signal_fire=false,facing=north]" : "bricks");
            p.set(x, 3, z2, "bricks");
        }
        for (int y = 4; y <= 10; y++) {
            p.set(15, y, z2, "bricks");
        }
        p.set(13, 1, z2, "blast_furnace[facing=north,lit=false]");
        p.set(12, 1, z2, "smithing_table");
        p.set(17, 1, z2, "grindstone[face=floor,facing=north]");
        // Ambosse, Kessel, Hackstöcke
        for (int[] a : new int[][] {{10, 16}, {14, 18}, {16, 15}, {9, 20}}) {
            p.set(a[0], 1, a[1], "anvil[facing=" + ((a[0] + a[1]) % 2 == 0 ? "north" : "east") + "]");
        }
        p.set(11, 1, 21, "water_cauldron[level=3]");
        p.set(17, 1, 19, "water_cauldron[level=2]");
        for (int[] s : new int[][] {{8, 14}, {12, 14}, {18, 17}, {6, 18}}) {
            p.set(s[0], 1, s[1], "stripped_spruce_log[axis=y]");
        }
        Kit.woodpile(p, 19, 1, 14, 6, 2, false);
        Decor.hangingLantern(p, 10, 4, 18, 1);
        Decor.hangingLantern(p, 15, 4, 16, 1);
        p.set(6, 1, 14, "lantern[hanging=false,waterlogged=false]");
    }

    /** Obstwiese im Nordwesten mit Bienenstöcken auf Hackstöcken. */
    private static void orchard(Plan p, Random r) {
        for (int x = -27; x <= -16; x += 5) {
            for (int z = -27; z <= -13; z += 6) {
                Decor.fruitTree(p, x, 1, z, r);
            }
        }
        int[][] hives = {{-25, -24}, {-20, -24}, {-25, -18}, {-20, -18}, {-17, -15}, {-23, -14}, {-28, -21},
            {-18, -27}};
        for (int[] h : hives) {
            p.set(h[0], 1, h[1], "stripped_spruce_log[axis=y]");
            p.set(h[0], 2, h[1], "bee_nest[facing=" + (h[1] % 2 == 0 ? "south" : "east") + ",honey_level=0]");
        }
        // Bienenstöcke am Boden (dort kann man sich auch verstecken)
        for (int[] h : new int[][] {{-22, -21}, {-27, -15}, {-16, -21}}) {
            p.set(h[0], 1, h[1], "bee_nest[facing=south,honey_level=0]");
        }
        Decor.fence(p, -29, -29, -14, -29, 1, "oak_fence");
        Decor.fence(p, -14, -29, -14, -12, 1, "oak_fence");
        p.set(-14, 1, -20, "oak_fence_gate[facing=east,open=true,in_wall=false]");
        for (int z = -28; z <= -12; z++) {
            for (int x = -28; x <= -15; x++) {
                if (r.nextInt(9) == 0 && p.get(x, 1, z) == null) {
                    p.set(x, 1, z, Decor.pick(r, "dandelion", "poppy", "oxeye_daisy", "cornflower", "azure_bluet"));
                }
            }
        }
    }

    /** Holzlege im Nordosten: offener Schuppen mit Holzstößen und Hackstöcken. */
    private static void woodshed(Plan p, Random r) {
        int x1 = 15;
        int x2 = 27;
        int z1 = -27;
        int z2 = -19;
        for (int x = x1; x <= x2; x++) {
            for (int z = z1; z <= z2; z++) {
                p.set(x, 0, z, Decor.pick(r, "coarse_dirt", "dirt", "podzol", "coarse_dirt"));
            }
        }
        for (int[] post : new int[][] {{x1, z1}, {x2, z1}, {x1, z2}, {x2, z2}, {21, z1}, {21, z2}}) {
            for (int y = 1; y <= 4; y++) {
                p.set(post[0], y, post[1], "spruce_log[axis=y]");
            }
        }
        Kit.roof(p, x1, z1, x2, z2, 5, "spruce", false);
        Kit.woodpile(p, 16, 1, -26, 11, 3, true);
        Kit.woodpile(p, 26, 1, -25, 5, 2, false);
        for (int[] s : new int[][] {{18, -22}, {22, -21}, {25, -20}, {17, -18}, {24, -17}, {20, -15}, {27, -14}}) {
            p.set(s[0], 1, s[1], "stripped_spruce_log[axis=y]");
        }
        p.set(19, 1, -20, "barrel[facing=up,open=false]");
        p.set(26, 1, -21, "barrel[facing=up,open=false]");
        p.set(23, 1, -23, "anvil[facing=north]");
        Decor.hangingLantern(p, 21, 4, -23, 1);
        p.set(15, 1, -17, "lantern[hanging=false,waterlogged=false]");
    }

    /** Gärtchen im Südwesten: Häusl, Gemüsebeet mit Kürbissen, Töpfe auf dem Zaun. */
    private static void garden(Plan p, Random r) {
        Kit.House h = new Kit.House(-27, 15, -17, 23, 'n', -22, "smooth_quartz", "spruce", false, false, "spruce_planks");
        Kit.house(p, h, r);
        p.set(-25, 1, 21, "barrel[facing=up,open=false]");
        p.set(-19, 1, 21, "water_cauldron[level=3]");
        p.set(-24, 1, 17, "lantern[hanging=false,waterlogged=false]");
        for (int x = -28; x <= -16; x++) {
            for (int z = 6; z <= 13; z++) {
                boolean edge = x == -28 || x == -16 || z == 6;
                if (edge) {
                    p.set(x, 1, z, x == -22 && z == 6 ? "spruce_fence_gate[facing=north,open=true,in_wall=false]" : "spruce_fence");
                    if (!(x == -22 && z == 6) && Math.floorMod(x, 3) == 0) {
                        p.set(x, 2, z, Decor.POTS[r.nextInt(Decor.POTS.length)]);
                    }
                } else if (z >= 8 && z <= 12 && x != -22) {
                    p.set(x, 0, z, "farmland[moisture=7]");
                    p.set(x, 1, z, (x + z) % 4 == 0 ? "pumpkin" : Decor.pick(r, "carrots[age=7]", "potatoes[age=7]",
                            "beetroots[age=3]"));
                }
            }
        }
        p.set(-22, 0, 7, "dirt_path");
        for (int z = 8; z <= 13; z++) {
            p.set(-22, 0, z, "dirt_path");
        }
        p.set(-24, 1, 7, "carved_pumpkin[facing=north]");
        p.set(-20, 1, 7, "bee_nest[facing=north,honey_level=0]");
        p.set(-18, 1, 7, "potted_red_tulip");
    }

    /** Heumandl auf der Wiese im Südosten. */
    private static void hayfield(Plan p, Random r) {
        int[][] stacks = {{22, 12}, {26, 16}, {22, 25}, {27, 23}, {19, 27}, {25, 28}};
        for (int[] s : stacks) {
            p.set(s[0], 1, s[1], "hay_block[axis=y]");
            if (r.nextBoolean()) {
                p.set(s[0], 2, s[1], "hay_block[axis=x]");
            }
        }
        for (int[] s : new int[][] {{20, 20}, {28, 13}, {24, 19}}) {
            p.set(s[0], 1, s[1], "hay_block[axis=z]");
        }
        p.set(-28, 1, 27, "hay_block[axis=y]");
        p.set(-26, 1, 28, "hay_block[axis=y]");
        p.set(-15, 1, 26, "hay_block[axis=x]");
    }

    /** Marterl (Wegkreuz) am Ortseingang im Süden, wo die Gendarmen losgehen. */
    private static void southGate(Plan p, Random r) {
        p.set(3, 0, 27, "stone_bricks");
        p.set(3, 1, 27, "stone_brick_wall");
        p.set(3, 2, 27, "stone_brick_wall");
        p.set(3, 3, 27, "chiseled_stone_bricks");
        p.set(3, 4, 27, "spruce_slab[type=bottom]");
        p.set(3, 3, 26, "lantern[hanging=false,waterlogged=false]");
        p.set(-3, 1, 26, "spruce_stairs[facing=west,half=bottom,shape=straight]");
        p.set(-3, 1, 27, "spruce_stairs[facing=west,half=bottom,shape=straight]");
        p.set(-3, 1, 25, "potted_poppy");
        p.set(4, 1, 24, "pumpkin");
        p.set(-5, 1, 22, "barrel[facing=up,open=false]");
        p.set(-6, 1, 22, "stripped_spruce_log[axis=y]");
    }

    /**
     * Attrappen der Gestalten aus dem Kramerladen: Werkbänke, Schleifsteine und Webstühle in Schmiede, Holzlege, Stadel,
     * Laden und Gärtchen, blühende Büsche auf den Wiesen, Kuchen auf Schänke, Biertischen und Ladentheke,
     * Kürbislaternen im Laden, am Gartenzaun und am Ortseingang.
     */
    private static void shopDecoys(Plan p) {
        Decor.decoys(p, "crafting_table", new int[] {8, 21}, new int[] {16, 21}, new int[] {17, -24},
                new int[] {24, -24}, new int[] {16, -6}, new int[] {16, 6}, new int[] {-19, 17});
        int[][] grind = {{13, 17}, {8, 19}, {21, -25}, {25, -22}, {24, -6}, {17, 4}};
        for (int i = 0; i < grind.length; i++) {
            Decor.decoy(p, grind[i][0], 1, grind[i][1], "grindstone[face=floor,facing=" + (i % 2 == 0 ? "north" : "east")
                    + "]");
        }
        Decor.decoys(p, "loom[facing=east]", new int[] {-22, -7}, new int[] {-21, 3});
        Decor.decoys(p, "loom[facing=west]", new int[] {-17, -7}, new int[] {-17, 3});
        Decor.decoys(p, "loom[facing=south]", new int[] {-26, 19}, new int[] {-21, 22}, new int[] {-18, 16});
        for (int[] b : new int[][] {{-12, 22}, {-6, 28}, {-18, 27}, {-28, 23}, {8, 28}, {24, 22}, {20, 24},
            {28, 19}, {-28, -11}, {-20, -11}, {-14, -13}, {-24, 13}}) {
            Decor.bushOnMeadow(p, b[0], b[1]);
        }
        // Kuchen auf der Schänke im Wirtshaus, auf den Biertischen und auf der Ladentheke
        for (int[] c : new int[][] {{-7, -26}, {-3, -26}, {3, -26}, {-8, -15}, {4, -15}, {-19, -5}, {-19, 2}}) {
            p.set(c[0], 2, c[1], "cake[bites=0]");
        }
        p.set(-26, 1, -6, "jack_o_lantern[facing=east]");
        p.set(-26, 1, 0, "jack_o_lantern[facing=east]");
        p.set(-12, 1, 2, "jack_o_lantern[facing=east]");
        p.set(-24, 1, 7, "jack_o_lantern[facing=north]");
        p.set(4, 1, 24, "jack_o_lantern[facing=south]");
        Decor.decoy(p, -28, 2, 6, "jack_o_lantern[facing=north]");
        Decor.decoy(p, -16, 2, 6, "jack_o_lantern[facing=north]");
        Decor.decoy(p, 14, 1, -6, "jack_o_lantern[facing=west]");
    }

    private static void trees(Plan p, Random r) {
        int[][] spots = {{-8, 14}, {-11, 24}, {6, 26}, {-4, 19}, {28, 10}, {12, 28}, {-29, 8}, {-13, -11},
            {12, -12}, {28, -12}, {-29, -10}, {-6, -16}};
        for (int[] s : spots) {
            if (p.get(s[0], 1, s[1]) == null && p.get(s[0], 0, s[1]) != null && !road(s[0], s[1])) {
                Decor.spruce(p, s[0], 1, s[1], 4 + r.nextInt(3), r);
            }
        }
    }

    /** Gendarmenstube unter dem Ortseingang: geschlossener Raum, hier warten die Gendarmen. */
    static void booth(Plan p, MapLayout layout) {
        int x = layout.booth().bx();
        int y = layout.booth().by();
        int z = layout.booth().bz();
        p.fill(x - 2, y - 1, z - 2, x + 2, y + 3, z + 2, "spruce_planks");
        p.clear(x - 1, y, z - 1, x + 1, y + 2, z + 1);
        p.fill(x - 1, y - 1, z - 1, x + 1, y - 1, z + 1, "dark_oak_planks");
        p.set(x + 1, y + 2, z + 1, "lantern[hanging=true,waterlogged=false]");
        p.set(x - 1, y, z + 1, "spruce_stairs[facing=south,half=bottom,shape=straight]");
    }
}
