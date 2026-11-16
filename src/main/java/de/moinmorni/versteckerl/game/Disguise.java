package de.moinmorni.versteckerl.game;

import de.moinmorni.versteckerl.rules.Shape;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

/**
 * Die Verwandlung eines Versteckten: eine Block-Anzeige, die ihm folgt (der Spieler selbst ist für die anderen
 * unsichtbar). Steht er lange genug still, „rastet er ein“: dann steht dort ein echter, fester Block, und die Anzeige
 * sieht nur noch er selbst. Bewegt er sich, springt der Block wieder auf und alles ist wie vorher.
 */
public final class Disguise {

    /** Markierung aller Gestalt-Anzeigen (für Aufräumen und Befehle). */
    public static final String TAG = "versteckerl_gestalt";
    private static final Map<String, BlockData> DATA = new HashMap<>();
    private static final int TELEPORT_TICKS = 2;

    private final Plugin plugin;
    private final Player owner;
    private Shape shape;
    private BlockDisplay display;
    private boolean solid;
    private Block cell;
    private BlockData before;
    private Location center;

    private Disguise(Plugin plugin, Player owner, Shape shape) {
        this.plugin = plugin;
        this.owner = owner;
        this.shape = shape;
    }

    /** Blockzustand einer Gestalt (zwischengespeichert, nicht verändern). */
    public static BlockData data(Shape s) {
        return DATA.computeIfAbsent(s.block(), b -> Bukkit.createBlockData("minecraft:" + b));
    }

    static Disguise create(Plugin plugin, Player owner, Shape shape) {
        Disguise d = new Disguise(plugin, owner, shape);
        d.spawn(owner.getLocation());
        return d;
    }

    private void spawn(Location at) {
        Location l = flat(at);
        display = l.getWorld().spawn(l, BlockDisplay.class, b -> {
            b.setBlock(data(shape));
            b.setPersistent(false);
            b.addScoreboardTag(TAG);
            b.setTeleportDuration(TELEPORT_TICKS);
            b.setTransformation(new Transformation(new Vector3f(-0.5f, 0f, -0.5f), new AxisAngle4f(),
                    new Vector3f(1f, 1f, 1f), new AxisAngle4f()));
        });
    }

    private static Location flat(Location at) {
        Location l = at.clone();
        l.setYaw(0f);
        l.setPitch(0f);
        return l;
    }

    public Shape shape() {
        return shape;
    }

    public boolean solid() {
        return solid;
    }

    /** Der echte Block, solange eingerastet (sonst null). */
    public Block cell() {
        return cell;
    }

    public Location center() {
        return center;
    }

    public BlockDisplay display() {
        return display;
    }

    /** Neue Gestalt (auch eingerastet: der Block wechselt mit). */
    void shape(Shape s) {
        this.shape = s;
        if (display != null && display.isValid()) {
            display.setBlock(data(s));
        }
        if (solid && cell != null) {
            cell.setBlockData(data(s), false);
            hideCellFromOwner();
        }
    }

    /** Der Anzeige folgen lassen (jeden Tick, solange nicht eingerastet). */
    void follow(Location feet) {
        if (display == null || !display.isValid()) {
            spawn(feet);
            return;
        }
        display.teleport(flat(feet));
    }

    /** Einrasten: echter Block in der Zelle, Anzeige nur noch für den Besitzer. */
    void solidify(Block at) {
        this.cell = at;
        this.before = at.getBlockData();
        this.solid = true;
        at.setBlockData(data(shape), false);
        center = at.getLocation().add(0.5, 0, 0.5);
        if (display != null && display.isValid()) {
            display.teleport(flat(center));
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p != owner) {
                    p.hideEntity(plugin, display);
                }
            }
        }
        hideCellFromOwner();
    }

    /**
     * Der Besitzer sieht an seiner Stelle Luft (sonst schöbe ihn sein eigenes Spiel aus dem Block). Der Server schickt
     * den echten Block erst am Ende des Ticks – darum gleich und noch zweimal kurz danach.
     */
    void hideCellFromOwner() {
        Block c = cell;
        if (c == null) {
            return;
        }
        BlockData air = Material.AIR.createBlockData();
        owner.sendBlockChange(c.getLocation(), air);
        for (long delay : new long[] {1L, 3L}) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (solid && cell != null && cell.equals(c) && owner.isOnline()) {
                    owner.sendBlockChange(c.getLocation(), air);
                }
            }, delay);
        }
    }

    /** Der Block springt auf: Zelle wie vorher, Anzeige wieder für alle (die sie sehen dürfen). */
    void release() {
        if (!solid) {
            return;
        }
        solid = false;
        if (cell != null) {
            cell.setBlockData(before, false);
        }
        cell = null;
        before = null;
        if (display != null && display.isValid()) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.showEntity(plugin, display);
            }
        }
    }

    /** Am Ende: leuchten lassen, damit alle sehen, wo sich jemand versteckt hat. */
    void glow() {
        if (display != null && display.isValid()) {
            display.setGlowing(true);
        }
    }

    /** Alles weg (Block zurück, Anzeige entfernen). */
    void remove() {
        release();
        if (display != null) {
            display.remove();
            display = null;
        }
    }
}
