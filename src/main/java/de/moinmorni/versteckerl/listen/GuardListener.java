package de.moinmorni.versteckerl.listen;

import de.moinmorni.versteckerl.VersteckerlPlugin;
import java.util.Set;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockFormEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.weather.WeatherChangeEvent;

/**
 * Schutz der eigenen Welt: niemand baut von Hand (nur Gestalten rasten als Block ein), nichts brennt, wächst oder
 * verfällt, keine fremden Tiere oder Monster, kein Regen. Ausnahme: Admins im Kreativmodus.
 */
public final class GuardListener implements Listener {

    private static final Set<CreatureSpawnEvent.SpawnReason> ALLOWED = Set.of(CreatureSpawnEvent.SpawnReason.CUSTOM,
            CreatureSpawnEvent.SpawnReason.COMMAND, CreatureSpawnEvent.SpawnReason.SPAWNER_EGG,
            CreatureSpawnEvent.SpawnReason.DEFAULT);

    private final VersteckerlPlugin plugin;

    public GuardListener(VersteckerlPlugin plugin) {
        this.plugin = plugin;
    }

    private boolean ours(World w) {
        return w == plugin.world();
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e) {
        if (ours(e.getBlock().getWorld()) && !plugin.builder(e.getPlayer())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent e) {
        if (ours(e.getBlock().getWorld()) && !plugin.builder(e.getPlayer())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        if (!ours(p.getWorld()) || plugin.builder(p)) {
            return;
        }
        if (e.getAction() == Action.PHYSICAL) {
            e.setCancelled(true);
        } else if (e.getAction() == Action.RIGHT_CLICK_BLOCK) {
            e.setUseInteractedBlock(Event.Result.DENY);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBucket(PlayerBucketEmptyEvent e) {
        if (ours(e.getBlock().getWorld()) && !plugin.builder(e.getPlayer())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBucket(PlayerBucketFillEvent e) {
        if (ours(e.getBlock().getWorld()) && !plugin.builder(e.getPlayer())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onIgnite(BlockIgniteEvent e) {
        if (ours(e.getBlock().getWorld()) && e.getCause() != BlockIgniteEvent.IgniteCause.FLINT_AND_STEEL) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBurn(BlockBurnEvent e) {
        if (ours(e.getBlock().getWorld())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onSpread(BlockSpreadEvent e) {
        if (ours(e.getBlock().getWorld())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onFade(BlockFadeEvent e) {
        if (ours(e.getBlock().getWorld())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onForm(BlockFormEvent e) {
        if (ours(e.getBlock().getWorld())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onFlow(BlockFromToEvent e) {
        if (ours(e.getBlock().getWorld())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onDecay(LeavesDecayEvent e) {
        if (ours(e.getBlock().getWorld())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onExplode(EntityExplodeEvent e) {
        if (ours(e.getEntity().getWorld())) {
            e.blockList().clear();
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onExplode(BlockExplodeEvent e) {
        if (ours(e.getBlock().getWorld())) {
            e.blockList().clear();
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onChange(EntityChangeBlockEvent e) {
        if (ours(e.getBlock().getWorld()) && !(e.getEntity() instanceof Player p && plugin.builder(p))) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onHanging(HangingBreakEvent e) {
        if (ours(e.getEntity().getWorld())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onSpawn(CreatureSpawnEvent e) {
        if (ours(e.getLocation().getWorld()) && !ALLOWED.contains(e.getSpawnReason())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onWeather(WeatherChangeEvent e) {
        if (ours(e.getWorld()) && e.toWeatherState()) {
            e.setCancelled(true);
        }
    }
}
