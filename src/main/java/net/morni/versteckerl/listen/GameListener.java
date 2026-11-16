package net.morni.versteckerl.listen;

import net.morni.versteckerl.VersteckerlPlugin;
import net.morni.versteckerl.game.Game;
import net.morni.versteckerl.hall.Kraemer;
import net.morni.versteckerl.hall.ShopMenus;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerAnimationType;

/**
 * Schläge und Schaden: Ein Gendarm schlägt in die Luft, auf einen Block oder eine Gestalt – das Spiel rechnet selbst
 * nach, was getroffen wurde (Versteckte sind für ihn unsichtbar). Echten Schaden gibt es in der Versteckerl-Welt nie.
 */
public final class GameListener implements Listener {

    private final VersteckerlPlugin plugin;

    public GameListener(VersteckerlPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onSwing(PlayerAnimationEvent e) {
        if (e.getAnimationType() != PlayerAnimationType.ARM_SWING) {
            return;
        }
        Game g = plugin.games().playing(e.getPlayer().getUniqueId());
        if (g != null) {
            g.swing(e.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onHit(EntityDamageByEntityEvent e) {
        if (e.getEntity().getWorld() != plugin.world()) {
            return;
        }
        e.setCancelled(true);
        if (e.getDamager() instanceof Player p && Kraemer.is(e.getEntity())
                && plugin.games().of(p.getUniqueId()) == null) {
            // Auf Bedrock tippt man Figuren eher an (Schlag) – auch das öffnet den Laden.
            ShopMenus.open(plugin, p);
            return;
        }
        if (e.getDamager() instanceof Player seeker && e.getEntity() instanceof Player hider) {
            Game g = plugin.games().playing(seeker.getUniqueId());
            if (g != null && g == plugin.games().playing(hider.getUniqueId())) {
                g.attack(seeker, hider);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent e) {
        if (e.getEntity() instanceof Player && e.getEntity().getWorld() == plugin.world()) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onFood(FoodLevelChangeEvent e) {
        if (e.getEntity().getWorld() == plugin.world()) {
            e.setCancelled(true);
        }
    }
}
