# Morni Versteckerl

Der Versteck-Modus des Minecraft-Netzwerks **MoinMorni** (moinmorni.net): **Verstecken mit Verwandlung.** Die
Versteckten werden zu einem Gegenstand der Karte – Heuballen, Fass, Blumentopf, Kürbis … –, die Dorfgendarmen müssen
sie finden. 4 bis 16 Leute. Ein Paper-Plugin für Paper 26.x (Java 25), im Netzwerk und auch ganz allein.

Englischer Name: **Hide & Seek**. Kennung im Netzwerk: `versteckerl`.

## So wird gespielt

1. In der **Gendarmerie** (Wartebereich mit Gendarmeriehaus, Maibaum, Brunnen und einem Schaukasten je Karte) stimmt
   man mit dem **Kompass** über die Karte ab und sagt mit dem **grünen Farbstoff** „bereit“. Ab 4 Leuten läuft der
   Countdown von selbst, mit 2–3 Leuten, wenn alle bereit sind. Höchstens 16 Leute pro Runde, Partys bleiben zusammen.
2. Etwa ein Viertel wird **Dorfgendarm** (mindestens einer; wer am seltensten gesucht hat, kommt zuerst dran, Partys
   möglichst auf derselben Seite). Die anderen verstecken sich.
3. **Versteckzeit** (40 s): Die Versteckten werden unsichtbar und laufen als **Gestalt** herum – eine Block-Anzeige,
   die ihnen folgt. Die Gestalt wählt man im Truhen-Fenster (öffnet sich von selbst, erster Knopf). Wer **still
   steht** (3 s), **rastet ein**: dann steht dort ein echter, fester Block – nicht von den Attrappen ringsum zu
   unterscheiden. Bewegt man sich, springt er auf. Die Gendarmen warten blind in ihrer Stube unter der Karte.
4. **Suchzeit** (5 Min.): Die Gendarmen schlagen mit dem **Gendarmenstab** auf alles Verdächtige. Ein Treffer auf
   einen Versteckten lässt dessen Block aufspringen und kostet ihn ein Herz (3 Herzen = gefunden). **Ein Schlag auf ein
   echtes Ding** der gleichen Art (ein echtes Fass, ein echter Heuballen …) **kostet den Gendarmen Leben** – wer zu oft
   danebenhaut, muss kurz verschnaufen.
5. **Gefundene werden Gendarmen** (Ansage für alle). Ist bei Ablauf noch jemand versteckt, gewinnen die Versteckten,
   sonst die Gendarmen. Danach: Rangliste mit Punkten, Aufdecken (übrige Gestalten leuchten), zurück in die
   Gendarmerie.

### Hilfen (Schnellleiste, Rechtsklick – geht auch auf Bedrock)

| Wer | Hilfe | Was sie tut |
|---|---|---|
| Versteckte | **Gestalt** | in der Versteckzeit beliebig oft wählen, in der Suchzeit **einmal** wechseln |
| Versteckte | **Pfiff** | der eigene Pfiff-Klang und Noten an der eigenen Stelle; gibt Punkte – je später, je lauter und je näher ein Gendarm, desto mehr. Ab der Hälfte der Suchzeit **Pflicht**: wer 30 s nicht gepfiffen hat, pfeift von selbst (ohne Punkte) |
| Versteckte | **Sprungkraft** | 4 s höher springen (Abklingzeit 25 s) |
| Gendarmen | **Spürnase** | einmal pro Runde: 4 s eine goldene Spur in Richtung des nächsten Versteckten (nur für einen selbst) |
| beide | **Kramerkiste** | der Rundenladen: kleine Hilfen für Punkte aus der laufenden Runde (siehe unten) |

## Punkte, Guthaben und Kramerladen

Nur Punkte aus dem Spiel – **nichts davon gibt es für echtes Geld**. Zwei Zähler: **Punkte insgesamt** (alles je
Verdiente, sinkt nie – Steckbrief und Bestenliste) und das **Guthaben** zum Einkaufen.

**In der Runde** (alle Zahlen in der `config.yml`, Abschnitt `punkte`):

| Wer | wofür | Punkte |
|---|---|---|
| Gendarm | Treffer · Fund · erster Fund der Runde · Sieg (jeder Gendarm am Ende) | 2 · 10 · +5 · 5 |
| Versteckte | je halbe Minute unentdeckt · als Letzter noch versteckt · bis zum Schluss | 1 · 10 · 20 |
| Versteckte | freiwilliger Pfiff: 1, je halbe Minute Suche einer mehr (höchstens 10) | 1–10 |
| Versteckte | … lauter Pfiff-Klang · sehr lauter Pfiff-Klang | +2 · +4 |
| Versteckte | … mutig: ein Gendarm steht höchstens 12 bzw. 6 Blöcke weg | +2 · +4 |

**Am Ende der Runde – die Abrechnung:** Was man in der Runde gesammelt und nicht im Rundenladen ausgegeben hat, kommt
aufs Guthaben. Dazu der **Serien-Bonus** (je weiterem Sieg in Folge +3, höchstens +15) und einmal am Tag der
**Tagesbonus** für den ersten Sieg (+25). Im Chat steht die Abrechnung Zeile für Zeile; bis es zurück in die
Gendarmerie geht, bleibt sie auch in der Leiste rechts stehen, danach zeigt die Leiste das Guthaben. Wer eine Runde
verlässt, behält, was er gesammelt hat – die Siegesserie ist dann aber vorbei.

**Rundenladen – die Kramerkiste** (Schnellleiste oder `/laden`, nur in der Suchzeit, bezahlt mit Punkten der Runde;
jede Hilfe begrenzt und mit Gegenmittel):

| Wer | Hilfe | Preis | Grenze | Was sie tut |
|---|---|---|---|---|
| Versteckte | **Lockpfiff** | 6 | 2× je Runde, 30 s Pause | der eigene Pfiff erklingt 7–12 Blöcke weiter weg (keine Punkte, kein Pflicht-Pfiff) |
| Versteckte | **Nebelwolke** | 8 | 1× je Runde | 4 s Nebel um einen: Gendarmen darin sehen 1,5 s nichts und werden langsam |
| Versteckte | **Noch ein Gestaltwechsel** | 10 | 1× je Runde | wenn der eine Wechsel schon weg ist |
| Gendarmen | **Laterne** | 8 | 2× je Runde, 40 s Pause | 5 s lang zeigen Funken, wer sich in 16 Blöcken bewegt – wer still hält oder eingerastet ist, bleibt verborgen; Versteckte in der Nähe bekommen einen Hinweis |
| Gendarmen | **Noch eine Spürnase** | 12 | 1× je Runde | wenn die eine Spürnase schon benutzt ist |

**Kramerladen in der Gendarmerie** – bei **Krämer Wastl** am Stand mit der rot-weißen Markise (Rechtsklick auf ihn),
mit dem Smaragd in der Schnellleiste oder `/laden`. Truhen-Fenster (auch für Bedrock). Kaufen braucht zwei Klicks (der
erste zeigt Preis und eine Vorschau), Gekauftes ist gleich ausgerüstet; ein Klick auf Freigeschaltetes rüstet aus
bzw. legt ab. Alles bleibt in der Spielerdatei. Jeder Preis und jedes Stück an/aus steht in der `config.yml`
(Abschnitt `laden`).

| Abteilung | Stücke (Preis in Punkten) |
|---|---|
| **Gestalten** – nur auf Karten mit genug echten Attrappen; auf Wunsch Lieblingsgestalt (damit startet man) | gewöhnlich: Werkbank, Schleifstein, Webstuhl (150) · selten: Blühender Busch, Lesepult, Kuchen (300) · prächtig: Kürbislaterne, Festtagskuchen (600) |
| **Pfiff-Klänge** – lautere geben mehr Punkte, hört man aber weiter | Flötenpfiff (frei) · leise: Kuckuck, Henne (80) · laut: Murmeltier-Pfiff, Kuhglocke (160) · sehr laut: Jodler, Trompete, Alphorn (280) |
| **Fund-Effekte** – wenn man als Gendarm jemanden findet | Grüne Sternchen (frei) · Herzerl, Rauchwolke (100) · Konfetti, Blütenregen (200) · Weiß-blaue Funken, Goldregen (350) |
| **Spuren** – nur in der Gendarmerie, nie in der Runde | keine (frei) · Notenspur, Herzerlspur (100) · Schneeflocken, Blütenblätter (200) · Weiß-blaue Funken, Glühwürmchen (350) |
| **Sieg-Effekte** – über dem Kopf nach einer gewonnenen Runde | keiner (frei) · Glockenläuten (120) · Konfetti-Regen, Goldregen (220) · Feuerwerk, Weiß-blaue Fontäne (400) |
| **Titel** – im Steckbrief, in der Leiste der Runde und in der Rangliste | ☘ Dorfkind (60) · ✿ Heustadl-Held (120) · ♪ Pfeifkönig (200, ab 50 Pfiffen) · ✪ Spürnase (250, ab 25 Funden) · ❀ Meister der Tarnung (300, ab 15 Runden unentdeckt) · ⚑ Oberwachtmeister (500, ab 75 Funden) · ★ Dorflegende (800, ab 100 Runden) |

Nichts davon hilft mehr als das Freie: neue Gestalten sind nur weitere Möglichkeiten (die Gendarmen sehen im
Karten-Menü, welche es gibt, und ein Schlag auf ein echtes Stück kostet sie Leben wie bei jeder Gestalt), Klänge und
Effekte sind nur zum Anschauen und Anhören – ein lauter Pfiff bringt mehr Punkte, verrät einen aber auch eher.

## Karten (eigene Entwürfe, aus dem Code gebaut)

| Karte | Was es gibt | Gestalten |
|---|---|---|
| Karte | Was es gibt | Gestalten | aus dem Kramerladen |
|---|---|---|---|
| **Dorfplatz** | Bauerndorf mit Brunnen und weiß-blauem Maibaum, Wirtshaus mit Biergarten, Stadel voller Heu, Kramerladen mit Markise, offene Schmiede, Obstwiese mit Bienenstöcken, Holzlege, Gärtchen, Heumandl | Heuballen, Fass, Blumentopf, Kürbis, Amboss, Bienenstock, Laterne, Hackstock, Kessel | Werkbank, Schleifstein, Webstuhl, Blühender Busch, Kuchen, Kürbislaterne |
| **Wirtshaus** | großes Gasthaus von innen: Gaststube mit Schänke, Bierfass-Regal, Stammtisch und Kachelofen, Küche, Stube mit Bücherwand, Bierkeller mit Gewölbe, Tanzsaal und Gästezimmer; draußen Biergarten, Kegelbahn, Hinterhof | Fass, Bierfass, Blumentopf, Laterne, Kessel, Ofen, Bücherregal, Bank | Werkbank, Blühender Busch, Lesepult, Kuchen, Festtagskuchen |
| **Heustadel** | Bauernhof mit großem Stadel (Tenne, Ställe, zwei Heuböden mit Leitern), Bauernhaus, Ziehbrunnen, Heuwagen, Gemüsegarten mit Kürbissen und Melonen, Bienenwiese mit Bienenhaus, Kompostecke, Hühnerstall, Obstwiese | Heuballen, Fass, Kürbis, Melone, Bienenstock, Komposter, Hackstock, Laterne, Kessel | Werkbank, Schleifstein, Webstuhl, Blühender Busch, Kürbislaterne |

Von jeder Gestalt stehen auf ihrer Karte viele echte Attrappen (`MapPlanTest` prüft mindestens 6, bei den Gestalten
aus dem Kramerladen mindestens 6 genau gleich aussehende). Alles entsteht beim ersten Start in der eigenen leeren Welt
`versteckerl`; auf jeder Karte läuft höchstens eine Runde gleichzeitig.

## Befehle

| Befehl | Was er tut |
|---|---|
| `/karte [dorfplatz\|wirtshaus\|heustadel]` | über die Karte abstimmen (ohne Angabe: Menü) – auch `/map`, `/vote` |
| `/bereit` | in der Gendarmerie bereit melden – auch `/ready` |
| `/gestalt` | Gestalt wählen bzw. einmal wechseln – auch `/shape` |
| `/zuschauen [spieler\|karte]` | einer Runde zuschauen – auch `/spectate` |
| `/verlassen` | Runde oder Zuschauen verlassen – auch `/leave` |
| `/laden` | in der Gendarmerie der Kramerladen, in der Runde die Kramerkiste (Hilfen) – auch `/shop`, `/kramerladen`, `/hilfen` |
| `/stats [spieler]` · `/top` | Statistik und Steckbriefe (Bestenlisten) |
| `/versteckerladmin info\|status\|start [karte]\|zeit <sek> [karte]\|sucher <namen…\|weg>\|ende [karte]\|guthaben <spieler> <zahl\|+zahl\|-zahl>\|geld [spieler] <betrag>\|punkte [spieler] <betrag>\|laden <spieler>\|neubauen\|neuladen` | Verwaltung und Tests (`moinmorni.versteckerl.admin`, auch `/vkadmin`); `guthaben` setzt oder ändert das Guthaben (nie unter 0), `geld` gibt Guthaben für den Kramerladen und `punkte` Rundenpunkte in der laufenden Runde (ohne Spieler: einem selbst – so geben sich Owner und Admins zum Testen etwas), `laden` zeigt Guthaben, Freigeschaltetes und Ausgerüstetes |

Einstellungen stehen in der `config.yml` (Countdown, Zeiten, Anteil der Gendarmen, Treffer bis gefunden, Schaden für
falsche Treffer, Pfiff-Pflicht, Hilfen, Punkte, Rundenladen, Preise und an/aus je Stück im Kramerladen …).

**Zurück ins Spiel:** Verliert jemand mitten in einer Runde die Verbindung (zum Beispiel, weil das Spiel abstürzt),
bleibt sein Platz `rueckkehr-sekunden` lang frei (ganz oben in der `config.yml`, Vorgabe 90, 0 = aus). Kommt er
rechtzeitig wieder – direkt oder über „Zurück ins Spiel“ in der Lobby –, geht es genau dort weiter: gleiche Rolle,
gleiche Gestalt, gleiches Versteck (der Block rastet wieder ein), gleiche Punkte. Solange ist seine Gestalt
verschwunden (niemand sucht etwas, das nicht gefunden werden kann), die Runde wartet auf ihn; wer bei Ablauf der
Suchzeit noch weg ist, zählt nicht als versteckt. Zu spät: Er hat die Runde verlassen wie bisher.

## Im Netzwerk

Das Netzwerk-Plugin („MoinMorni“) stellt die Schnittstelle `de.moinmorni.api.MoinMorni` bereit; Versteckerl nutzt
sie, wenn sie da ist (`softdepend`): Sprache der Spieler, Party (zusammen in eine Runde; wer später kommt, hilft in
der laufenden Runde seiner Party als Gendarm mit), Statistik, Zustand für Lobby-Figur und Webseite, zurück zur Lobby.
Ab Schnittstelle 2 meldet Versteckerl, wer gerade eine Runde spielt (dann fragt „/lobby“ erst nach), das Bett in der
Gendarmerie fragt über das Netzwerk, und ein freigehaltener Platz erscheint in der Lobby als „Zurück ins Spiel“; mit
einem älteren Netzwerk-Plugin läuft alles wie bisher.
Ohne Netzwerk: Sprache aus dem Spiel (englischer Client → Englisch), Statistik nur lokal, „zur Lobby“ trennt die
Verbindung mit einem Hinweis.

Statistik nach jeder Runde (`stats("versteckerl", …)`, höher ist besser):

- hochgezählt: `games`, `wins_hider`, `wins_seeker`, `found` (gefunden als Gendarm), `survived`, `whistles`, `points`
  (in der Runde gesammelt), `earned` (aufs Guthaben, mit Boni), `bonus` (Serien- und Tagesbonus), `helps` (Hilfen aus
  dem Rundenladen)
- Bestwerte: `best_points`, `best_streak` (Siege in Folge)
- beim Kauf im Kramerladen: `unlocks` (+1) und `spent` (Preis)

Zustand: `waiting` oder `running`, dazu die Spielerzahl. Lokal: `spieler/<uuid>.yml` (dazu Guthaben, Punkte
insgesamt, Freigeschaltetes, Ausgerüstetes, Serie, Tagesbonus) und `bestenliste.yml` (Steckbriefe: beste Runde,
Punkte insgesamt, meiste Funde, am öftesten unentdeckt).

Bei jedem Push auf `main` baut GitHub ein Release mit `Morni-Versteckerl.jar` (Version aus `pom.xml`).

## Gut zu wissen

- Versteckte sind für alle außer Zuschauern ausgeblendet (auch aus der Tab-Liste), nur ihre Gestalt ist zu sehen. Was
  ein Schlag trifft, rechnet das Plugin selbst aus (Blick des Gendarmen, Gestalt-Kasten, Block dahinter).
- Bedrock (Geyser): Menüs sind Truhen-Fenster, Hilfen liegen in der Schnellleiste. Ob Geyser die mitlaufende
  Block-Anzeige zeigt, ist nicht geprüft – eingerastete Gestalten sind echte Blöcke und überall zu sehen.

## Bauen und testen

```bash
mvn package                      # Tests + target/Morni-Versteckerl.jar (Java 25)
mvn -Ppaper-26.1 compile         # passt der Code auch zu Paper 26.1?
test/run.sh                      # echter Test: Paper-Server + Test-Spieler (mineflayer)
```

`test/run.sh` baut das Plugin und ein Test-Netzwerk (`test/fake-network`: Bert spricht Englisch, Anna und Carl sind
eine Party, alle Meldungen landen in `calls.log`), richtet einen Paper-26.1.2-Server ein (offline, Port 25820, RCON
25920, kurze Zeiten, 2 Treffer bis gefunden, kleine Preise im Rundenladen) und schickt fünf Test-Spieler durch zwei
Runden (`test/versteckerl-test.cjs`): Dorfplatz (Versteckte gewinnen) und Heustadel (Gendarmen gewinnen) mit
Gestalt-Menü, Einrasten, falschem Treffer, Verschnaufen, Treffer auf Block und mitlaufende Gestalt, Pfiff (auch
mutig), Pflicht-Pfiff, Sprungkraft, Gestalt wechseln, Spürnase, Rundenladen (Lockpfiff mit Abklingzeit, noch ein
Gestaltwechsel, Spürnase, Laterne, Nebelwolke, zu wenig Punkte), Abrechnung, Kramerladen (Krämer Wastl, zu wenig
Guthaben, Kaufen mit Nachfrage, Ausrüsten, Titel mit Bedingung), Lieblingsgestalt und gesperrte Gestalt in der Runde,
Kuhglocke mit Extrapunkten, Siegesserie, Zuschauen, Party-Nachzügler, Statistik, Steckbriefe und Lobby. Danach startet
er den Server neu und prüft, ob Guthaben, Freigeschaltetes und Ausgerüstetes noch da sind (`SKIP_RESTART=1` lässt das
aus).

---

Verfasst mit Claude 🤖
