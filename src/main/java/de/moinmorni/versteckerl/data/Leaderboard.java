package de.moinmorni.versteckerl.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * Die Steckbriefe der Gendarmerie: Bestenlisten in vier Sparten (beste Runde in Punkten, Punkte insgesamt, meiste
 * Funde, am öftesten bis zum Schluss versteckt). Je Name ein Eintrag (der beste Wert zählt), höher ist besser, bei
 * Gleichstand steht vorn, wer es zuerst geschafft hat. Gespeichert als {@code bestenliste.yml}.
 */
public final class Leaderboard {

    /** Die Sparten. */
    public enum Board {
        POINTS("punkte"),
        TOTAL("gesamt"),
        FOUND("funde"),
        SURVIVED("verstecke");

        private final String id;

        Board(String id) {
            this.id = id;
        }

        public String id() {
            return id;
        }
    }

    /** Ein Eintrag: Name, Wert, Zeitpunkt. */
    public record Entry(String name, long value, long when) {
    }

    public static final int SIZE = 10;

    private final Map<Board, List<Entry>> boards = new LinkedHashMap<>();

    /**
     * Wert eintragen (ersetzt den alten Eintrag dieses Namens, wenn er besser ist). Liefert den Platz (1 = ganz vorn)
     * oder 0, wenn es nicht für die Liste reicht.
     */
    public int put(Board board, String name, long value, long when) {
        if (value <= 0) {
            return 0;
        }
        List<Entry> list = boards.computeIfAbsent(board, b -> new ArrayList<>());
        for (int i = 0; i < list.size(); i++) {
            Entry e = list.get(i);
            if (e.name().equalsIgnoreCase(name)) {
                if (e.value() >= value) {
                    return i + 1;
                }
                list.remove(i);
                break;
            }
        }
        int pos = 0;
        while (pos < list.size() && list.get(pos).value() >= value) {
            pos++;
        }
        if (pos >= SIZE) {
            return 0;
        }
        list.add(pos, new Entry(name, value, when));
        while (list.size() > SIZE) {
            list.remove(list.size() - 1);
        }
        return pos + 1;
    }

    public List<Entry> top(Board board) {
        return List.copyOf(boards.getOrDefault(board, List.of()));
    }

    public String toYaml() {
        YamlConfiguration y = new YamlConfiguration();
        boards.forEach((board, list) -> {
            List<Map<String, Object>> rows = new ArrayList<>();
            for (Entry e : list) {
                Map<String, Object> row = new HashMap<>();
                row.put("name", e.name());
                row.put("wert", e.value());
                row.put("zeit", e.when());
                rows.add(row);
            }
            y.set(board.id(), rows);
        });
        return y.saveToString();
    }

    public static Leaderboard fromYaml(String text) {
        Leaderboard b = new Leaderboard();
        YamlConfiguration y = new YamlConfiguration();
        try {
            y.loadFromString(text);
        } catch (InvalidConfigurationException e) {
            throw new IllegalArgumentException(e);
        }
        for (Board board : Board.values()) {
            List<Entry> list = new ArrayList<>();
            for (Map<?, ?> row : y.getMapList(board.id())) {
                Object n = row.get("name");
                Object v = row.get("wert");
                Object t = row.get("zeit");
                if (n == null) {
                    continue;
                }
                list.add(new Entry(String.valueOf(n), v instanceof Number num ? num.longValue() : 0L,
                        t instanceof Number num ? num.longValue() : 0L));
            }
            list.sort((a, c) -> a.value() != c.value() ? Long.compare(c.value(), a.value()) : Long.compare(a.when(), c.when()));
            if (!list.isEmpty()) {
                b.boards.put(board, new ArrayList<>(list.subList(0, Math.min(SIZE, list.size()))));
            }
        }
        return b;
    }
}
