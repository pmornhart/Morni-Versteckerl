package net.morni.versteckerl;

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
        shopTexts();
        shopItems();
        helpTexts();
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
        t("lobby.kick", "<au>morni.net</au>\n<gray>Zurück in die Lobby: Bitte verbinde dich neu mit morni.net.</gray>",
                "<au>morni.net</au>\n<gray>Back to the lobby: please reconnect to morni.net.</gray>");
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
        t("hall.shop-hint", "<gray>Punkte aus den Runden kommen aufs <au>Guthaben</au>. Beim <em>Krämer Wastl</em> am Stand (oder mit dem <white>Smaragd</white>) gibt's dafür neue Gestalten, Pfiff-Klänge, Effekte und Titel.</gray>",
                "<gray>Points from your rounds go to your <au>balance</au>. <em>Wastl the shopkeeper</em> at his stall (or the <white>emerald</white>) has new disguises, whistle sounds, effects and titles for them.</gray>");
    }

    private static void items() {
        t("item.vote", "<em>✎ Karte wählen</em>", "<em>✎ Vote for a map</em>");
        t("item.vote.lore", "<gray>Rechtsklick: Karte wählen oder einer Runde zuschauen.</gray>",
                "<gray>Right-click: vote for a map or watch a round.</gray>");
        t("item.ready", "<em>✔ Bereit</em>", "<em>✔ Ready</em>");
        t("item.ready.lore", "<gray>Rechtsklick: bereit / nicht bereit.</gray>", "<gray>Right-click: ready / not ready.</gray>");
        t("item.shop", "<em>✦ Kramerladen</em>", "<em>✦ General store</em>");
        t("item.shop.lore", "<gray>Rechtsklick: mit deinem Guthaben Gestalten,</gray>\n<gray>Pfiff-Klänge, Effekte und Titel freischalten.</gray>",
                "<gray>Right-click: unlock disguises, whistle sounds,</gray>\n<gray>effects and titles with your balance.</gray>");
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
        t("item.whistle.lore", "<gray>Rechtsklick: pfeifen. Gibt Punkte – je später,</gray>\n<gray>je lauter und je näher ein Gendarm, desto mehr.</gray>\n<gray>Aber die Gendarmen hören dich!</gray>",
                "<gray>Right-click: whistle. Gives points – the later,</gray>\n<gray>the louder and the closer a constable, the more.</gray>\n<gray>But the constables can hear you!</gray>");
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
        t("item.helps", "<au>✦ Kramerkiste</au>", "<au>✦ Supply box</au>");
        t("item.helps.hider.lore", "<gray>Rechtsklick: Hilfen für diese Runde –</gray>\n<gray>Lockpfiff, Nebelwolke, noch ein Gestaltwechsel.</gray>\n<gray>Bezahlt mit Punkten aus der Runde.</gray>",
                "<gray>Right-click: helps for this round –</gray>\n<gray>decoy whistle, fog cloud, another disguise change.</gray>\n<gray>Paid with points from this round.</gray>");
        t("item.helps.seeker.lore", "<gray>Rechtsklick: Hilfen für diese Runde –</gray>\n<gray>Laterne, noch eine Spürnase.</gray>\n<gray>Bezahlt mit Punkten aus der Runde.</gray>",
                "<gray>Right-click: helps for this round –</gray>\n<gray>lantern, another sniff.</gray>\n<gray>Paid with points from this round.</gray>");
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
        t("shape.werkbank", "Werkbank", "Workbench");
        t("shape.werkbank.hint", "<gray>In Schmiede, Holzlege, Tenne und Küche.</gray>",
                "<gray>In the smithy, woodshed, threshing floor and kitchen.</gray>");
        t("shape.schleifstein", "Schleifstein", "Grindstone");
        t("shape.schleifstein.hint", "<gray>Wo Messer und Sensen geschliffen werden.</gray>",
                "<gray>Wherever knives and scythes get sharpened.</gray>");
        t("shape.webstuhl", "Webstuhl", "Loom");
        t("shape.webstuhl.hint", "<gray>In der Bauernstube und im Laden.</gray>",
                "<gray>In the farmhouse parlour and the shop.</gray>");
        t("shape.busch", "Blühender Busch", "Flowering bush");
        t("shape.busch.hint", "<gray>Auf den Wiesen – such dir einen Platz im Grünen.</gray>",
                "<gray>Out on the meadows – find a spot in the green.</gray>");
        t("shape.lesepult", "Lesepult", "Lectern");
        t("shape.lesepult.hint", "<gray>In der Stube, in den Gästezimmern und im Saal.</gray>",
                "<gray>In the parlour, the guest rooms and the hall.</gray>");
        t("shape.kuchen", "Kuchen", "Cake");
        t("shape.kuchen.hint", "<gray>Auf Tischen und Theken – spring hinauf!</gray>",
                "<gray>On tables and counters – jump up!</gray>");
        t("shape.kuerbislaterne", "Kürbislaterne", "Jack-o'-lantern");
        t("shape.kuerbislaterne.hint", "<gray>Leuchtet – im Laden, im Garten, am Zaun.</gray>",
                "<gray>It glows – in the shop, the garden, on the fence.</gray>");
        t("shape.festtagskuchen", "Festtagskuchen", "Festive cake");
        t("shape.festtagskuchen.hint", "<gray>Mit Kerze – auf den Tischen im Tanzsaal.</gray>",
                "<gray>With a candle – on the tables in the dance hall.</gray>");
    }

    private static void menus() {
        t("menu.vote.title", "Karte wählen", "Vote for a map");
        t("menu.vote.shapes", "<gray>Gestalten: <white><list></white></gray>", "<gray>Disguises: <white><list></white></gray>");
        t("menu.vote.shop-shapes", " (+<n> aus dem Laden)", " (+<n> from the store)");
        t("menu.vote.shop", "<gray>Aus dem Kramerladen: <au><list></au></gray>", "<gray>From the general store: <au><list></au></gray>");
        t("menu.vote.votes", "<gray>Stimmen: <white><n></white></gray>", "<gray>Votes: <white><n></white></gray>");
        t("menu.vote.busy", "<gray>Läuft gerade (<white><n></white> versteckt) – klicken zum Zuschauen</gray>",
                "<gray>Running (<white><n></white> hidden) – click to watch</gray>");
        t("menu.vote.click", "<em>▶ Klicken zum Abstimmen</em>", "<em>▶ Click to vote</em>");
        t("menu.vote.mine", "<au>✔ Deine Stimme</au>", "<au>✔ Your vote</au>");
        t("menu.close", "<red>Schließen</red>", "<red>Close</red>");
        t("menu.back", "<gray>◀ Zurück</gray>", "<gray>◀ Back</gray>");
        t("menu.stats.title", "Steckbriefe", "Wanted posters");
        t("menu.stats.me", "<au>Deine Werte</au>", "<au>Your stats</au>");
        t("menu.stats.empty", "<gray>Noch leer – sei der Erste!</gray>", "<gray>Still empty – be the first!</gray>");
        t("menu.stats.row", "<au><pos>.</au> <white><name></white> <gray>– <n></gray>",
                "<au><pos>.</au> <white><name></white> <gray>– <n></gray>");
        t("menu.stats.lines", "<gray>Runden: <white><games></white></gray>\n<gray>Siege als Versteckter: <white><winh></white></gray>\n<gray>Siege als Gendarm: <white><wins></white></gray>\n<gray>Gefunden: <white><found></white></gray>\n<gray>Bis zum Schluss versteckt: <white><survived></white></gray>\n<gray>Pfiffe: <white><whistles></white></gray>\n<gray>Punkte insgesamt: <white><points></white> · beste Runde: <au><best></au></gray>",
                "<gray>Rounds: <white><games></white></gray>\n<gray>Wins as hider: <white><winh></white></gray>\n<gray>Wins as constable: <white><wins></white></gray>\n<gray>Found: <white><found></white></gray>\n<gray>Hidden until the end: <white><survived></white></gray>\n<gray>Whistles: <white><whistles></white></gray>\n<gray>Total points: <white><points></white> · best round: <au><best></au></gray>");
        t("menu.shape.title", "Gestalt wählen", "Choose your disguise");
        t("menu.shape.title.change", "Gestalt wechseln (einmal)", "Change disguise (once)");
        t("menu.shape.mine", "<au>✔ Das bist du gerade</au>", "<au>✔ That's you right now</au>");
        t("menu.shape.click", "<em>▶ Klicken zum Verwandeln</em>", "<em>▶ Click to transform</em>");
        t("menu.shape.click.change", "<em>▶ Klicken: dein einer Wechsel in dieser Runde</em>",
                "<em>▶ Click: your one change this round</em>");
        t("menu.shape.locked", "<gray>✖ Noch nicht freigeschaltet – <au><n> ✦</au></gray>",
                "<gray>✖ Not unlocked yet – <au><n> ✦</au></gray>");
        t("menu.shape.locked.where", "<gray>Gibt's beim Krämer Wastl in der Gendarmerie.</gray>",
                "<gray>Wastl the shopkeeper at the police post has it.</gray>");
        t("menu.stats.wallet", "<gray>Guthaben: <au><balance> ✦</au></gray>\n<gray>Siege in Folge: <white><streak></white> · beste Serie: <white><best></white></gray>\n<gray>Freigeschaltet: <white><unlocks></white></gray>",
                "<gray>Balance: <au><balance> ✦</au></gray>\n<gray>Wins in a row: <white><streak></white> · best streak: <white><best></white></gray>\n<gray>Unlocked: <white><unlocks></white></gray>");
        t("menu.stats.title-line", "<gray>Titel: <au><title></au></gray>", "<gray>Title: <au><title></au></gray>");
        t("board.title", "<au><b>❖ Steckbriefe ❖</b></au>", "<au><b>❖ Wanted posters ❖</b></au>");
        t("board.punkte", "<em>Beste Runde (Punkte)</em>", "<em>Best round (points)</em>");
        t("board.gesamt", "<em>Punkte insgesamt</em>", "<em>Total points</em>");
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
        t("start.hider.chat", "<gray>Deine Gestalt: <au><shape></au>. Such dir gern eine andere aus (erster Knopf), dann ein gutes Versteck. Steh still, dann <white>rastest du ein</white> und bist ein echter Block. Bewegst du dich, springt er auf.</gray>",
                "<gray>Your disguise: <au><shape></au>. Pick another one if you like (first button), then a good hiding spot. Stand still and you <white>lock in place</white> as a real block. Move, and it pops open again.</gray>");
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
        t("shape.chosen", "<gray>Neue Gestalt: <au><shape></au>.</gray>", "<gray>New disguise: <au><shape></au>.</gray>");
        t("shape.only-hiders", "<gray>Gestalten gibt es nur für Versteckte.</gray>", "<gray>Disguises are only for hiders.</gray>");
        t("shape.locked", "<gray>Die Gestalt <au><shape></au> schaltest du beim Krämer Wastl in der Gendarmerie frei.</gray>",
                "<gray>Unlock the <au><shape></au> disguise at Wastl's stall at the police post.</gray>");
        t("whistle.not-yet", "<gray>Pfeifen geht erst, wenn die Suche läuft.</gray>",
                "<gray>You can whistle once the search is running.</gray>");
        t("whistle.cooldown", "<gray>Gleich wieder – noch <white><secs> s</white>.</gray>",
                "<gray>Not yet – <white><secs> s</white> to go.</gray>");
        t("whistle.done", "<em>♪ Fiu-fiu!</em> <au>+<n> Punkte</au><extra>", "<em>♪ Phweet!</em> <au>+<n> points</au><extra>");
        t("whistle.brave", "mutig +<n>", "brave +<n>");
        t("whistle.loud", "laut +<n>", "loud +<n>");
        t("last.hider", "<au>Du bist der Letzte! +<n> Punkte – halt durch!</au>",
                "<au>You're the last one! +<n> points – hang in there!</au>");
        t("last.chat", "<gray>Nur noch <au><player></au> ist versteckt!</gray>", "<gray>Only <au><player></au> is still hidden!</gray>");
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
        t("found.first", "<em>Erster Fund der Runde!</em> <au>+<n> Punkte</au>", "<em>First find of the round!</em> <au>+<n> points</au>");
        t("wrong.hit", "<red>Daneben!</red> <gray>Das war echt (<white><thing></white>) – kostet dich Leben.</gray>",
                "<red>Wrong!</red> <gray>That was real (<white><thing></white>) – it costs you health.</gray>");
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
        t("over.row", "  <au><pos>.</au> <white><name></white><au><title></au> <gray>– <n> Punkte</gray>",
                "  <au><pos>.</au> <white><name></white><au><title></au> <gray>– <n> points</gray>");
        t("settle.head", "<au>Abrechnung</au>", "<au>Your tally</au>");
        t("settle.round", "  <au>+<n></au> <gray>Punkte aus der Runde</gray>", "  <au>+<n></au> <gray>points from the round</gray>");
        t("settle.spent", "  <red>−<n></red> <gray>für Hilfen ausgegeben</gray>", "  <red>−<n></red> <gray>spent on helps</gray>");
        t("settle.streak", "  <au>+<n></au> <gray>Siegesserie (<white><streak></white> in Folge)</gray>",
                "  <au>+<n></au> <gray>winning streak (<white><streak></white> in a row)</gray>");
        t("settle.daily", "  <au>+<n></au> <gray>erster Sieg heute</gray>", "  <au>+<n></au> <gray>first win today</gray>");
        t("settle.total", "  <em>= <n> ✦ aufs Guthaben</em> <gray>· jetzt <au><balance> ✦</au></gray>",
                "  <em>= <n> ✦ to your balance</em> <gray>· now <au><balance> ✦</au></gray>");
        t("settle.bar", "<em>+<n> ✦ aufs Guthaben</em> <gray>· jetzt <au><balance> ✦</au></gray>",
                "<em>+<n> ✦ to your balance</em> <gray>· now <au><balance> ✦</au></gray>");
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
        t("side.points.left", "<gray>Punkte:</gray> <au><n></au> <gray>(übrig <white><left></white>)</gray>",
                "<gray>Points:</gray> <au><n></au> <gray>(<white><left></white> left)</gray>");
        t("side.title-line", "<au><title></au>", "<au><title></au>");
        t("side.bill.head", "<au><b>Abrechnung</b></au>", "<au><b>Your tally</b></au>");
        t("side.bill.round", "<gray>Aus der Runde:</gray> <au>+<n></au>", "<gray>From the round:</gray> <au>+<n></au>");
        t("side.bill.spent", "<gray>Für Hilfen:</gray> <red>−<n></red>", "<gray>Spent on helps:</gray> <red>−<n></red>");
        t("side.bill.streak", "<gray>Siegesserie:</gray> <au>+<n></au>", "<gray>Winning streak:</gray> <au>+<n></au>");
        t("side.bill.daily", "<gray>Erster Sieg heute:</gray> <au>+<n></au>", "<gray>First win today:</gray> <au>+<n></au>");
        t("side.bill.total", "<em>= +<n> ✦ aufs Guthaben</em>", "<em>= +<n> ✦ to your balance</em>");
        t("side.bill.balance", "<gray>Guthaben jetzt:</gray> <au><n> ✦</au>", "<gray>Balance now:</gray> <au><n> ✦</au>");
        t("side.hall.balance", "<gray>Guthaben:</gray> <au><n> ✦</au>", "<gray>Balance:</gray> <au><n> ✦</au>");
        t("side.hall.total", "<gray>Punkte insgesamt:</gray> <white><n></white>", "<gray>Total points:</gray> <white><n></white>");
        t("side.hall.streak", "<gray>Siege in Folge:</gray> <em><n></em>", "<gray>Wins in a row:</gray> <em><n></em>");
        t("side.site", "<bl>morni.net</bl>", "<bl>morni.net</bl>");
        t("side.hall.players", "<gray>In der Gendarmerie:</gray> <white><n></white>", "<gray>At the post:</gray> <white><n></white>");
        t("side.hall.start", "<gray>Start in</gray> <au><secs> s</au>", "<gray>Start in</gray> <au><secs> s</au>");
        t("side.hall.wait", "<gray>Warte auf Mitspieler …</gray>", "<gray>Waiting for players …</gray>");
        t("side.hall.found", "<gray>Deine Funde:</gray> <em><n></em>", "<gray>Your finds:</gray> <em><n></em>");
        t("side.hall.survived", "<gray>Unentdeckt:</gray> <au><n></au>", "<gray>Never found:</gray> <au><n></au>");
        t("side.hall.best", "<gray>Beste Runde:</gray> <au><n></au>", "<gray>Best round:</gray> <au><n></au>");
    }

    private static void shopTexts() {
        t("kraemer.sign", "<em><b>Krämer Wastl</b></em>\n<gray>Kramerladen – Rechtsklick</gray>",
                "<em><b>Wastl the shopkeeper</b></em>\n<gray>General store – right-click</gray>");
        t("shop.closed", "<gray>Der Kramerladen hat gerade zu.</gray>", "<gray>The general store is closed right now.</gray>");
        t("shop.not-now", "<gray>Der Kramerladen ist in der Gendarmerie – erst nach der Runde.</gray>",
                "<gray>The general store is at the police post – after the round.</gray>");
        t("shop.off", "<gray><white><item></white> gibt es gerade nicht.</gray>", "<gray><white><item></white> isn't available right now.</gray>");
        t("shop.already", "<gray><white><item></white> hast du schon an.</gray>", "<gray>You already have <white><item></white> on.</gray>");
        t("shop.equipped", "<em>✔ <item></em> <gray>ist jetzt dabei.</gray>", "<em>✔ <item></em> <gray>is now equipped.</gray>");
        t("shop.unequipped", "<gray><white><item></white> abgelegt – wieder das Standard-Stück.</gray>",
                "<gray>Took off <white><item></white> – back to the standard one.</gray>");
        t("shop.favourite", "<em>✔ <item></em> <gray>ist deine Lieblingsgestalt – damit startest du, wo es sie gibt.</gray>",
                "<em>✔ <item></em> <gray>is your favourite disguise – you start as it wherever it exists.</gray>");
        t("shop.favourite.off", "<gray>Keine Lieblingsgestalt mehr – du startest wieder zufällig.</gray>",
                "<gray>No favourite disguise any more – you start with a random one again.</gray>");
        t("shop.need", "<gray><white><item></white> gibt's erst ab <au><need></au>.</gray>",
                "<gray><white><item></white> unlocks at <au><need></au>.</gray>");
        t("shop.need.runden", "<n> Runden (du hast <have>)", "<n> rounds (you have <have>)");
        t("shop.need.funde", "<n> Funden (du hast <have>)", "<n> finds (you have <have>)");
        t("shop.need.unentdeckt", "<n> Runden unentdeckt (du hast <have>)", "<n> rounds never found (you have <have>)");
        t("shop.need.pfiffe", "<n> Pfiffen (du hast <have>)", "<n> whistles (you have <have>)");
        t("shop.need.siege", "<n> Siegen (du hast <have>)", "<n> wins (you have <have>)");
        t("shop.too-poor", "<gray>Für <white><item></white> fehlen dir noch <au><n> ✦</au> – ab in die nächste Runde!</gray>",
                "<gray>You need <au><n> ✦</au> more for <white><item></white> – off to the next round!</gray>");
        t("shop.confirm", "<gray><white><item></white> für <au><n> ✦</au>? <em>Nochmal klicken zum Kaufen.</em></gray>",
                "<gray><white><item></white> for <au><n> ✦</au>? <em>Click again to buy.</em></gray>");
        t("shop.bought", "<em>Gekauft: <item>!</em> <gray>−<n> ✦, Guthaben jetzt <au><balance> ✦</au>. Gleich ausgerüstet.</gray>",
                "<em>Bought: <item>!</em> <gray>−<n> ✦, balance now <au><balance> ✦</au>. Equipped right away.</gray>");
        t("shop.bought.shape", "<em>Gekauft: <item>!</em> <gray>−<n> ✦, Guthaben jetzt <au><balance> ✦</au>. Ab jetzt wählbar, wo es sie gibt – und deine Lieblingsgestalt.</gray>",
                "<em>Bought: <item>!</em> <gray>−<n> ✦, balance now <au><balance> ✦</au>. Now choosable wherever it exists – and your favourite disguise.</gray>");
        t("shop.owned", "<gray><white><item></white> hast du schon.</gray>", "<gray>You already own <white><item></white>.</gray>");
        t("tier.frei", "<em>Für alle</em>", "<em>For everyone</em>");
        t("tier.gewoehnlich", "<white>Gewöhnlich</white>", "<white>Common</white>");
        t("tier.selten", "<bl>Selten</bl>", "<bl>Rare</bl>");
        t("tier.praechtig", "<au>Prächtig</au>", "<au>Splendid</au>");
        t("menu.shop.title", "Kramerladen", "General store");
        t("menu.shop.title.kind", "Kramerladen · <kind>", "General store · <kind>");
        t("menu.shop.balance", "<au>Guthaben: <n> ✦</au>", "<au>Balance: <n> ✦</au>");
        t("menu.shop.balance.lore", "<gray>Punkte insgesamt: <white><total></white></gray>\n<gray>Siege in Folge: <white><streak></white> · beste Serie: <white><best></white></gray>\n<gray>Ausgegeben: <white><spent> ✦</white></gray>\n<gray>Tagesbonus: <white><daily></white></gray>",
                "<gray>Total points: <white><total></white></gray>\n<gray>Wins in a row: <white><streak></white> · best streak: <white><best></white></gray>\n<gray>Spent: <white><spent> ✦</white></gray>\n<gray>Daily bonus: <white><daily></white></gray>");
        t("menu.shop.balance.title", "<gray>Titel: <au><title></au></gray>", "<gray>Title: <au><title></au></gray>");
        t("menu.shop.daily.open", "heute noch offen (erster Sieg)", "still open today (first win)");
        t("menu.shop.daily.taken", "heute schon geholt", "already collected today");
        t("menu.shop.kind.gestalten", "<au>Gestalten</au>", "<au>Disguises</au>");
        t("menu.shop.kind.gestalten.desc", "<gray>Neue Gegenstände zum Verwandeln –</gray>\n<gray>nur auf Karten, wo es sie wirklich gibt.</gray>",
                "<gray>New things to turn into –</gray>\n<gray>only on maps where they really exist.</gray>");
        t("menu.shop.kind.pfiffe", "<au>Pfiff-Klänge</au>", "<au>Whistle sounds</au>");
        t("menu.shop.kind.pfiffe.desc", "<gray>So klingt dein Pfiff. Lautere Klänge</gray>\n<gray>geben mehr Punkte – man hört sie aber weiter.</gray>",
                "<gray>What your whistle sounds like. Louder sounds</gray>\n<gray>give more points – but carry further.</gray>");
        t("menu.shop.kind.fund-effekte", "<au>Fund-Effekte</au>", "<au>Find effects</au>");
        t("menu.shop.kind.fund-effekte.desc", "<gray>Das passiert, wenn du als Gendarm</gray>\n<gray>jemanden findest.</gray>",
                "<gray>What happens when you find</gray>\n<gray>someone as a constable.</gray>");
        t("menu.shop.kind.spuren", "<au>Spuren</au>", "<au>Trails</au>");
        t("menu.shop.kind.spuren.desc", "<gray>Eine Spur hinter dir beim Laufen –</gray>\n<gray>nur in der Gendarmerie, nie in der Runde.</gray>",
                "<gray>A trail behind you as you walk –</gray>\n<gray>only at the police post, never in a round.</gray>");
        t("menu.shop.kind.sieg-effekte", "<au>Sieg-Effekte</au>", "<au>Victory effects</au>");
        t("menu.shop.kind.sieg-effekte.desc", "<gray>Am Ende einer gewonnenen Runde</gray>\n<gray>über deinem Kopf.</gray>",
                "<gray>Above your head at the end</gray>\n<gray>of a round you won.</gray>");
        t("menu.shop.kind.titel", "<au>Titel</au>", "<au>Titles</au>");
        t("menu.shop.kind.titel.desc", "<gray>Im Steckbrief, in der Leiste der Runde</gray>\n<gray>und in der Rangliste.</gray>",
                "<gray>On your wanted poster, in the round's</gray>\n<gray>sidebar and in the ranking.</gray>");
        t("menu.shop.kind.have", "<gray>Freigeschaltet: <white><n></white> von <white><total></white></gray>",
                "<gray>Unlocked: <white><n></white> of <white><total></white></gray>");
        t("menu.shop.kind.equipped", "<gray>Dabei: <em><item></em></gray>", "<gray>Equipped: <em><item></em></gray>");
        t("menu.shop.kind.favourite", "<gray>Lieblingsgestalt: <em><item></em></gray>", "<gray>Favourite: <em><item></em></gray>");
        t("menu.shop.kind.click", "<em>▶ Klicken zum Anschauen</em>", "<em>▶ Click to browse</em>");
        t("menu.shop.how", "<em>So verdienst du Punkte</em>", "<em>How to earn points</em>");
        t("menu.shop.how.lore", "<gray>Als Gendarm: Treffer <au>+<hit></au>, Fund <au>+<find></au>,</gray>\n<gray>erster Fund der Runde <au>+<first></au>, Sieg <au>+<swin></au>.</gray>\n<gray>Versteckt: je halbe Minute <au>+<half></au>, Pfiff bis <au>+<wmax></au>,</gray>\n<gray>laut <au>+<loud></au> / sehr laut <au>+<vloud></au>, mutig (Gendarm keine <bblocks> Blöcke weg) bis <au>+<brave></au>,</gray>\n<gray>als Letzter versteckt <au>+<last></au>, bis zum Schluss <au>+<survive></au>.</gray>\n<gray>Danach: Siegesserie <au>+<streak></au> je Sieg in Folge,</gray>\n<gray>erster Sieg des Tages <au>+<daily></au>.</gray>\n<gray>Was übrig bleibt, kommt aufs Guthaben.</gray>",
                "<gray>As a constable: hit <au>+<hit></au>, find <au>+<find></au>,</gray>\n<gray>first find of the round <au>+<first></au>, win <au>+<swin></au>.</gray>\n<gray>Hiding: every half minute <au>+<half></au>, whistle up to <au>+<wmax></au>,</gray>\n<gray>loud <au>+<loud></au> / very loud <au>+<vloud></au>, brave (constable less than <bblocks> blocks away) up to <au>+<brave></au>,</gray>\n<gray>last one hidden <au>+<last></au>, until the end <au>+<survive></au>.</gray>\n<gray>Then: winning streak <au>+<streak></au> per win in a row,</gray>\n<gray>first win of the day <au>+<daily></au>.</gray>\n<gray>Whatever is left goes to your balance.</gray>");
        t("menu.shop.maps", "<gray>Auf: <white><list></white></gray>", "<gray>On: <white><list></white></gray>");
        t("menu.shop.loud.1", "<gray>Leise – hört man nicht so weit.</gray>", "<gray>Quiet – doesn't carry far.</gray>");
        t("menu.shop.loud.2", "<gray>Laut – <au>+<n> Punkte</au> je Pfiff, hört man weiter.</gray>",
                "<gray>Loud – <au>+<n> points</au> per whistle, carries further.</gray>");
        t("menu.shop.loud.3", "<gray>Sehr laut – <au>+<n> Punkte</au> je Pfiff, hört man überall.</gray>",
                "<gray>Very loud – <au>+<n> points</au> per whistle, heard everywhere.</gray>");
        t("menu.shop.need", "<gray>Erst ab <white><need></white></gray>", "<gray>Unlocks at <white><need></white></gray>");
        t("menu.shop.state.equipped", "<em>✔ Dabei</em>\n<gray>Klicken zum Ablegen</gray>", "<em>✔ Equipped</em>\n<gray>Click to take off</gray>");
        t("menu.shop.state.equipped.free", "<em>✔ Dabei</em>", "<em>✔ Equipped</em>");
        t("menu.shop.state.favourite", "<em>✔ Lieblingsgestalt</em>\n<gray>Klicken: wieder zufällig starten</gray>",
                "<em>✔ Favourite disguise</em>\n<gray>Click: start randomly again</gray>");
        t("menu.shop.state.owned", "<em>Freigeschaltet</em>\n<gray>▶ Klicken zum Ausrüsten</gray>", "<em>Unlocked</em>\n<gray>▶ Click to equip</gray>");
        t("menu.shop.state.owned.shape", "<em>Freigeschaltet</em>\n<gray>▶ Klicken: als Lieblingsgestalt wählen</gray>",
                "<em>Unlocked</em>\n<gray>▶ Click: make it your favourite</gray>");
        t("menu.shop.state.buy", "<au>Preis: <n> ✦</au>\n<em>▶ Klicken zum Kaufen</em>", "<au>Price: <n> ✦</au>\n<em>▶ Click to buy</em>");
        t("menu.shop.state.confirm", "<au>Preis: <n> ✦</au>\n<em>▶ Nochmal klicken – dann gehört es dir!</em>",
                "<au>Price: <n> ✦</au>\n<em>▶ Click again – and it's yours!</em>");
        t("menu.shop.state.poor", "<au>Preis: <n> ✦</au>\n<red>Dir fehlen noch <missing> ✦</red>", "<au>Price: <n> ✦</au>\n<red>You need <missing> ✦ more</red>");
        t("menu.shop.state.need", "<au>Preis: <n> ✦</au>\n<red>Noch nicht zu haben</red>", "<au>Price: <n> ✦</au>\n<red>Not available yet</red>");
        t("menu.shop.state.off", "<red>Gerade nicht im Laden</red>", "<red>Not in the store right now</red>");
    }

    private static void shopItems() {
        t("shop.pfiff.floete", "Flötenpfiff", "Flute whistle");
        t("shop.pfiff.floete.desc", "<gray>Zwei helle Flötentöne – der Klassiker.</gray>", "<gray>Two bright flute notes – the classic.</gray>");
        t("shop.pfiff.kuckuck", "Kuckuck", "Cuckoo");
        t("shop.pfiff.kuckuck.desc", "<gray>„Kuck-kuck, kuck-kuck“ aus dem Wald.</gray>", "<gray>\"Cuck-oo, cuck-oo\" from the woods.</gray>");
        t("shop.pfiff.henne", "Henne", "Hen");
        t("shop.pfiff.henne.desc", "<gray>Gegacker – und plopp, ein Ei.</gray>", "<gray>Clucking – and plop, an egg.</gray>");
        t("shop.pfiff.murmeltier", "Murmeltier-Pfiff", "Marmot whistle");
        t("shop.pfiff.murmeltier.desc", "<gray>Schriller Warnpfiff wie auf der Alm.</gray>", "<gray>A shrill alarm whistle like up on the alp.</gray>");
        t("shop.pfiff.kuhglocke", "Kuhglocke", "Cowbell");
        t("shop.pfiff.kuhglocke.desc", "<gray>Bimmel-bammel wie auf der Weide.</gray>", "<gray>Ding-dong like out on the pasture.</gray>");
        t("shop.pfiff.jodler", "Jodler", "Yodel");
        t("shop.pfiff.jodler.desc", "<gray>„Holla-di-ri-di“ – hoch und tief im Wechsel.</gray>", "<gray>\"Holla-dee-ree-dee\" – high and low in turn.</gray>");
        t("shop.pfiff.trompete", "Trompete", "Trumpet");
        t("shop.pfiff.trompete.desc", "<gray>Eine kleine Fanfare: Tä-tä-tä-täää!</gray>", "<gray>A little fanfare: ta-ta-ta-taaa!</gray>");
        t("shop.pfiff.alphorn", "Alphorn", "Alphorn");
        t("shop.pfiff.alphorn.desc", "<gray>Ein langer, tiefer Ruf über die Berge.</gray>", "<gray>A long, deep call across the mountains.</gray>");
        t("shop.fund.sternchen", "Grüne Sternchen", "Green sparkles");
        t("shop.fund.sternchen.desc", "<gray>Ein paar grüne Funkel – der Standard.</gray>", "<gray>A few green sparkles – the standard.</gray>");
        t("shop.fund.herzerl", "Herzerl", "Little hearts");
        t("shop.fund.herzerl.desc", "<gray>Herzen steigen auf, wo du jemanden findest.</gray>", "<gray>Hearts rise where you find someone.</gray>");
        t("shop.fund.rauchwolke", "Rauchwolke", "Puff of smoke");
        t("shop.fund.rauchwolke.desc", "<gray>Puff! Eine dicke Wolke.</gray>", "<gray>Poof! A big cloud.</gray>");
        t("shop.fund.konfetti", "Konfetti", "Confetti");
        t("shop.fund.konfetti.desc", "<gray>Bunte Schnipsel in alle Richtungen.</gray>", "<gray>Colourful bits in every direction.</gray>");
        t("shop.fund.blueten", "Blütenregen", "Blossom shower");
        t("shop.fund.blueten.desc", "<gray>Rosa Blütenblätter wirbeln.</gray>", "<gray>Pink petals swirl around.</gray>");
        t("shop.fund.weissblau", "Weiß-blaue Funken", "White-and-blue sparks");
        t("shop.fund.weissblau.desc", "<gray>Ein Ring aus weißen und blauen Funken.</gray>", "<gray>A ring of white and blue sparks.</gray>");
        t("shop.fund.goldregen", "Goldregen", "Golden rain");
        t("shop.fund.goldregen.desc", "<gray>Goldstaub rieselt herab.</gray>", "<gray>Gold dust trickles down.</gray>");
        t("shop.spur.keine", "Keine Spur", "No trail");
        t("shop.spur.keine.desc", "<gray>Du gehst, ohne Spuren zu hinterlassen.</gray>", "<gray>You walk without leaving a trace.</gray>");
        t("shop.spur.noten", "Notenspur", "Trail of notes");
        t("shop.spur.noten.desc", "<gray>Bunte Noten bei jedem Schritt.</gray>", "<gray>Colourful notes with every step.</gray>");
        t("shop.spur.herzerl", "Herzerlspur", "Trail of hearts");
        t("shop.spur.herzerl.desc", "<gray>Kleine Herzen hinter dir.</gray>", "<gray>Little hearts behind you.</gray>");
        t("shop.spur.schneeflocken", "Schneeflocken", "Snowflakes");
        t("shop.spur.schneeflocken.desc", "<gray>Es schneit, wo du gehst.</gray>", "<gray>It snows wherever you walk.</gray>");
        t("shop.spur.blueten", "Blütenblätter", "Petals");
        t("shop.spur.blueten.desc", "<gray>Rosa Blätter fallen hinter dir.</gray>", "<gray>Pink petals fall behind you.</gray>");
        t("shop.spur.funken", "Weiß-blaue Funken", "White-and-blue sparks");
        t("shop.spur.funken.desc", "<gray>Funken in Weiß und Blau.</gray>", "<gray>Sparks in white and blue.</gray>");
        t("shop.spur.gluehwuermchen", "Glühwürmchen", "Fireflies");
        t("shop.spur.gluehwuermchen.desc", "<gray>Kleine Lichter tanzen um dich herum.</gray>", "<gray>Tiny lights dance around you.</gray>");
        t("shop.sieg.keiner", "Kein Sieg-Effekt", "No victory effect");
        t("shop.sieg.keiner.desc", "<gray>Bescheiden gewinnen.</gray>", "<gray>Win modestly.</gray>");
        t("shop.sieg.glocken", "Glockenläuten", "Bells ringing");
        t("shop.sieg.glocken.desc", "<gray>Die Glocken läuten für dich.</gray>", "<gray>The bells ring for you.</gray>");
        t("shop.sieg.konfetti", "Konfetti-Regen", "Confetti shower");
        t("shop.sieg.konfetti.desc", "<gray>Bunte Schnipsel über deinem Kopf.</gray>", "<gray>Colourful bits above your head.</gray>");
        t("shop.sieg.goldregen", "Goldregen", "Golden rain");
        t("shop.sieg.goldregen.desc", "<gray>Gold rieselt auf dich herab.</gray>", "<gray>Gold trickles down on you.</gray>");
        t("shop.sieg.feuerwerk", "Feuerwerk", "Fireworks");
        t("shop.sieg.feuerwerk.desc", "<gray>Grün-goldenes Feuerwerk über dir.</gray>", "<gray>Green-and-gold fireworks above you.</gray>");
        t("shop.sieg.weissblau", "Weiß-blaue Fontäne", "White-and-blue fountain");
        t("shop.sieg.weissblau.desc", "<gray>Eine Fontäne aus weißen und blauen Funken.</gray>", "<gray>A fountain of white and blue sparks.</gray>");
        t("shop.titel.keiner", "Kein Titel", "No title");
        t("shop.titel.keiner.desc", "<gray>Nur dein Name.</gray>", "<gray>Just your name.</gray>");
        t("shop.titel.dorfkind", "Dorfkind", "Village kid");
        t("shop.titel.dorfkind.desc", "<gray>Hier kennt dich jeder.</gray>", "<gray>Everybody here knows you.</gray>");
        t("shop.titel.heustadl-held", "Heustadl-Held", "Hay barn hero");
        t("shop.titel.heustadl-held.desc", "<gray>Kein Heuballen ist vor dir sicher.</gray>", "<gray>No hay bale is safe from you.</gray>");
        t("shop.titel.pfeifkoenig", "Pfeifkönig", "Whistle royalty");
        t("shop.titel.pfeifkoenig.desc", "<gray>Für alle, die sich trauen zu pfeifen.</gray>", "<gray>For everyone who dares to whistle.</gray>");
        t("shop.titel.spuernase", "Spürnase", "Sharp nose");
        t("shop.titel.spuernase.desc", "<gray>Du findest sie alle.</gray>", "<gray>You find them all.</gray>");
        t("shop.titel.meister-der-tarnung", "Meister der Tarnung", "Master of disguise");
        t("shop.titel.meister-der-tarnung.desc", "<gray>Niemand hat dich je gesehen.</gray>", "<gray>Nobody has ever seen you.</gray>");
        t("shop.titel.oberwachtmeister", "Oberwachtmeister", "Chief constable");
        t("shop.titel.oberwachtmeister.desc", "<gray>Die Gendarmerie hört auf dein Kommando.</gray>", "<gray>The police post follows your lead.</gray>");
        t("shop.titel.dorflegende", "Dorflegende", "Village legend");
        t("shop.titel.dorflegende.desc", "<gray>Man erzählt sich Geschichten über dich.</gray>", "<gray>People tell stories about you.</gray>");
    }

    private static void helpTexts() {
        t("menu.helps.title", "Kramerkiste – Hilfen für die Runde", "Supply box – helps for this round");
        t("menu.helps.points", "<au>Übrig: <n> Punkte</au>", "<au>Left: <n> points</au>");
        t("menu.helps.points.lore", "<gray>In der Runde gesammelt: <white><earned></white></gray>\n<gray>Für Hilfen ausgegeben: <white><spent></white></gray>\n<gray>Was übrig bleibt, kommt nach der Runde aufs Guthaben.</gray>",
                "<gray>Collected this round: <white><earned></white></gray>\n<gray>Spent on helps: <white><spent></white></gray>\n<gray>Whatever is left goes to your balance after the round.</gray>");
        t("menu.helps.price", "<gray>Preis: <au><n> Punkte</au> aus der Runde</gray>", "<gray>Price: <au><n> points</au> from this round</gray>");
        t("menu.helps.uses", "<gray>Noch <white><n></white> von <white><max></white> in dieser Runde</gray>",
                "<gray><white><n></white> of <white><max></white> left this round</gray>");
        t("menu.helps.cooldown", "<gray>Danach <white><secs> s</white> warten</gray>", "<gray>Then wait <white><secs> s</white></gray>");
        t("menu.helps.state.ok", "<em>▶ Klicken: kaufen und gleich einsetzen</em>", "<em>▶ Click: buy and use right away</em>");
        t("menu.helps.state.off", "<red>Gerade abgeschaltet</red>", "<red>Switched off right now</red>");
        t("menu.helps.state.not-yet", "<gray>Erst, wenn die Suche läuft</gray>", "<gray>Only once the search is running</gray>");
        t("menu.helps.state.wrong-role", "<gray>Nicht für deine Seite</gray>", "<gray>Not for your side</gray>");
        t("menu.helps.state.not-needed", "<gray>Brauchst du gerade nicht – deins ist noch frei</gray>",
                "<gray>Not needed right now – yours is still unused</gray>");
        t("menu.helps.state.used-up", "<gray>In dieser Runde aufgebraucht</gray>", "<gray>Used up for this round</gray>");
        t("menu.helps.state.cooldown", "<gray>Gleich wieder – noch <white><secs> s</white></gray>", "<gray>Soon – <white><secs> s</white> to go</gray>");
        t("menu.helps.state.too-poor", "<red>Dir fehlen noch <n> Punkte</red>", "<red>You need <n> more points</red>");
        t("help.closed", "<gray>Die Kramerkiste ist in dieser Runde zu.</gray>", "<gray>The supply box is closed this round.</gray>");
        t("help.bought", "<em>✔ <help></em> <gray>für <au><n> Punkte</au> – noch <white><left></white> übrig.</gray>",
                "<em>✔ <help></em> <gray>for <au><n> points</au> – <white><left></white> left.</gray>");
        t("help.no.off", "<gray><white><help></white> gibt es gerade nicht.</gray>", "<gray><white><help></white> isn't available right now.</gray>");
        t("help.no.not-yet", "<gray><white><help></white> geht erst, wenn die Suche läuft.</gray>",
                "<gray><white><help></white> only works once the search is running.</gray>");
        t("help.no.wrong-role", "<gray><white><help></white> ist nicht für deine Seite.</gray>", "<gray><white><help></white> isn't for your side.</gray>");
        t("help.no.not-needed", "<gray><white><help></white> brauchst du gerade nicht – deins ist noch frei.</gray>",
                "<gray>You don't need <white><help></white> right now – yours is still unused.</gray>");
        t("help.no.used-up", "<gray><white><help></white> ist für diese Runde aufgebraucht.</gray>",
                "<gray><white><help></white> is used up for this round.</gray>");
        t("help.no.cooldown", "<gray><white><help></white> geht gleich wieder – noch <white><secs> s</white>.</gray>",
                "<gray><white><help></white> is ready again soon – <white><secs> s</white> to go.</gray>");
        t("help.no.too-poor", "<gray>Für <white><help></white> fehlen dir noch <au><n> Punkte</au> aus dieser Runde.</gray>",
                "<gray>You need <au><n> more points</au> from this round for <white><help></white>.</gray>");
        t("help.lockpfiff", "<em>Lockpfiff</em>", "<em>Decoy whistle</em>");
        t("help.lockpfiff.desc", "<gray>Dein Pfiff erklingt ein Stück weiter weg –</gray>\n<gray>lockt die Gendarmen auf die falsche Fährte.</gray>\n<gray>Keine Punkte, zählt nicht als Pflicht-Pfiff.</gray>",
                "<gray>Your whistle sounds a bit further away –</gray>\n<gray>sends the constables on a wild goose chase.</gray>\n<gray>No points, doesn't count as a forced whistle.</gray>");
        t("help.lockpfiff.done", "<em>♪ Der Lockpfiff ist unterwegs …</em>", "<em>♪ The decoy whistle is on its way …</em>");
        t("help.nebelwolke", "<em>Nebelwolke</em>", "<em>Fog cloud</em>");
        t("help.nebelwolke.desc", "<gray>Kurz Nebel um dich: Gendarmen darin sehen</gray>\n<gray>einen Moment nichts und kommen langsamer voran.</gray>",
                "<gray>A moment of fog around you: constables inside</gray>\n<gray>can't see for a second and move more slowly.</gray>");
        t("help.nebelwolke.done", "<em>Nebel! Jetzt schnell weg.</em>", "<em>Fog! Now get away quickly.</em>");
        t("help.nebelwolke.seeker", "<gray>Huch – Nebel! Hier hat sich jemand eingenebelt …</gray>",
                "<gray>Whoa – fog! Someone just vanished in a cloud …</gray>");
        t("help.gestaltwechsel", "<em>Noch ein Gestaltwechsel</em>", "<em>Another disguise change</em>");
        t("help.gestaltwechsel.desc", "<gray>Wenn dein einer Wechsel schon weg ist:</gray>\n<gray>noch einmal die Gestalt wechseln.</gray>",
                "<gray>When your one change is already used:</gray>\n<gray>change your disguise once more.</gray>");
        t("help.laterne", "<em>Laterne</em>", "<em>Lantern</em>");
        t("help.laterne.desc", "<gray>Ein paar Sekunden zeigen Funken, wer sich</gray>\n<gray>in der Nähe bewegt. Wer still hält oder</gray>\n<gray>eingerastet ist, bleibt unsichtbar.</gray>",
                "<gray>For a few seconds sparks show who's moving</gray>\n<gray>nearby. Anyone who keeps still or is</gray>\n<gray>locked in place stays hidden.</gray>");
        t("help.laterne.done", "<em>Die Laterne leuchtet – <secs> s lang!</em>", "<em>The lantern shines – for <secs> s!</em>");
        t("help.laterne.hider", "<au>Ein Gendarm leuchtet mit der Laterne – halt still!</au>",
                "<au>A constable is shining a lantern – keep still!</au>");
        t("help.spuernase", "<em>Noch eine Spürnase</em>", "<em>Another sniff</em>");
        t("help.spuernase.desc", "<gray>Wenn deine Spürnase schon benutzt ist:</gray>\n<gray>noch einmal schnuppern.</gray>",
                "<gray>When your sniffer nose is already used:</gray>\n<gray>sniff once more.</gray>");
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
    public static String plain(String lang, String key, TagResolver... resolvers) {
        return net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                .serialize(MM.deserialize(raw(lang, key), resolvers));
    }
}
