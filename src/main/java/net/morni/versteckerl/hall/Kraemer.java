package net.morni.versteckerl.hall;

import net.morni.versteckerl.Holo;
import net.morni.versteckerl.Texts;
import net.morni.versteckerl.VersteckerlPlugin;
import net.morni.versteckerl.world.HallPlan;
import net.morni.versteckerl.world.MapType;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;

/**
 * Krämer Wastl hinter der Theke des Kramerstands: ein Dorfbewohner ohne eigenen Willen (bleibt stehen, nimmt keinen
 * Schaden, handelt nicht), der den Leuten in der Nähe nachschaut. Anklicken öffnet den Kramerladen. Darüber ein Schild
 * in beiden Sprachen. Nicht dauerhaft gespeichert – nach einem Neustart steht er neu da.
 */
public final class Kraemer {

    /** Markierung (zum Erkennen und Aufräumen). */
    public static final String TAG = "versteckerl_kraemer";
    private static final double LOOK_RANGE = 8;
    private static final double SIGN_HEIGHT = 2.35;

    private final VersteckerlPlugin plugin;
    private Villager villager;
    private Holo sign;

    public Kraemer(VersteckerlPlugin plugin) {
        this.plugin = plugin;
    }

    private Location home() {
        return new Location(plugin.world(), HallPlan.KRAEMER[0], MapType.FLOOR_Y + 1, HallPlan.KRAEMER[1],
                HallPlan.KRAEMER_YAW, 0f);
    }

    /** Aufstellen (oder neu, falls er fehlt). */
    public void spawn() {
        remove();
        for (Entity e : plugin.world().getEntities()) {
            if (e.getScoreboardTags().contains(TAG)) {
                e.remove();
            }
        }
        Location at = home();
        villager = plugin.world().spawn(at, Villager.class, v -> {
            v.setAI(false);
            v.setInvulnerable(true);
            v.setSilent(true);
            v.setPersistent(false);
            v.setCollidable(false);
            v.setGravity(false);
            v.setProfession(Villager.Profession.FARMER);
            v.setVillagerType(Villager.Type.TAIGA);
            v.setVillagerLevel(5);
            v.setRemoveWhenFarAway(false);
            v.addScoreboardTag(TAG);
        });
        sign = Holo.spawn(plugin, at.clone().add(0, SIGN_HEIGHT, 0), lang -> Texts.text(lang, "kraemer.sign"), 0.9f,
                180);
    }

    /** Jede Sekunde: fehlt er (z. B. weggeräumt), neu aufstellen; sonst dem nächsten Spieler nachschauen. */
    public void tick() {
        if (villager == null || !villager.isValid() || sign == null || !sign.valid()) {
            if (plugin.world().isChunkLoaded((int) Math.floor(HallPlan.KRAEMER[0]) >> 4,
                    (int) Math.floor(HallPlan.KRAEMER[1]) >> 4)) {
                spawn();
            }
            return;
        }
        Location here = villager.getLocation();
        Player nearest = null;
        double best = LOOK_RANGE * LOOK_RANGE;
        for (Player p : plugin.world().getPlayers()) {
            double d = p.getLocation().distanceSquared(here);
            if (d < best && plugin.hall().inHall(p.getUniqueId())) {
                best = d;
                nearest = p;
            }
        }
        float yaw = HallPlan.KRAEMER_YAW;
        float pitch = 0f;
        if (nearest != null) {
            Location eye = nearest.getEyeLocation();
            double dx = eye.getX() - here.getX();
            double dz = eye.getZ() - here.getZ();
            double dy = eye.getY() - (here.getY() + villager.getEyeHeight());
            yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
            pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.hypot(dx, dz)));
        }
        villager.setRotation(yaw, Math.max(-40f, Math.min(40f, pitch)));
        villager.setBodyYaw(yaw);
    }

    /** Ist das der Krämer? */
    public static boolean is(Entity e) {
        return e != null && e.getScoreboardTags().contains(TAG);
    }

    public void remove() {
        if (villager != null) {
            villager.remove();
            villager = null;
        }
        if (sign != null) {
            sign.remove();
            sign = null;
        }
    }
}
