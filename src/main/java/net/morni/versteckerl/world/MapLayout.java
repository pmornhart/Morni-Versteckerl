package net.morni.versteckerl.world;

/**
 * Wichtige Stellen einer Karte (relativ zum Mittelpunkt; y = 1 heißt: man steht auf der obersten Bodenschicht). Ohne
 * Bukkit – {@code MapPlanTest} prüft, dass alle Punkte frei und erreichbar sind.
 *
 * @param hiders   hier starten die Versteckten
 * @param seekers  hier werden die Dorfgendarmen nach der Versteckzeit losgelassen
 * @param booth    Gendarmenstube: hier warten die Gendarmen während der Versteckzeit (geschlossen, unter der Karte)
 * @param view     Blick für Zuschauer
 * @param half     halbe Breite der Spielfläche (Rand mit unsichtbarer Wand)
 * @param ceiling  Höhe der unsichtbaren Decke über dem Boden
 */
public record MapLayout(Point hiders, float hidersYaw, Point seekers, float seekersYaw, Point booth, Point view,
        int half, int ceiling) {

    /** Ein Punkt (Blockmitte mit .5). */
    public record Point(double x, double y, double z) {

        public int bx() {
            return (int) Math.floor(x);
        }

        public int by() {
            return (int) Math.floor(y);
        }

        public int bz() {
            return (int) Math.floor(z);
        }
    }

    /** Liegt ein Punkt (relativ) auf der Spielfläche? */
    public boolean inside(double x, double z) {
        return Math.abs(x) <= half + 0.5 && Math.abs(z) <= half + 0.5;
    }
}
