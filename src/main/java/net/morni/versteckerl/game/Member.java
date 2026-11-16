package net.morni.versteckerl.game;

import java.util.UUID;
import org.bukkit.Location;

/** Wer in einer Runde mitspielt: Rolle, Punkte und der Zustand als Versteckter bzw. Gendarm. */
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
