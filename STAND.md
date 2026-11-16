# Stand – Morni Versteckerl

**Was es ist:** Der Versteck-Modus des Netzwerks morni.net (Paper-Plugin `MorniVersteckerl`, Java 25, Paper 26.x):
Verstecken mit Verwandlung für 4–16 Leute. Die Versteckten werden zu einem Gegenstand der Karte (Heuballen, Fass,
Blumentopf …), die Dorfgendarmen suchen sie. Punkte aus den Runden kommen aufs Guthaben, damit kauft man bei Krämer
Wastl in der Gendarmerie Gestalten, Pfiff-Klänge, Effekte und Titel. Repo `pmornhart/morni-versteckerl` (GitHub:
`Morni-Versteckerl`), Jar `Morni-Versteckerl.jar`, Netzwerk-Kennung `versteckerl`, englisch „Hide & Seek“.

## Aufbau (`src/main/java/net/morni/versteckerl/`)

- `VersteckerlPlugin` (Start, Welt, Takt), `Settings`, `Texts` (de/en), `Ui`, `Items`, `Holo`, `Sidebar`, `LangBar`,
  `Boards` (Leiste + Tafeln der Gendarmerie), `Commands` (inkl. `/versteckerladmin`).
- `rules/` (ohne Server, mit Tests): `Shape` (15 freie + 8 Gestalten aus dem Laden, Blockzustand als Text, „gleiche
  Art“ für falsche Treffer, `Tier`), `Roles` (Gendarmen auslosen: Anteil, Fairness, Partys), `Points` (Vorgaben),
  `Rewards` (Punkte aus der config.yml: Pfiff mit Lautstärke und Mut, Letzter, erster Fund, Serie, Tagesbonus),
  `Settlement` (Abrechnung am Rundenende), `Helps` (Rundenladen: Regeln, Preis, Abklingzeit, je Runde), `Catalog`
  (alles aus dem Kramerladen in sechs Abteilungen), `Outcome`, `Whistle` (Pfiff-Pflicht), `HallRules`, `Ray`
  (Treffer-Rechnung Blick ↔ Kasten), `Votes`, `Groups`.
- `data/`: `PlayerData` (dazu Guthaben, Punkte insgesamt, Kauf in einem Schritt, Freigeschaltetes, Ausgerüstetes,
  Serie, Tagesbonus), `Store`, `Leaderboard` (Steckbriefe: beste Runde, Punkte insgesamt, Funde, unentdeckt).
- `world/` (Baupläne als Text, ohne Server prüfbar): `Plan`, `Blocks` (Zustände zerlegen, Zäune/Mauern verbinden),
  `Decor`, `Kit` (Alpenhaus, Dächer, Möbel), `HallPlan` (Gendarmerie), `DorfplatzPlan`, `WirtshausPlan`,
  `HeustadelPlan`, `MapType` + `MapLayout`, `Construction` (Pläne im Hintergrund, Blöcke Stück für Stück, `VERSION`),
  `VoidGenerator`.
- `game/`: `Game` (Versteckzeit, Suchzeit, Einrasten, Schläge, Hilfen, Rundenladen, Abrechnung, Ende, Sichtbarkeit),
  `Disguise` (Block-Anzeige + echter Block, Luft für den Besitzer), `Member`, `Gear`, `ShapeMenu` (auch gesperrte
  Laden-Gestalten), `HelpMenu` (Kramerkiste), `Flair` (Pfiff-Klänge, Fund-/Sieg-Effekte, Spuren aus Minecraft-Klängen
  und -Partikeln), `Games`.
- `hall/`: `Hall` (Spuren beim Laufen), `HallMenus`, `Shop` (Kaufen mit Nachfrage, Ausrüsten, Titel), `ShopMenus`
  (Kramerladen-Fenster), `Kraemer` (Krämer Wastl am Stand). `menu/`: `Menu`, `MenuListener` (offene Menüs werden jede
  Sekunde neu gezeichnet). `listen/`: `PlayerListener`, `GameListener` (Arm-Schwung → Treffer-Rechnung),
  `GuardListener`. `net/`: `Network`, `Connected`, `Standalone` (Sprache aus dem Client).
- `test/`: `run.sh` (mit Neustart-Teil), `versteckerl-test.cjs`, `rcon.cjs`, `fake-network/`.

## Stand (16.11.2026)

- **Version 0.2.0 – Punkte sammeln und einkaufen** (Philips Wunsch, Vorbild nur die Mechanik großer
  Hide-and-Seek-Server; Branch `claude/erster-stand`, wartet auf Philips Go): Punkte der Runde (mehr Anlässe: erster
  Fund, Letzter, mutiger und lauter Pfiff) → Abrechnung am Rundenende → Guthaben (dazu Siegesserie, Tagesbonus);
  „Punkte insgesamt“ sinken nie (Steckbrief, neue Bestenliste). Rundenladen „Kramerkiste“ (Lockpfiff, Nebelwolke, noch
  ein Gestaltwechsel; Laterne, noch eine Spürnase – bezahlt mit Rundenpunkten, begrenzt, mit Abklingzeit und
  Gegenmittel). Kramerladen bei Krämer Wastl (Stand mit Markise in der Gendarmerie, Smaragd, `/laden`): 8 Gestalten
  (Werkbank, Schleifstein, Webstuhl, Blühender Busch, Lesepult, Kuchen, Kürbislaterne, Festtagskuchen – je mindestens
  6 gleich aussehende Attrappen auf den passenden Karten), 7 Pfiff-Klänge, 6 Fund-Effekte, 6 Spuren, 5 Sieg-Effekte,
  7 Titel; Kauf mit Nachfrage und Vorschau, Ausrüsten im Menü, Lieblingsgestalt. Alle Zahlen, Preise und an/aus je
  Stück in der config.yml (`punkte`, `rundenladen`, `laden`). Abrechnung im Chat und – bis es zurück in die
  Gendarmerie geht – auch in der Leiste. Bau-Version 4.
- 40 Unit-Tests grün (neu: Punkte/Pfiff/Mut/Serie, Abrechnung, Rundenladen-Regeln, Katalog, Kauf atomar und nie
  negativ, Spielerdatei mit Guthaben, alte Dateien, config.yml = Vorgaben, Preise/an-aus aus der config, Texte aller
  Stücke, gleich aussehende Attrappen der Laden-Gestalten). Übersetzt gegen Paper 26.3 und 26.1.
- Echter Test (`test/run.sh`, Paper 26.1.2 + Test-Netzwerk, fünf Test-Spieler): **151/151** und nach einem Neustart
  des Servers **5/5**, Server-Log ohne Fehler – zusätzlich zu allem Bisherigen: Krämer Wastl (Rechtsklick öffnet den
  Laden, englisch), Smaragd, sechs Abteilungen, „So verdienst du Punkte“, zu wenig Guthaben, mutiger Pfiff (+4),
  Kramerkiste (Lockpfiff mit Abklingzeit, noch ein Gestaltwechsel, noch eine Spürnase, Laterne, zu wenig Punkte für die
  Nebelwolke, Nebelwolke macht die Gendarmin kurz blind), Abrechnung (Runde, Ausgaben, Tagesbonus, Guthaben, Punkte
  insgesamt, Leiste), Kauf mit Nachfrage (Werkbank, Kuhglocke, Titel), Lieblingsgestalt an/aus, Titel mit Bedingung,
  Netzwerk-Statistik (`earned`, `bonus`, `helps`, `best_streak`, `unlocks`, `spent`), Spielerdatei, Lieblingsgestalt
  Werkbank in der Runde (eingerastet als echte Werkbank), gesperrte Gestalt für andere, Kuhglocke +2, Siegesserie ohne
  zweiten Tagesbonus, Titel in der Rangliste, Abrechnung in der Leiste; nach dem Neustart Guthaben, Freigeschaltetes,
  Ausgerüstetes, Wastl und Tafeln wie vorher, `/verlassen` holt nach dem Zuschauermodus zurück in die Gendarmerie.
- Bilder mit echtem Client 26.3 (nur lokal unter `~/mm-cache/bilder-versteckerl/laden/`): Kramerstand mit Wastl (nah
  und weit), Spuren im Hof, Kramerladen (Übersicht, „So verdienst du Punkte“, Gestalten, Pfiff-Klänge mit Nachfrage
  und gekauft, Fund-Effekte, Titel mit Bedingung, Kauf im Chat), neue Gestalten zwischen echten Attrappen (Werkbank in
  der Schmiede, Kuchen am Tresen, Blühender Busch, Kürbislaterne), Fund-Effekt weiß-blau, Sieg-Feuerwerk, eine eigene
  Runde der Kamera (Pfiff mit Kuhglocke „+7 – mutig +4 · laut +2“, Kramerkiste, Lockpfiff, Abrechnung in Chat und
  Leiste). Dabei verbessert: Kuchen und Glocke verdeckten Wastls Gesicht und man sah seine Füße unter der Theke (jetzt
  Theke oben überstehend und unten zu, vor Wastl frei, Laternen an den Enden der Markise statt vor seinem Schild),
  Abrechnung zusätzlich in der Leiste (im Chat rutscht sie schnell weg), Schaukästen kürzer („+5 aus dem Laden“),
  Erklärung „mutig“ verständlicher; `/verlassen` holte nach dem Zuschauer-/Kreativmodus nicht zurück (sagte „schon in
  der Gendarmerie“, man war aber nicht mehr eingeplant). Hinweis für Bilder: der Kamera-Client braucht
  `--option chatScale:1.0`, sonst ist der Chat unsichtbar.
- Version 0.1.0: alles aus dem README (noch nicht veröffentlicht – das erste Release wird 0.2.0).
- 27 Unit-Tests grün (Texte, Rollen, Punkte, Sieg, Pfiff-Pflicht, Countdown-Regel, Treffer-Rechnung, Gestalten,
  Gruppen/Abstimmung, Spielerdaten, Steckbriefe, Baupläne: ≥ 6 Attrappen je Gestalt, freie Startpunkte, dichte
  Gendarmenstube, unsichtbarer Rand, nur 26.x-Blocknamen, Zäune/Mauern). Übersetzt gegen Paper 26.3 und 26.1.
- Echter Test (Paper 26.1.2 + Test-Netzwerk, fünf Test-Spieler): **95/95**, Server-Log ohne Fehler – Gendarmerie,
  Abstimmung, Countdown-Regeln (ab 4 / alle bereit), Rollen, Gestalt-Menü (de/en), Einrasten (echter Block, Besitzer
  sieht Luft, andere sehen die Anzeige nicht), falscher Treffer, Verschnaufen, Treffer auf Block und mitlaufende
  Gestalt, Gefundener wird Gendarm, Pfiff (Punkte, hörbar, Abklingzeit), Pflicht-Pfiff, Sprungkraft, Gestalt einmal
  wechseln, Spürnase einmal, Sieg der Versteckten und der Gendarmen, Netzwerk-Statistik, Spielerdateien, Steckbriefe,
  Zuschauen, Party-Nachzügler als Gendarm, Aufräumen, zweite Runde, Lobby.
- Bilder mit echtem Client 26.3 (über ViaVersion, nur lokal unter `~/mm-cache/bilder-versteckerl/`): Gendarmerie,
  alle drei Karten innen und außen, laufende Runden (Gestalten zwischen Attrappen, Aufdecken).
- Funde beim Bauen behoben: Plan-Bau blockierte den Server > 10 s (jetzt im Hintergrund), Einrasten auf Trampelpfad,
  Tanzsaal ohne Decke, rosa Putz, Tafeltitel unter dem Vordach.
- Nicht echt geprüft: Bedrock (Geyser zeigt Block-Anzeigen evtl. nicht), echte Spieler (Gleichgewicht, Zeiten),
  Paper 26.3 als Server, echter Hub.
- **Umbenannt in morni.net** (16.11., Philip): Paket `net.morni.versteckerl`, Schnittstelle `net.morni.api.MorniNet`
  (byte-gleich aus dem Netzwerk-Repo), `softdepend: [MorniNet]`, Recht `morninet.versteckerl.admin`, Texte und Leiste
  „morni.net“, Test-Netzwerk `MorniNet` (`plugins/MorniNet/`, `MorniNet-FakeNetwork.jar`). Plugin-Name, Datenordner
  (`plugins/MorniVersteckerl/`) und Kennung `versteckerl` bleiben – Spielerdaten gelten weiter. Unit-Tests,
  `-Ppaper-26.1 compile` und echter Test **95/95** grün (eine Prüfung wartet jetzt kurz, bis das Paket da ist, das
  Versteckte nach der Runde wieder zeigt – lief unter Last einmal zu früh).

## Offen

- Philip: Bilder ansehen, ausprobieren; Go für `main` (dann erstes Release v0.2.0).
- Einstellungs-Katalog im Netzwerk-Repo: neue Schlüssel `punkte.*`, `rundenladen.*`, `laden.*` (Liste im Bericht vom
  16.11.); Webseite/Statistik: neue Werte `earned`, `bonus`, `helps`, `best_streak`, `unlocks`, `spent`.
- Nicht echt geprüft: Gleichgewicht der Preise mit echten Spielern, Bedrock (Truhen-Menüs, Partikel), echter Hub.
- Netzwerk-Repo: `MODE_REPOS`-Eintrag, Lobby-Figur, Webseite (Modus-Seite, Statistik-Schlüssel), Einstellungs-Katalog
  (config.yml).
