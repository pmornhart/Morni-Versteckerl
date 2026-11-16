package de.moinmorni.versteckerl.rules;

/**
 * Trifft ein Blick (Strahl ab dem Auge) einen Kasten? Gerechnet wie beim Zielen in Minecraft: der Strahl läuft vom
 * Auge in Blickrichtung, getroffen wird die erste Wand des Kastens. Ohne Bukkit.
 */
public final class Ray {

    /** Kein Treffer. */
    public static final double MISS = Double.POSITIVE_INFINITY;
    private static final double EPS = 1e-9;

    private Ray() {
    }

    /**
     * Abstand vom Auge {@code (ox, oy, oz)} in Richtung {@code (dx, dy, dz)} (Länge 1) bis zum Kasten
     * {@code [minX, maxX] × [minY, maxY] × [minZ, maxZ]}, oder {@link #MISS}, wenn er nicht innerhalb von
     * {@code reach} getroffen wird. Steht das Auge im Kasten, ist der Abstand 0.
     */
    public static double box(double ox, double oy, double oz, double dx, double dy, double dz,
            double minX, double minY, double minZ, double maxX, double maxY, double maxZ, double reach) {
        double near = 0;
        double far = reach;
        double[] o = {ox, oy, oz};
        double[] d = {dx, dy, dz};
        double[] lo = {minX, minY, minZ};
        double[] hi = {maxX, maxY, maxZ};
        for (int i = 0; i < 3; i++) {
            if (Math.abs(d[i]) < EPS) {
                if (o[i] < lo[i] || o[i] > hi[i]) {
                    return MISS;
                }
                continue;
            }
            double t1 = (lo[i] - o[i]) / d[i];
            double t2 = (hi[i] - o[i]) / d[i];
            if (t1 > t2) {
                double t = t1;
                t1 = t2;
                t2 = t;
            }
            near = Math.max(near, t1);
            far = Math.min(far, t2);
            if (near > far) {
                return MISS;
            }
        }
        return near;
    }

    /**
     * Kasten einer Gestalt, die mit ihrem Versteckten mitläuft: ein Block, unten mittig an den Füßen
     * ({@code fx, fy, fz}), etwas großzügiger ({@code grow}), weil der Server die Lage immer etwas später kennt.
     */
    public static double shape(double ox, double oy, double oz, double dx, double dy, double dz,
            double fx, double fy, double fz, double grow, double reach) {
        double h = 0.5 + grow;
        return box(ox, oy, oz, dx, dy, dz, fx - h, fy - grow, fz - h, fx + h, fy + 1 + grow, fz + h, reach);
    }

    /** Blickrichtung aus Gier- und Nickwinkel (Grad, wie Minecraft): {x, y, z}, Länge 1. */
    public static double[] direction(float yaw, float pitch) {
        double y = Math.toRadians(yaw);
        double p = Math.toRadians(pitch);
        double xz = Math.cos(p);
        return new double[] {-xz * Math.sin(y), -Math.sin(p), xz * Math.cos(y)};
    }
}
