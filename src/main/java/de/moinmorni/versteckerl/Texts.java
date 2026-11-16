package de.moinmorni.versteckerl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

/**
 * Alle Texte für Spieler, auf Deutsch und Englisch, als MiniMessage. Eigene Farben: {@code <em>} Smaragd,
 * {@code <au>} Gold, {@code <bl>} Weiß-Blau. Namen von Spielern kommen immer als {@code Placeholder.unparsed} hinein.
 */
public final class Texts {

    public static final int EMERALD = 0x2BB673;
    public static final int GOLD = 0xF2B632;
    public static final int BLUE = 0x5DA9E9;

    public static final MiniMessage MM = MiniMessage.builder()
            .editTags(t -> t.resolvers(
                    TagResolver.resolver("em", Tag.styling(TextColor.color(EMERALD))),
                    TagResolver.resolver("au", Tag.styling(TextColor.color(GOLD))),
                    TagResolver.resolver("bl", Tag.styling(TextColor.color(BLUE)))))
            .build();

    private static final Map<String, String[]> TEXTS = new LinkedHashMap<>();

    private Texts() {
    }

    private static void t(String key, String de, String en) {
        if (TEXTS.put(key, new String[] {de, en}) != null) {
            throw new IllegalStateException("Text doppelt: " + key);
        }
    }

    static {
        general();
        hall();
        items();
        maps();
        shapes();
        menus();
        round();
        end();
        side();
    }

    private static void general() {
        t("prefix", "<au>❖</au> <em>Versteckerl</em> <dark_gray>»</dark_gray> ",
                "<au>❖</au> <em>Hide & Seek</em> <dark_gray>»</dark_gray> ");
        t("mode", "Versteckerl", "Hide & Seek");
        t("only-players", "Das geht nur im Spiel.", "Only possible in-game.");
        t("not-ready", "<gray>Die Karten werden gerade gebaut – gleich geht's los!</gray>",
                "<gray>The maps are being built – just a moment!</gray>");
        t("unknown-player", "<gray>Niemanden namens <white><name></white> gefunden.</gray>",
                "<gray>Nobody called <white><name></white> found.</gray>");
        t("lobby.kick", "<au>MoinMorni</au>\n<gray>Zurück in die Lobby: Bitte verbinde dich neu mit dem Netzwerk.</gray>",
                "<au>MoinMorni</au>\n<gray>Back to the lobby: please reconnect to the network.</gray>");
        t("lobby.going", "<gray>Ab in die Lobby – bis bald!</gray>", "<gray>Off to the lobby – see you soon!</gray>");
        t("spectate.start", "<gray>Du schaust <em><map></em> zu. <white>/verlassen</white> bringt dich zurück.</gray>",
                "<gray>You're watching <em><map></em>. <white>/leave</white> takes you back.</gray>");
        t("spectate.none", "<gray>Gerade läuft keine Runde.</gray>", "<gray>No round is running right now.</gray>");
        t("leave.none", "<gray>Du bist schon in der Gendarmerie.</gray>", "<gray>You're already at the police post.</gray>");
        t("leave.done", "<gray>Zurück in der Gendarmerie.</gray>", "<gray>Back at the police post.</gray>");
        t("stats.head", "<au>Versteckerl – <name></au>", "<au>Hide & Seek – <name></au>");
        t("stats.none", "<gray>Von <white><name></white> gibt es noch keine Werte.</gray>",
                "<gray>No stats for <white><name></white> yet.</gray>");
        t("top.head", "<au><board></au>", "<au><board></au>");
    }

    private static void hall() {
        t("hall.title", "<au>❖ Versteckerl</au>", "<au>❖ Hide & Seek</au>");
        t("hall.subtitle", "<gray>Verwandle dich – und lass dich nicht finden!</gray>",
                "<gray>Disguise yourself – and don't get found!</gray>");
        t("hall.welcome", "<gray>Grüß dich in der <em>Gendarmerie</em>! Mit dem <white>Kompass</white> stimmst du über die Karte ab, mit dem <white>grünen Farbstoff</white> sagst du „bereit“.</gray>",
                "<gray>Welcome to the <em>police post</em>! Use the <white>compass</white> to vote for a map and the <white>green dye</white> to say you're ready.</gray>");
        t("hall.how", "<gray>So geht's: Die <au>Versteckten</au> verwandeln sich in einen Gegenstand der Karte – ein Fass, einen Heuballen, einen Blumentopf … Wer ein paar Sekunden still steht, <white>rastet ein</white> und ist ein echter Block. Die <em>Dorfgendarmen</em> suchen und schlagen zu – aber Vorsicht: ein Schlag auf ein echtes Ding kostet sie Leben!</gray>",
                "<gray>How it works: the <au>hiders</au> turn into something on the map – a barrel, a hay bale, a flower pot … Stand still for a few seconds and you <white>lock in place</white> as a real block. The <em>village constables</em> search and strike – but careful: hitting a real thing costs them health!</gray>");
        t("hall.waiting", "<gray>Warte auf Mitspieler … (<white><n></white> da, ab <white><min></white> geht's los – oder alle „bereit“)</gray>",
                "<gray>Waiting for players … (<white><n></white> here, starts at <white><min></white> – or when everyone is ready)</gray>");
        t("hall.countdown", "<gray>Nächste Runde in <au><secs> s</au> · Karte: <em><map></em></gray>",
                "<gray>Next round in <au><secs> s</au> · map: <em><map></em></gray>");
        t("hall.no-map", "<gray>Alle Karten sind belegt – gleich wird eine frei.</gray>",
                "<gray>All maps are busy – one will be free soon.</gray>");
        t("hall.ready", "<em>Du bist bereit.</em> <gray>Sind alle bereit, geht's gleich los.</gray>",
                "<em>You're ready.</em> <gray>Once everyone is ready, we start.</gray>");
        t("hall.not-ready", "<gray>Du bist nicht mehr bereit.</gray>", "<gray>You're no longer ready.</gray>");
        t("hall.voted", "<gray>Deine Stimme: <em><map></em>.</gray>", "<gray>Your vote: <em><map></em>.</gray>");
        t("hall.full", "<gray>Die Runde ist voll (höchstens 16) – du bist in der nächsten dabei.</gray>",
                "<gray>The round is full (16 at most) – you're in the next one.</gray>");
        t("hall.starting", "<em>Auf geht's!</em> <gray>Karte: <au><map></au> – <white><n></white> Leute.</gray>",
                "<em>Here we go!</em> <gray>Map: <au><map></au> – <white><n></white> players.</gray>");
        t("hall.party-joined", "<gray>Deine Party spielt gerade – du hilfst als <em>Dorfgendarm</em> beim Suchen!</gray>",
                "<gray>Your party is playing – you join the search as a <em>village constable</em>!</gray>");
    }

    private static void items() {
        t("item.vote", "<em>✎ Karte wählen</em>", "<em>✎ Vote for a map</em>");
        t("item.vote.lore", "<gray>Rechtsklick: Karte wählen oder einer Runde zuschauen.</gray>",
                "<gray>Right-click: vote for a map or watch a round.</gray>");
        t("item.ready", "<em>✔ Bereit</em>", "<em>✔ Ready</em>");
        t("item.ready.lore", "<gray>Rechtsklick: bereit / nicht bereit.</gray>", "<gray>Right-click: ready / not ready.</gray>");
        t("item.stats", "<au>★ Steckbriefe</au>", "<au>★ Wanted posters</au>");
        t("item.stats.lore", "<gray>Rechtsklick: deine Werte und die Besten.</gray>",
                "<gray>Right-click: your stats and the best players.</gray>");
        t("item.lobby", "<bl>⌂ Zurück zur Lobby</bl>", "<bl>⌂ Back to the lobby</bl>");
        t("item.lobby.lore", "<gray>Rechtsklick: zurück ins Dorf.</gray>", "<gray>Right-click: back to the village.</gray>");
        t("item.shape", "<au>❖ Gestalt wählen</au>", "<au>❖ Choose your disguise</au>");
        t("item.shape.lore", "<gray>Rechtsklick: in was verwandelst du dich?</gray>\n<gray>Geht in der Versteckzeit, so oft du magst.</gray>",
                "<gray>Right-click: what do you turn into?</gray>\n<gray>Change as often as you like while hiding.</gray>");
        t("item.shape.change", "<au>❖ Gestalt wechseln (1×)</au>", "<au>❖ Change disguise (once)</au>");
        t("item.shape.change.lore", "<gray>Rechtsklick: einmal pro Runde während der Suche</gray>\n<gray>die Gestalt wechseln.</gray>",
                "<gray>Right-click: change your disguise once</gray>\n<gray>per round during the search.</gray>");
        t("item.shape.used.lore", "<gray>Schon gewechselt – diese Runde nicht mehr.</gray>",
                "<gray>Already changed – not again this round.</gray>");
        t("item.whistle", "<em>♪ Pfiff</em>", "<em>♪ Whistle</em>");
        t("item.whistle.lore", "<gray>Rechtsklick: pfeifen. Gibt Punkte – je später,</gray>\n<gray>desto mehr. Aber die Gendarmen hören dich!</gray>",
                "<gray>Right-click: whistle. Gives points – the later,</gray>\n<gray>the more. But the constables can hear you!</gray>");
        t("item.jump", "<bl>⇧ Sprungkraft</bl>", "<bl>⇧ Spring step</bl>");
        t("item.jump.lore", "<gray>Rechtsklick: ein paar Sekunden höher springen.</gray>",
                "<gray>Right-click: jump higher for a few seconds.</gray>");
        t("item.stick", "<em>⚔ Gendarmenstab</em>", "<em>⚔ Constable's baton</em>");
        t("item.stick.lore", "<gray>Linksklick auf einen verdächtigen Gegenstand.</gray>\n<gray>Ein echter Gegenstand kostet dich Leben!</gray>",
                "<gray>Left-click a suspicious object.</gray>\n<gray>Hitting a real object costs you health!</gray>");
        t("item.stick.hint", "<gray>Mit <white>Linksklick</white> schlägst du zu.</gray>",
                "<gray>Strike with a <white>left-click</white>.</gray>");
        t("item.sniff", "<au>☘ Spürnase</au>", "<au>☘ Sniffer nose</au>");
        t("item.sniff.lore", "<gray>Rechtsklick (einmal pro Runde): eine Spur zeigt</gray>\n<gray>kurz in Richtung eines Versteckten.</gray>",
                "<gray>Right-click (once per round): a trail briefly</gray>\n<gray>points towards a hider.</gray>");
        t("item.sniff.used.lore", "<gray>Schon geschnuppert – diese Runde nicht mehr.</gray>",
                "<gray>Already sniffed – not again this round.</gray>");
    }

    private static void maps() {
        t("map.dorfplatz", "Dorfplatz", "Village Square");
        t("map.dorfplatz.desc", "<gray>Bauerndorf mit Brunnen und Maibaum, Wirtshaus,</gray>\n<gray>Stadel, Kramerladen und Schmiede.</gray>",
                "<gray>Farming village with fountain and maypole, inn,</gray>\n<gray>barn, general store and smithy.</gray>");
        t("map.wirtshaus", "Wirtshaus", "The Inn");
        t("map.wirtshaus.desc", "<gray>Großes Gasthaus von innen: Gaststube, Küche,</gray>\n<gray>Stube, Bierkeller und Tanzsaal.</gray>",
                "<gray>A big inn from the inside: taproom, kitchen,</gray>\n<gray>parlour, beer cellar and dance hall.</gray>");
        t("map.heustadel", "Heustadel", "Hay Barn");
        t("map.heustadel.desc", "<gray>Bauernhof mit Heuboden, Stall, Gemüsegarten,</gray>\n<gray>Bienenwiese und Kompostecke.</gray>",
                "<gray>Farm with hay loft, stable, vegetable garden,</gray>\n<gray>bee meadow and compost corner.</gray>");
        t("board.stand.desc.dorfplatz", "Bauerndorf mit Brunnen und Maibaum", "Farming village with maypole");
        t("board.stand.desc.wirtshaus", "Gaststube, Küche, Keller und Saal", "Taproom, kitchen, cellar and hall");
        t("board.stand.desc.heustadel", "Bauernhof mit Heuboden und Garten", "Farm with hay loft and garden");
    }

    private static void shapes() {
        t("shape.heuballen", "Heuballen", "Hay bale");
        t("shape.heuballen.hint", "<gray>Im Stadel und auf der Wiese liegen viele.</gray>",
                "<gray>Lots of them in the barn and on the meadow.</gray>");
        t("shape.fass", "Fass", "Barrel");
        t("shape.fass.hint", "<gray>Fässer stehen überall herum.</gray>", "<gray>Barrels stand around everywhere.</gray>");
        t("shape.bierfass", "Bierfass", "Beer keg");
        t("shape.bierfass.hint", "<gray>Liegt im Regal hinter der Schänke und im Keller.</gray>",
                "<gray>Lies in the rack behind the bar and in the cellar.</gray>");
        t("shape.blumentopf", "Blumentopf", "Flower pot");
        t("shape.blumentopf.hint", "<gray>Klein und unauffällig – auf Tischen und Fensterbänken.</gray>",
                "<gray>Small and inconspicuous – on tables and window sills.</gray>");
        t("shape.kuerbis", "Kürbis", "Pumpkin");
        t("shape.kuerbis.hint", "<gray>Im Garten und in der Auslage.</gray>", "<gray>In the garden and on display.</gray>");
        t("shape.melone", "Melone", "Melon");
        t("shape.melone.hint", "<gray>Zwischen den Ranken im Gemüsegarten.</gray>",
                "<gray>Between the vines in the vegetable garden.</gray>");
        t("shape.amboss", "Amboss", "Anvil");
        t("shape.amboss.hint", "<gray>Schwer zu übersehen – aber in der Schmiede gibt's viele.</gray>",
                "<gray>Hard to miss – but there are plenty in the smithy.</gray>");
        t("shape.bienenstock", "Bienenstock", "Beehive");
        t("shape.bienenstock.hint", "<gray>Auf der Obst- und der Bienenwiese.</gray>",
                "<gray>In the orchard and on the bee meadow.</gray>");
        t("shape.laterne", "Laterne", "Lantern");
        t("shape.laterne.hint", "<gray>Steht auf Tischen, Bänken und am Boden.</gray>",
                "<gray>Stands on tables, benches and the ground.</gray>");
        t("shape.hackstock", "Hackstock", "Chopping block");
        t("shape.hackstock.hint", "<gray>Bei der Holzlege und unter den Bienenstöcken.</gray>",
                "<gray>By the woodshed and under the beehives.</gray>");
        t("shape.kessel", "Kessel", "Cauldron");
        t("shape.kessel.hint", "<gray>Tränken, Brunnenkübel, Waschkessel.</gray>",
                "<gray>Troughs, well buckets, washing pots.</gray>");
        t("shape.ofen", "Ofen", "Stove");
        t("shape.ofen.hint", "<gray>In der Küche stehen Herde und Räucheröfen.</gray>",
                "<gray>The kitchen is full of stoves and smokers.</gray>");
        t("shape.buecherregal", "Bücherregal", "Bookshelf");
        t("shape.buecherregal.hint", "<gray>Die Stube hat eine ganze Bücherwand.</gray>",
                "<gray>The parlour has a whole wall of books.</gray>");
        t("shape.bank", "Bank", "Bench");
        t("shape.bank.hint", "<gray>An jedem Tisch stehen Bänke.</gray>", "<gray>Every table has benches.</gray>");
        t("shape.komposter", "Komposter", "Compost bin");
        t("shape.komposter.hint", "<gray>In der Kompostecke und im Garten.</gray>",
                "<gray>In the compost corner and the garden.</gray>");
    }

    private static void menus() {
        t("menu.vote.title", "Karte wählen", "Vote for a map");
        t("menu.vote.shapes", "<gray>Gestalten: <white><list></white></gray>", "<gray>Disguises: <white><list></white></gray>");
        t("menu.vote.votes", "<gray>Stimmen: <white><n></white></gray>", "<gray>Votes: <white><n></white></gray>");
        t("menu.vote.busy", "<gray>Läuft gerade (<white><n></white> versteckt) – klicken zum Zuschauen</gray>",
                "<gray>Running (<white><n></white> hidden) – click to watch</gray>");
        t("menu.vote.click", "<em>▶ Klicken zum Abstimmen</em>", "<em>▶ Click to vote</em>");
        t("menu.vote.mine", "<au>✔ Deine Stimme</au>", "<au>✔ Your vote</au>");
        t("menu.close", "<red>Schließen</red>", "<red>Close</red>");
        t("menu.stats.title", "Steckbriefe", "Wanted posters");
        t("menu.stats.me", "<au>Deine Werte</au>", "<au>Your stats</au>");
        t("menu.stats.empty", "<gray>Noch leer – sei der Erste!</gray>", "<gray>Still empty – be the first!</gray>");
        t("menu.stats.row", "<au><pos>.</au> <white><name></white> <gray>– <n></gray>",
                "<au><pos>.</au> <white><name></white> <gray>– <n></gray>");
        t("menu.stats.lines", "<gray>Runden: <white><games></white></gray>\n<gray>Siege als Versteckter: <white><winh></white></gray>\n<gray>Siege als Gendarm: <white><wins></white></gray>\n<gray>Gefunden: <white><found></white></gray>\n<gray>Bis zum Schluss versteckt: <white><survived></white></gray>\n<gray>Pfiffe: <white><whistles></white></gray>\n<gray>Punkte: <white><points></white> · beste Runde: <au><best></au></gray>",
                "<gray>Rounds: <white><games></white></gray>\n<gray>Wins as hider: <white><winh></white></gray>\n<gray>Wins as constable: <white><wins></white></gray>\n<gray>Found: <white><found></white></gray>\n<gray>Hidden until the end: <white><survived></white></gray>\n<gray>Whistles: <white><whistles></white></gray>\n<gray>Points: <white><points></white> · best round: <au><best></au></gray>");
        t("menu.shape.title", "Gestalt wählen", "Choose your disguise");
        t("menu.shape.title.change", "Gestalt wechseln (einmal)", "Change disguise (once)");
        t("menu.shape.mine", "<au>✔ Das bist du gerade</au>", "<au>✔ That's you right now</au>");
        t("menu.shape.click", "<em>▶ Klicken zum Verwandeln</em>", "<em>▶ Click to transform</em>");
        t("menu.shape.click.change", "<em>▶ Klicken: dein einer Wechsel in dieser Runde</em>",
                "<em>▶ Click: your one change this round</em>");
        t("board.title", "<au><b>❖ Steckbriefe ❖</b></au>", "<au><b>❖ Wanted posters ❖</b></au>");
        t("board.punkte", "<em>Beste Runde (Punkte)</em>", "<em>Best round (points)</em>");
        t("board.funde", "<em>Meiste Funde</em>", "<em>Most finds</em>");
        t("board.verstecke", "<em>Am öftesten unentdeckt</em>", "<em>Most often never found</em>");
        t("board.stand", "<em><b><map></b></em>\n<gray><desc></gray>\n<au><list></au>",
                "<em><b><map></b></em>\n<gray><desc></gray>\n<au><list></au>");
    }

    private static void round() {
        t("start.roles", "<gray>Dorfgendarmen: <em><seekers></em> · <au><n></au> verstecken sich.</gray>",
                "<gray>Village constables: <em><seekers></em> · <au><n></au> are hiding.</gray>");
        t("start.hider.title", "<au>Versteck dich!</au>", "<au>Go and hide!</au>");
        t("start.hider.sub", "<gray>Die Gendarmen kommen in <white><secs> s</white></gray>",
                "<gray>The constables come in <white><secs> s</white></gray>");
        t("start.hider.chat", "<gray>Du bist ein <au><shape></au>. Such dir eine andere <white>Gestalt</white> aus (erster Knopf) und dann ein gutes Versteck. Steh still, dann <white>rastest du ein</white> und bist ein echter Block. Bewegst du dich, springt er auf.</gray>",
                "<gray>You're a <au><shape></au>. Pick another <white>disguise</white> (first button), then a good hiding spot. Stand still and you <white>lock in place</white> as a real block. Move, and it pops open again.</gray>");
        t("start.seeker.title", "<em>Du bist Dorfgendarm!</em>", "<em>You're a village constable!</em>");
        t("start.seeker.sub", "<gray>Augen zu – in <white><secs> s</white> geht die Suche los</gray>",
                "<gray>Eyes closed – the search starts in <white><secs> s</white></gray>");
        t("start.seeker.chat", "<gray>Du wartest in der Gendarmenstube. Danach: schlag mit dem <white>Gendarmenstab</white> auf alles, was verdächtig ist – aber ein echter Gegenstand kostet dich Leben. Die <au>Spürnase</au> hilft einmal pro Runde.</gray>",
                "<gray>You wait in the constables' room. Then: strike anything suspicious with your <white>baton</white> – but a real object costs you health. Your <au>sniffer nose</au> helps once per round.</gray>");
        t("game.late", "<gray><white><player></white> hilft jetzt als Gendarm mit.</gray>",
                "<gray><white><player></white> now helps as a constable.</gray>");
        t("game.left", "<gray><white><player></white> hat die Runde verlassen.</gray>",
                "<gray><white><player></white> left the round.</gray>");
        t("hide.soon", "<au>Die Gendarmen kommen in <secs> s!</au>", "<au>The constables come in <secs> s!</au>");
        t("seek.title", "<em>Die Suche beginnt!</em>", "<em>The search begins!</em>");
        t("seek.sub.seeker", "<gray>Finde alle Versteckten!</gray>", "<gray>Find all the hiders!</gray>");
        t("seek.sub.hider", "<gray>Still halten – und ab und zu pfeifen</gray>", "<gray>Stay still – and whistle now and then</gray>");
        t("seek.chat", "<gray>Die Gendarmen sind los! Suchzeit: <white><time></white>.</gray>",
                "<gray>The constables are out! Search time: <white><time></white>.</gray>");
        t("lock.no", "<gray>Hier kannst du nicht einrasten – such dir freien Boden.</gray>",
                "<gray>You can't lock in here – find a free spot on solid ground.</gray>");
        t("lock.taken", "<gray>Hier steht schon jemand.</gray>", "<gray>Someone is already standing here.</gray>");
        t("lock.done", "<em>✔ Eingerastet</em> <gray>– du bist jetzt ein echter Block.</gray>",
                "<em>✔ Locked in</em> <gray>– you're a real block now.</gray>");
        t("shape.change.used", "<gray>Deine Gestalt hast du in dieser Runde schon gewechselt.</gray>",
                "<gray>You've already changed your disguise this round.</gray>");
        t("shape.chosen", "<gray>Du bist jetzt ein <au><shape></au>.</gray>", "<gray>You're now a <au><shape></au>.</gray>");
        t("shape.only-hiders", "<gray>Gestalten gibt es nur für Versteckte.</gray>", "<gray>Disguises are only for hiders.</gray>");
        t("whistle.not-yet", "<gray>Pfeifen geht erst, wenn die Suche läuft.</gray>",
                "<gray>You can whistle once the search is running.</gray>");
        t("whistle.cooldown", "<gray>Gleich wieder – noch <white><secs> s</white>.</gray>",
                "<gray>Not yet – <white><secs> s</white> to go.</gray>");
        t("whistle.done", "<em>♪ Fiu-fiu!</em> <au>+<n> Punkte</au>", "<em>♪ Phweet!</em> <au>+<n> points</au>");
        t("whistle.forced", "<gray>♪ <au>Pflicht-Pfiff!</au> Du warst zu lange still – diesmal ohne Punkte.</gray>",
                "<gray>♪ <au>Forced whistle!</au> You were quiet too long – no points this time.</gray>");
        t("whistle.duty", "<au>♪ Ab jetzt muss jeder Versteckte mindestens alle <secs> Sekunden pfeifen!</au>",
                "<au>♪ From now on every hider must whistle at least every <secs> seconds!</au>");
        t("whistle.warn", "<au>♪ Pfeif in den nächsten <secs> s – sonst pfeifst du von selbst!</au>",
                "<au>♪ Whistle within <secs> s – or you'll whistle by yourself!</au>");
        t("jump.cooldown", "<gray>Sprungkraft lädt noch – <white><secs> s</white>.</gray>",
                "<gray>Spring step is recharging – <white><secs> s</white>.</gray>");
        t("jump.done", "<bl>⇧ Sprungkraft für <secs> s!</bl>", "<bl>⇧ Spring step for <secs> s!</bl>");
        t("sniff.not-yet", "<gray>Die Spürnase hilft erst bei der Suche.</gray>",
                "<gray>The sniffer nose only helps during the search.</gray>");
        t("sniff.used", "<gray>Deine Spürnase hast du in dieser Runde schon benutzt.</gray>",
                "<gray>You've already used your sniffer nose this round.</gray>");
        t("sniff.nothing", "<gray>Hier riecht nichts mehr nach Versteckten …</gray>",
                "<gray>Nothing smells of hiders any more …</gray>");
        t("sniff.start", "<au>☘ Deine Spürnase zuckt!</au> <gray>Folg der goldenen Spur – etwa <white><n></white> Blöcke.</gray>",
                "<au>☘ Your nose twitches!</au> <gray>Follow the golden trail – about <white><n></white> blocks.</gray>");
        t("hit.hider", "<red>Getroffen!</red> <gray>Noch</gray> <lives> <gray>– lauf!</gray>",
                "<red>Hit!</red> <gray>Left:</gray> <lives> <gray>– run!</gray>");
        t("hit.seeker", "<em>Treffer!</em> <gray>Noch <white><n></white> bis gefunden.</gray>",
                "<em>Hit!</em> <gray><white><n></white> more to find them.</gray>");
        t("found.chat", "<em><seeker></em> <gray>hat</gray> <au><hider></au> <gray>gefunden – als <white><shape></white>! Noch <white><n></white> versteckt.</gray>",
                "<em><seeker></em> <gray>found</gray> <au><hider></au> <gray>– disguised as a <white><shape></white>! <white><n></white> still hidden.</gray>");
        t("found.title", "<red>Gefunden!</red>", "<red>Found!</red>");
        t("found.sub", "<gray>Jetzt suchst du als Gendarm mit.</gray>", "<gray>Now you search as a constable.</gray>");
        t("found.seeker", "<em>Gefunden: <hider></em>", "<em>Found: <hider></em>");
        t("wrong.hit", "<red>Daneben!</red> <gray>Das war nur ein echter <white><thing></white>.</gray>",
                "<red>Wrong!</red> <gray>That was just a real <white><thing></white>.</gray>");
        t("wrong.stun.title", "<red>Puh …</red>", "<red>Phew …</red>");
        t("wrong.stun.sub", "<gray>Zu viel danebengehauen – <white><secs> s</white> verschnaufen.</gray>",
                "<gray>Too many wrong hits – catch your breath for <white><secs> s</white>.</gray>");
        t("wrong.stunned", "<gray>Du verschnaufst noch …</gray>", "<gray>Still catching your breath …</gray>");
        t("wrong.recovered", "<em>Wieder bei Puste!</em>", "<em>Back on your feet!</em>");
        t("hider.bar", "<au><shape></au> <dark_gray>·</dark_gray> <lives> <dark_gray>·</dark_gray> <state>",
                "<au><shape></au> <dark_gray>·</dark_gray> <lives> <dark_gray>·</dark_gray> <state>");
        t("hider.state.solid", "<em>✔ eingerastet</em>", "<em>✔ locked in</em>");
        t("hider.state.still", "<gray>still stehen zum Einrasten <white><bar></white></gray>",
                "<gray>stand still to lock in <white><bar></white></gray>");
        t("seeker.bar", "<em>☘ Spürnase bereit</em> <dark_gray>·</dark_gray> <gray><n> versteckt</gray>",
                "<em>☘ Sniffer nose ready</em> <dark_gray>·</dark_gray> <gray><n> hidden</gray>");
        t("seeker.bar.used", "<gray><n> noch versteckt</gray>", "<gray><n> still hidden</gray>");
        t("bar.hide", "<em>Versteckzeit</em> <gray>– die Gendarmen kommen in</gray> <white><time></white>",
                "<em>Hiding time</em> <gray>– the constables come in</gray> <white><time></white>");
        t("bar.seek", "<au>Suche</au> <white><time></white> <gray>· <n> versteckt</gray>",
                "<au>Search</au> <white><time></white> <gray>· <n> hidden</gray>");
        t("bar.over.hiders", "<au>Die Versteckten haben gewonnen!</au>", "<au>The hiders won!</au>");
        t("bar.over.seekers", "<em>Die Gendarmen haben gewonnen!</em>", "<em>The constables won!</em>");
    }

    private static void end() {
        t("over.hiders", "<au>Die Versteckten gewinnen!</au>", "<au>The hiders win!</au>");
        t("over.seekers", "<em>Die Gendarmen gewinnen!</em>", "<em>The constables win!</em>");
        t("over.you-won", "<gray>Gewonnen – sauber!</gray>", "<gray>You won – well done!</gray>");
        t("over.you-lost", "<gray>Diesmal nicht – nächste Runde!</gray>", "<gray>Not this time – next round!</gray>");
        t("over.hiders.chat", "<au>Die Versteckten gewinnen!</au> <gray>Nicht gefunden: <white><names></white>.</gray>",
                "<au>The hiders win!</au> <gray>Never found: <white><names></white>.</gray>");
        t("over.seekers.chat", "<em>Die Gendarmen gewinnen!</em> <gray>Alle gefunden von: <white><names></white>.</gray>",
                "<em>The constables win!</em> <gray>Everyone found by: <white><names></white>.</gray>");
        t("over.row", "  <au><pos>.</au> <white><name></white> <gray>– <n> Punkte</gray>",
                "  <au><pos>.</au> <white><name></white> <gray>– <n> points</gray>");
        t("over.mine", "<gray>Deine Punkte: <au><n></au></gray>", "<gray>Your points: <au><n></au></gray>");
        t("over.back", "<gray>In <white><secs> s</white> geht's zurück in die Gendarmerie.</gray>",
                "<gray>Back to the police post in <white><secs> s</white>.</gray>");
        t("over.best", "<au>★ Deine beste Runde bisher: <n> Punkte!</au>", "<au>★ Your best round yet: <n> points!</au>");
    }

    private static void side() {
        t("side.title", "<au><b>❖ Versteckerl</b></au>", "<au><b>❖ Hide & Seek</b></au>");
        t("side.map", "<gray>Karte:</gray> <em><map></em>", "<gray>Map:</gray> <em><map></em>");
        t("side.phase.hide", "<gray>Versteckzeit:</gray> <white><time></white>", "<gray>Hiding:</gray> <white><time></white>");
        t("side.phase.seek", "<gray>Suchzeit:</gray> <white><time></white>", "<gray>Searching:</gray> <white><time></white>");
        t("side.phase.over", "<gray>Vorbei</gray> <white><time></white>", "<gray>Over</gray> <white><time></white>");
        t("side.hiders", "<au>Versteckt:</au> <white><n></white>", "<au>Hidden:</au> <white><n></white>");
        t("side.seekers", "<em>Gendarmen:</em> <white><n></white>", "<em>Constables:</em> <white><n></white>");
        t("side.role.hider", "<gray>Du:</gray> <au>versteckt</au>", "<gray>You:</gray> <au>hiding</au>");
        t("side.role.seeker", "<gray>Du:</gray> <em>Gendarm</em>", "<gray>You:</gray> <em>constable</em>");
        t("side.role.spectator", "<gray>Du schaust zu</gray>", "<gray>You're watching</gray>");
        t("side.points", "<gray>Punkte:</gray> <au><n></au>", "<gray>Points:</gray> <au><n></au>");
        t("side.site", "<bl>moinmorni.net</bl>", "<bl>moinmorni.net</bl>");
        t("side.hall.players", "<gray>In der Gendarmerie:</gray> <white><n></white>", "<gray>At the post:</gray> <white><n></white>");
        t("side.hall.start", "<gray>Start in</gray> <au><secs> s</au>", "<gray>Start in</gray> <au><secs> s</au>");
        t("side.hall.wait", "<gray>Warte auf Mitspieler …</gray>", "<gray>Waiting for players …</gray>");
        t("side.hall.found", "<gray>Deine Funde:</gray> <em><n></em>", "<gray>Your finds:</gray> <em><n></em>");
        t("side.hall.survived", "<gray>Unentdeckt:</gray> <au><n></au>", "<gray>Never found:</gray> <au><n></au>");
        t("side.hall.best", "<gray>Beste Runde:</gray> <au><n></au>", "<gray>Best round:</gray> <au><n></au>");
    }

    /** Alle Schlüssel (für Tests). */
    public static Set<String> keys() {
        return Collections.unmodifiableSet(TEXTS.keySet());
    }

    /** Roher MiniMessage-Text. Unbekannte Sprache → Deutsch. */
    public static String raw(String lang, String key) {
        String[] t = TEXTS.get(key);
        if (t == null) {
            return key;
        }
        return "en".equals(lang) ? t[1] : t[0];
    }

    /** Text ohne Vorsilbe; für Gegenstände ohne kursiv. */
    public static Component text(String lang, String key, TagResolver... resolvers) {
        return MM.deserialize(raw(lang, key), resolvers).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
    }

    /** Nachricht für den Chat, mit „❖ Versteckerl »“ davor. */
    public static Component msg(String lang, String key, TagResolver... resolvers) {
        return MM.deserialize(raw(lang, "prefix")).append(MM.deserialize(raw(lang, key), resolvers));
    }

    /** Mehrzeiliger Text als einzelne Zeilen (für Beschreibungen von Gegenständen). */
    public static List<Component> lines(String lang, String key, TagResolver... resolvers) {
        List<Component> out = new ArrayList<>();
        for (String line : raw(lang, key).split("\n")) {
            out.add(MM.deserialize(line, resolvers).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE));
        }
        return out;
    }

    /** Reiner Text (ohne Formatierung). */
    public static String plain(String lang, String key) {
        return net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                .serialize(MM.deserialize(raw(lang, key)));
    }
}
