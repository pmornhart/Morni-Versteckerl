package net.morni.versteckerl;

import net.morni.versteckerl.data.Leaderboard;
import net.morni.versteckerl.data.PlayerData;
import net.morni.versteckerl.game.Game;
import net.morni.versteckerl.hall.HallMenus;
import net.morni.versteckerl.hall.ShopMenus;
import net.morni.versteckerl.rules.Catalog;
import net.morni.versteckerl.world.MapType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

/** Alle Befehle. */
final class Commands implements TabExecutor {

    private static final String ADMIN = "morninet.versteckerl.admin";
    private final VersteckerlPlugin plugin;

    Commands(VersteckerlPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String name = command.getName().toLowerCase(Locale.ROOT);
        if (name.equals("versteckerladmin")) {
            return admin(sender, args);
        }
        if (name.equals("stats")) {
            stats(sender, args);
            return true;
        }
        if (name.equals("top")) {
            top(sender);
            return true;
        }
        if (!(sender instanceof Player p)) {
            sender.sendMessage(Texts.msg("de", "only-players"));
            return true;
        }
        String lang = plugin.lang(p);
        switch (name) {
            case "bereit" -> plugin.hall().toggleReady(p);
            case "karte" -> {
                MapType m = args.length > 0 ? MapType.byId(args[0]) : null;
                if (m == null || !plugin.hall().inHall(p.getUniqueId())) {
                    HallMenus.vote(plugin, p);
                } else {
                    plugin.hall().vote(p, m);
                }
            }
            case "gestalt" -> {
                Game g = plugin.games().playing(p.getUniqueId());
                if (g == null) {
                    p.sendMessage(Texts.msg(lang, "shape.only-hiders"));
                } else {
                    g.openShapes(p);
                }
            }
            case "zuschauen" -> spectate(p, args);
            case "laden" -> {
                Game g = plugin.games().playing(p.getUniqueId());
                if (g != null) {
                    g.openHelps(p);
                } else if (plugin.games().of(p.getUniqueId()) != null) {
                    p.sendMessage(Texts.msg(lang, "shop.not-now"));
                } else {
                    ShopMenus.open(plugin, p);
                }
            }
            case "verlassen" -> {
                Game g = plugin.games().of(p.getUniqueId());
                if (g == null) {
                    p.sendMessage(Texts.msg(lang, "leave.none"));
                } else {
                    g.leave(p);
                    plugin.hall().enter(p);
                    p.sendMessage(Texts.msg(lang, "leave.done"));
                }
            }
            default -> {
                return false;
            }
        }
        return true;
    }

    private void spectate(Player p, String[] args) {
        String lang = plugin.lang(p);
        Game g = null;
        if (args.length > 0) {
            MapType m = MapType.byId(args[0]);
            if (m != null) {
                g = plugin.games().on(m);
            } else {
                Player t = Bukkit.getPlayerExact(args[0]);
                if (t == null) {
                    p.sendMessage(Texts.msg(lang, "unknown-player", Placeholder.unparsed("name", args[0])));
                    return;
                }
                g = plugin.games().of(t.getUniqueId());
            }
        } else if (!plugin.games().all().isEmpty()) {
            g = plugin.games().all().iterator().next();
        }
        if (g == null) {
            p.sendMessage(Texts.msg(lang, "spectate.none"));
            return;
        }
        if (g.member(p.getUniqueId()) != null) {
            return;
        }
        Game before = plugin.games().of(p.getUniqueId());
        if (before != null && before != g) {
            before.leave(p);
        }
        plugin.spectate(p, g);
    }

    private void stats(CommandSender sender, String[] args) {
        String lang = sender instanceof Player p ? plugin.lang(p) : "de";
        Optional<PlayerData> data;
        String who;
        if (args.length > 0) {
            who = args[0];
            Player online = Bukkit.getPlayerExact(who);
            if (online != null) {
                data = Optional.of(plugin.data(online));
            } else {
                @SuppressWarnings("deprecation")
                OfflinePlayer off = Bukkit.getOfflinePlayer(who);
                data = plugin.store().read(off.getUniqueId());
            }
        } else if (sender instanceof Player p) {
            who = p.getName();
            data = Optional.of(plugin.data(p));
        } else {
            sender.sendMessage(Texts.msg(lang, "only-players"));
            return;
        }
        if (data.isEmpty() || data.get().games() == 0) {
            sender.sendMessage(Texts.msg(lang, "stats.none", Placeholder.unparsed("name", who)));
            return;
        }
        PlayerData d = data.get();
        sender.sendMessage(Texts.msg(lang, "stats.head", Placeholder.unparsed("name", d.name())));
        for (Component line : HallMenus.statLines(lang, d)) {
            sender.sendMessage(line);
        }
    }

    private void top(CommandSender sender) {
        String lang = sender instanceof Player p ? plugin.lang(p) : "de";
        for (Leaderboard.Board b : Leaderboard.Board.values()) {
            sender.sendMessage(Texts.msg(lang, "top.head", Placeholder.unparsed("board", Texts.plain(lang, "board." + b.id()))));
            for (Component row : HallMenus.rows(plugin, lang, b, Leaderboard.SIZE)) {
                sender.sendMessage(row);
            }
        }
    }

    // ---------- Verwaltung ----------

    private boolean admin(CommandSender s, String[] args) {
        if (!s.hasPermission(ADMIN)) {
            return true;
        }
        String sub = args.length > 0 ? args[0].toLowerCase(Locale.ROOT) : "info";
        switch (sub) {
            case "info" -> {
                s.sendMessage(Component.text("Morni Versteckerl " + plugin.getPluginMeta().getVersion() + " – Netzwerk: "
                        + (plugin.net().connected() ? "ja (" + plugin.net().serverName() + ")" : "nein") + ", bereit: "
                        + plugin.ready() + ", Runden: " + plugin.games().all().size() + ", Gendarmerie: "
                        + plugin.hall().waiting().size()));
            }
            case "status" -> {
                List<String> lines = new ArrayList<>();
                lines.add("bereit=" + plugin.ready() + " gendarmerie=" + plugin.hall().waiting().size() + " countdown="
                        + plugin.hall().countdown());
                for (Game g : plugin.games().all()) {
                    lines.add(g.status());
                }
                s.sendMessage(Component.text(String.join("\n", lines)));
            }
            case "start" -> {
                MapType m = args.length > 1 ? MapType.byId(args[1]) : null;
                if (m == null) {
                    m = plugin.hall().votes().winner(plugin.games().free());
                }
                if (m == null || plugin.games().on(m) != null) {
                    s.sendMessage(Component.text("Keine freie Karte."));
                    return true;
                }
                Game g = plugin.hall().start(m);
                s.sendMessage(Component.text(g == null ? "Zu wenige Leute in der Gendarmerie (mindestens 2)."
                        : "Runde auf " + m.id() + " gestartet."));
            }
            case "zeit" -> {
                if (args.length < 2) {
                    s.sendMessage(Component.text("/versteckerladmin zeit <sekunden> [karte]"));
                    return true;
                }
                Game g = gameArg(args, 2);
                int secs;
                try {
                    secs = Integer.parseInt(args[1]);
                } catch (NumberFormatException e) {
                    s.sendMessage(Component.text("Keine Zahl: " + args[1]));
                    return true;
                }
                if (g == null) {
                    s.sendMessage(Component.text("Keine Runde."));
                    return true;
                }
                g.forceTime(secs);
                s.sendMessage(Component.text("Restzeit " + g.map().id() + ": " + secs + " s"));
            }
            case "sucher" -> {
                List<UUID> list = new ArrayList<>();
                for (String n : Arrays.copyOfRange(args, 1, args.length)) {
                    if (n.equalsIgnoreCase("weg")) {
                        list.clear();
                        break;
                    }
                    Player p = Bukkit.getPlayerExact(n);
                    if (p != null) {
                        list.add(p.getUniqueId());
                    }
                }
                plugin.hall().preferSeekers(list);
                s.sendMessage(Component.text("Nächste Gendarmen: " + list.size()));
            }
            case "ende" -> {
                Game g = gameArg(args, 1);
                if (g != null) {
                    g.forceEnd();
                }
                s.sendMessage(Component.text(g == null ? "Keine Runde." : "Runde " + g.map().id() + " beendet."));
            }
            case "neubauen" -> {
                s.sendMessage(Component.text("Baue neu …"));
                plugin.rebuild(() -> s.sendMessage(Component.text("Neubau fertig.")));
            }
            case "neuladen" -> {
                plugin.reloadSettings();
                s.sendMessage(Component.text("Einstellungen neu geladen."));
            }
            case "guthaben" -> balance(s, args);
            case "laden" -> wallet(s, args);
            default -> s.sendMessage(Component.text("/versteckerladmin info|status|start [karte]|zeit <sek> [karte]"
                    + "|sucher <namen…|weg>|ende [karte]|guthaben <spieler> <zahl|+zahl|-zahl>|laden <spieler>"
                    + "|neubauen|neuladen"));
        }
        return true;
    }

    /** Guthaben setzen oder ändern (Verwaltung, Tests): nie unter 0, zählt nicht zu „Punkte insgesamt“. */
    private void balance(CommandSender s, String[] args) {
        if (args.length < 3) {
            s.sendMessage(Component.text("/versteckerladmin guthaben <spieler> <zahl|+zahl|-zahl>"));
            return;
        }
        Player p = Bukkit.getPlayerExact(args[1]);
        if (p == null) {
            s.sendMessage(Component.text("Nicht online: " + args[1]));
            return;
        }
        PlayerData d = plugin.data(p);
        String v = args[2];
        long n;
        try {
            n = Long.parseLong(v.startsWith("+") ? v.substring(1) : v);
        } catch (NumberFormatException e) {
            s.sendMessage(Component.text("Keine Zahl: " + v));
            return;
        }
        long now = v.startsWith("+") || v.startsWith("-") ? d.adjust(n) : d.adjust(n - d.balance());
        plugin.store().save(d);
        plugin.boards().hall(p);
        s.sendMessage(Component.text("Guthaben " + p.getName() + ": " + now));
    }

    /** Kramerladen-Werte einer Person (Verwaltung, Tests). */
    private void wallet(CommandSender s, String[] args) {
        if (args.length < 2) {
            s.sendMessage(Component.text("/versteckerladmin laden <spieler>"));
            return;
        }
        Player online = Bukkit.getPlayerExact(args[1]);
        Optional<PlayerData> data;
        if (online != null) {
            data = Optional.of(plugin.data(online));
        } else {
            @SuppressWarnings("deprecation")
            OfflinePlayer off = Bukkit.getOfflinePlayer(args[1]);
            data = plugin.store().read(off.getUniqueId());
        }
        if (data.isEmpty()) {
            s.sendMessage(Component.text("Keine Daten: " + args[1]));
            return;
        }
        PlayerData d = data.get();
        List<String> eq = new ArrayList<>();
        for (Catalog.Kind k : Catalog.Kind.values()) {
            Catalog.Item i = plugin.shop().equipped(d, k);
            eq.add(k.prefix() + ":" + (i == null ? "-" : i.id()));
        }
        s.sendMessage(Component.text("guthaben=" + d.balance() + " gesamt=" + d.points() + " ausgegeben=" + d.spent()
                + " serie=" + d.streak() + " beste-serie=" + d.bestStreak() + " tagesbonus=" + (d.dailyDay().isEmpty()
                        ? "-" : d.dailyDay()) + " hilfen=" + d.helps() + " frei=" + String.join(",", d.owned())
                + " aus=" + String.join(",", eq)));
    }

    private Game gameArg(String[] args, int index) {
        if (args.length > index) {
            MapType m = MapType.byId(args[index]);
            return m == null ? null : plugin.games().on(m);
        }
        return plugin.games().all().isEmpty() ? null : plugin.games().all().iterator().next();
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        String name = command.getName().toLowerCase(Locale.ROOT);
        List<String> options = new ArrayList<>();
        if (name.equals("versteckerladmin") && args.length == 1) {
            options.addAll(List.of("info", "status", "start", "zeit", "sucher", "ende", "guthaben", "laden", "neubauen",
                    "neuladen"));
        } else if (name.equals("versteckerladmin") && args.length == 2
                && (args[0].equalsIgnoreCase("guthaben") || args[0].equalsIgnoreCase("laden"))) {
            Bukkit.getOnlinePlayers().forEach(p -> options.add(p.getName()));
        } else if ((name.equals("karte") || name.equals("zuschauen")) && args.length == 1
                || name.equals("versteckerladmin") && args.length == 2 && args[0].equalsIgnoreCase("start")) {
            for (MapType m : MapType.values()) {
                options.add(m.id());
            }
            if (name.equals("zuschauen")) {
                Bukkit.getOnlinePlayers().forEach(p -> options.add(p.getName()));
            }
        } else if (name.equals("stats") && args.length == 1) {
            Bukkit.getOnlinePlayers().forEach(p -> options.add(p.getName()));
        }
        String last = args.length == 0 ? "" : args[args.length - 1].toLowerCase(Locale.ROOT);
        return options.stream().filter(o -> o.toLowerCase(Locale.ROOT).startsWith(last)).toList();
    }
}
