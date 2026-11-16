package de.moinmorni.versteckerl.game;

import de.moinmorni.versteckerl.VersteckerlPlugin;
import de.moinmorni.versteckerl.world.MapType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import org.bukkit.entity.Player;

/** Alle laufenden Runden (höchstens eine je Karte). */
public final class Games {

    private final VersteckerlPlugin plugin;
    private final Map<MapType, Game> running = new EnumMap<>(MapType.class);

    public Games(VersteckerlPlugin plugin) {
        this.plugin = plugin;
    }

    public Game start(MapType map, List<Player> players, Set<UUID> seekers) {
        Game g = new Game(plugin, map, players, seekers);
        running.put(map, g);
        plugin.stateChanged();
        return g;
    }

    public Collection<Game> all() {
        return running.values();
    }

    public Game on(MapType map) {
        return running.get(map);
    }

    /** Karten ohne laufende Runde. */
    public Set<MapType> free() {
        Set<MapType> out = EnumSet.allOf(MapType.class);
        out.removeAll(running.keySet());
        return out;
    }

    /** Runde, in der jemand mitspielt oder zuschaut. */
    public Game of(UUID uuid) {
        for (Game g : running.values()) {
            if (g.member(uuid) != null || g.spectating(uuid)) {
                return g;
            }
        }
        return null;
    }

    /** Runde, in der jemand mitspielt (nicht zuschauend). */
    public Game playing(UUID uuid) {
        for (Game g : running.values()) {
            if (g.member(uuid) != null) {
                return g;
            }
        }
        return null;
    }

    /** Neuer Spieler auf dem Server: Versteckte aller Runden für ihn unsichtbar machen. */
    public void applyVisibility(Player viewer) {
        for (Game g : running.values()) {
            g.applyVisibility(viewer);
        }
    }

    public void tick() {
        for (Game g : new ArrayList<>(running.values())) {
            boolean alive;
            try {
                alive = g.tick();
            } catch (RuntimeException e) {
                plugin.getLogger().log(Level.SEVERE, "Fehler in der Runde " + g.map().id() + " – sie wird beendet", e);
                g.close();
                alive = false;
            }
            if (!alive) {
                running.remove(g.map());
                plugin.stateChanged();
                plugin.boards().refreshHall();
            }
        }
    }

    public void abortAll() {
        for (Game g : new ArrayList<>(running.values())) {
            g.close();
        }
        running.clear();
    }

    public int playing() {
        return running.values().stream().mapToInt(Game::playerCount).sum();
    }
}
