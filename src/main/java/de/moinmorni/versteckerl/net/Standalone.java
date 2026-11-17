package de.moinmorni.versteckerl.net;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Versteckerl ohne Netzwerk: Sprache aus dem Spiel des Spielers (englischer Client → Englisch, sonst Deutsch), keine
 * Party, keine Statistik nach außen (lokal zählt Versteckerl trotzdem).
 */
final class Standalone implements Network {

    @Override
    public boolean connected() {
        return false;
    }

    @Override
    public String language(UUID player) {
        Player p = Bukkit.getPlayer(player);
        if (p == null) {
            return "de";
        }
        String lang = p.locale().getLanguage();
        return "en".equals(lang) ? "en" : "de";
    }

    @Override
    public void stats(UUID player, Map<String, Long> add, Map<String, Long> best) {
        // ohne Netzwerk nur lokal
    }

    @Override
    public void state(String state, int players) {
        // niemand fragt
    }

    @Override
    public void toLobby(Player player, Component message) {
        player.kick(message);
    }

    @Override
    public void askToLobby(Player player, Component message) {
        player.kick(message);
    }

    @Override
    public void playing(Player player, boolean playing) {
        // niemand fragt nach
    }

    @Override
    public void holdSlot(UUID player, int seconds) {
        // ohne Netzwerk verbindet man sich direkt wieder mit diesem Server
    }

    @Override
    public CompletableFuture<Optional<Party>> party(UUID player) {
        return CompletableFuture.completedFuture(Optional.empty());
    }

    @Override
    public String serverName() {
        return "";
    }
}
