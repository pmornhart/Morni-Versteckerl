package de.moinmorni.versteckerl.listen;

import de.moinmorni.versteckerl.Items;
import de.moinmorni.versteckerl.Texts;
import de.moinmorni.versteckerl.Ui;
import de.moinmorni.versteckerl.VersteckerlPlugin;
import de.moinmorni.versteckerl.game.Game;
import de.moinmorni.versteckerl.hall.HallMenus;
import de.moinmorni.versteckerl.hall.Kraemer;
import de.moinmorni.versteckerl.hall.ShopMenus;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

/** Betreten, Verlassen und die Knöpfe in der Schnellleiste (Rechtsklick, geht auch auf Bedrock). */
public final class PlayerListener implements Listener {

    private final VersteckerlPlugin plugin;

    public PlayerListener(VersteckerlPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        e.joinMessage(null);
        plugin.data(p);
        plugin.hall().enter(p);
        plugin.games().applyVisibility(p);
        String lang = plugin.lang(p);
        p.sendMessage(Texts.msg(lang, "hall.welcome"));
        p.sendMessage(Texts.msg(lang, "hall.how"));
        if (plugin.shop().open()) {
            p.sendMessage(Texts.msg(lang, "hall.shop-hint"));
        }
        Ui.title(p, Texts.text(lang, "hall.title"), Texts.text(lang, "hall.subtitle"), 2500);
        // Spielt die Party schon? Dann gleich dazu – als Gendarm.
        UUID uuid = p.getUniqueId();
        plugin.net().party(uuid).thenAccept(party -> Bukkit.getScheduler().runTask(plugin, () -> {
            Player q = Bukkit.getPlayer(uuid);
            if (party.isEmpty() || q == null || !plugin.hall().inHall(uuid)) {
                return;
            }
            for (var m : party.get().members()) {
                Game mate = m.uuid().equals(uuid) ? null : plugin.games().playing(m.uuid());
                if (mate != null && plugin.hall().room(mate)) {
                    plugin.hall().leave(uuid);
                    q.sendMessage(Texts.msg(plugin.lang(q), "hall.party-joined"));
                    mate.joinLate(q);
                    return;
                }
            }
        }));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        e.quitMessage(null);
        plugin.hall().leave(p.getUniqueId());
        Game g = plugin.games().of(p.getUniqueId());
        if (g != null) {
            g.leave(p);
        }
        plugin.boards().sidebar().forget(p.getUniqueId());
        plugin.shop().forget(p.getUniqueId());
        plugin.store().unload(p.getUniqueId());
        plugin.forgetLanguage(p.getUniqueId());
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onInteract(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Action a = e.getAction();
        if (a != Action.RIGHT_CLICK_AIR && a != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Player p = e.getPlayer();
        ItemStack item = e.getItem();
        String hall = Items.hallTag(item);
        if (hall != null) {
            e.setCancelled(true);
            switch (hall) {
                case "vote" -> HallMenus.vote(plugin, p);
                case "ready" -> plugin.hall().toggleReady(p);
                case "stats" -> HallMenus.stats(plugin, p);
                case "shop" -> ShopMenus.open(plugin, p);
                case "lobby" -> {
                    p.sendMessage(Texts.msg(plugin.lang(p), "lobby.going"));
                    plugin.net().toLobby(p, Texts.text(plugin.lang(p), "lobby.kick"));
                }
                default -> {
                }
            }
            return;
        }
        String kit = Items.kitTag(item);
        if (kit == null) {
            return;
        }
        e.setCancelled(true);
        Game g = plugin.games().playing(p.getUniqueId());
        if (g == null) {
            return;
        }
        switch (kit) {
            case "shape" -> g.openShapes(p);
            case "whistle" -> g.whistle(p, false);
            case "jump" -> g.jump(p);
            case "sniff" -> g.sniff(p);
            case "helps" -> g.openHelps(p);
            case "stick" -> p.sendActionBar(Texts.text(plugin.lang(p), "item.stick.hint"));
            default -> {
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onEntity(PlayerInteractEntityEvent e) {
        if (e.getPlayer().getWorld() != plugin.world()) {
            return;
        }
        if (Kraemer.is(e.getRightClicked())) {
            e.setCancelled(true);
            if (e.getHand() == EquipmentSlot.HAND && plugin.games().of(e.getPlayer().getUniqueId()) == null) {
                ShopMenus.open(plugin, e.getPlayer());
            }
            return;
        }
        if (!plugin.builder(e.getPlayer())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent e) {
        if (e.getPlayer().getWorld() == plugin.world() && !plugin.builder(e.getPlayer())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onSwap(PlayerSwapHandItemsEvent e) {
        if (e.getPlayer().getWorld() == plugin.world() && !plugin.builder(e.getPlayer())) {
            e.setCancelled(true);
        }
    }

    /** Knöpfe bleiben, wo sie sind. */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onInventory(InventoryClickEvent e) {
        if (e.getWhoClicked() instanceof Player p && p.getWorld() == plugin.world() && !plugin.builder(p)
                && e.getClickedInventory() == p.getInventory()) {
            e.setCancelled(true);
        }
    }
}
