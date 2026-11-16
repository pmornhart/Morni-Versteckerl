# Morni Versteckerl

Der Versteck-Modus des Minecraft-Netzwerks **morni.net**: **Verstecken mit Verwandlung.** Die
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
| Versteckte | **Pfiff** | Flöten-Pfiff und Noten an der eigenen Stelle; gibt Punkte – je später, desto mehr (1 bis 10). Ab der Hälfte der Suchzeit **Pflicht**: wer 30 s nicht gepfiffen hat, pfeift von selbst (ohne Punkte) |
| Versteckte | **Sprungkraft** | 4 s höher springen (Abklingzeit 25 s) |
| Gendarmen | **Spürnase** | einmal pro Runde: 4 s eine goldene Spur in Richtung des nächsten Versteckten (nur für einen selbst) |

### Punkte

Treffer 2, Fund 10, Gendarmen-Sieg 5 (jeder Gendarm am Ende) · Versteckte: je halbe Minute unentdeckt 1, freiwilliger
Pfiff 1–10, bis zum Schluss versteckt 20.

## Karten (eigene Entwürfe, aus dem Code gebaut)

| Karte | Was es gibt | Gestalten |
|---|---|---|
| **Dorfplatz** | Bauerndorf mit Brunnen und weiß-blauem Maibaum, Wirtshaus mit Biergarten, Stadel voller Heu, Kramerladen mit Markise, offene Schmiede, Obstwiese mit Bienenstöcken, Holzlege, Gärtchen, Heumandl | Heuballen, Fass, Blumentopf, Kürbis, Amboss, Bienenstock, Laterne, Hackstock, Kessel |
| **Wirtshaus** | großes Gasthaus von innen: Gaststube mit Schänke, Bierfass-Regal, Stammtisch und Kachelofen, Küche, Stube mit Bücherwand, Bierkeller mit Gewölbe, Tanzsaal und Gästezimmer; draußen Biergarten, Kegelbahn, Hinterhof | Fass, Bierfass, Blumentopf, Laterne, Kessel, Ofen, Bücherregal, Bank |
| **Heustadel** | Bauernhof mit großem Stadel (Tenne, Ställe, zwei Heuböden mit Leitern), Bauernhaus, Ziehbrunnen, Heuwagen, Gemüsegarten mit Kürbissen und Melonen, Bienenwiese mit Bienenhaus, Kompostecke, Hühnerstall, Obstwiese | Heuballen, Fass, Kürbis, Melone, Bienenstock, Komposter, Hackstock, Laterne, Kessel |

Von jeder Gestalt stehen auf ihrer Karte viele echte Attrappen (`MapPlanTest` prüft mindestens 6). Alles entsteht
beim ersten Start in der eigenen leeren Welt `versteckerl`; auf jeder Karte läuft höchstens eine Runde gleichzeitig.

## Befehle

| Befehl | Was er tut |
|---|---|
| `/karte [dorfplatz\|wirtshaus\|heustadel]` | über die Karte abstimmen (ohne Angabe: Menü) – auch `/map`, `/vote` |
| `/bereit` | in der Gendarmerie bereit melden – auch `/ready` |
| `/gestalt` | Gestalt wählen bzw. einmal wechseln – auch `/shape` |
| `/zuschauen [spieler\|karte]` | einer Runde zuschauen – auch `/spectate` |
| `/verlassen` | Runde oder Zuschauen verlassen – auch `/leave` |
| `/stats [spieler]` · `/top` | Statistik und Steckbriefe (Bestenlisten) |
| `/versteckerladmin info\|status\|start [karte]\|zeit <sek> [karte]\|sucher <namen…\|weg>\|ende [karte]\|neubauen\|neuladen` | Verwaltung und Tests (`morninet.versteckerl.admin`, auch `/vkadmin`) |

Einstellungen stehen in der `config.yml` (Countdown, Zeiten, Anteil der Gendarmen, Treffer bis gefunden, Schaden für
falsche Treffer, Pfiff-Pflicht, Hilfen …).

## Im Netzwerk

Das Netzwerk-Plugin („MorniNet“) stellt die Schnittstelle `net.morni.api.MorniNet` bereit; Versteckerl nutzt
sie, wenn sie da ist (`softdepend`): Sprache der Spieler, Party (zusammen in eine Runde; wer später kommt, hilft in
der laufenden Runde seiner Party als Gendarm mit), Statistik, Zustand für Lobby-Figur und Webseite, zurück zur Lobby.
Ohne Netzwerk: Sprache aus dem Spiel (englischer Client → Englisch), Statistik nur lokal, „zur Lobby“ trennt die
Verbindung mit einem Hinweis.

Statistik nach jeder Runde (`stats("versteckerl", …)`, höher ist besser):

- hochgezählt: `games`, `wins_hider`, `wins_seeker`, `found` (gefunden als Gendarm), `survived`, `whistles`, `points`
- Bestwert: `best_points`

Zustand: `waiting` oder `running`, dazu die Spielerzahl. Lokal: `spieler/<uuid>.yml` und `bestenliste.yml`
(Steckbriefe: beste Runde, meiste Funde, am öftesten unentdeckt).

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
25920, kurze Zeiten, 2 Treffer bis gefunden) und schickt fünf Test-Spieler durch zwei Runden
(`test/versteckerl-test.cjs`): Dorfplatz (Versteckte gewinnen) und Heustadel (Gendarmen gewinnen) mit Gestalt-Menü,
Einrasten, falschem Treffer, Verschnaufen, Treffer auf Block und mitlaufende Gestalt, Pfiff, Pflicht-Pfiff,
Sprungkraft, Gestalt wechseln, Spürnase, Zuschauen, Party-Nachzügler, Statistik, Steckbriefe und Lobby.

---

Verfasst mit Claude 🤖
