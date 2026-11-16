package net.morni.versteckerl.game;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
import net.morni.versteckerl.rules.Helps;
import net.morni.versteckerl.rules.Settlement;
import org.bukkit.Location;

/**
 * Wer in einer Runde mitspielt: Rolle, Punkte und der Zustand als Versteckter bzw. Gendarm. {@code points} sind die in
 * der Runde gesammelten Punkte (Rangliste, beste Runde); davon geht ab, was man im Rundenladen ausgibt
 * ({@code spent}) – der Rest kommt am Ende aufs Guthaben.
 */
public final class Member {

    public enum Role {
        HIDER,
        SEEKER
    }

    final UUID uuid;
    final String name;
    final boolean startedHider;
    Role role;
    boolean online = true;
    int points;
    int finds;
    int hits;
    int whistles;
    boolean survived;
    /** Im Rundenladen ausgegeben. */
    int spent;
    int helpsBought;
    /** Abrechnung am Rundenende (für die Leiste, bis es zurück in die Gendarmerie geht) und Guthaben danach. */
    Settlement bill;
    long balanceAfter;
    final Map<Helps.Help, Integer> helpUses = new EnumMap<>(Helps.Help.class);
    /** Tick, ab dem eine Hilfe wieder geht. */
    final Map<Helps.Help, Long> helpReady = new EnumMap<>(Helps.Help.class);
    /** Zuletzt bewegt (Tick) – die Laterne der Gendarmen zeigt, wer sich gerade bewegt. */
    long lastMoveTick = Long.MIN_VALUE / 2;

    // ---------- als Versteckter ----------
    Disguise disguise;
    int lives;
    boolean changeUsed;
    /** Tick der Suchzeit beim letzten Pfiff, -1 = noch nie. */
    long lastWhistle = -1;
    long whistleReady;
    long jumpReady;
    long immuneUntil;
    long noLockUntil;
    long stillSince;
    long graceUntil;
    Location lastPos;
    boolean lockHint;
    boolean warned;

    // ---------- als Gendarm ----------
    boolean sniffUsed;
    UUID sniffTarget;
    long sniffUntil;
    long lastSwing = -100;
    long lastAttack = -100;
    long stunnedUntil;
    long lanternUntil;

    Member(UUID uuid, String name, Role role) {
        this.uuid = uuid;
        this.name = name;
        this.role = role;
        this.startedHider = role == Role.HIDER;
    }

    public UUID uuid() {
        return uuid;
    }

    public String name() {
        return name;
    }

    public Role role() {
        return role;
    }

    public boolean online() {
        return online;
    }

    public int points() {
        return points;
    }

    /** Punkte der Runde, die noch übrig sind (gesammelt minus im Rundenladen ausgegeben). */
    public int available() {
        return Math.max(0, points - spent);
    }

    /** Abrechnung dieser Runde, {@code null} solange die Runde läuft. */
    public Settlement bill() {
        return bill;
    }

    /** Guthaben nach der Abrechnung. */
    public long balanceAfter() {
        return balanceAfter;
    }

    public int spent() {
        return spent;
    }

    int uses(Helps.Help h) {
        return helpUses.getOrDefault(h, 0);
    }

    public int finds() {
        return finds;
    }

    public int whistles() {
        return whistles;
    }

    public int lives() {
        return lives;
    }

    public boolean hider() {
        return role == Role.HIDER;
    }

    /** Noch versteckt und da? */
    public boolean hidden() {
        return role == Role.HIDER && online;
    }

    public Disguise disguise() {
        return disguise;
    }
}
