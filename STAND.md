# Stand – Morni Versteckerl

**Was es ist:** Der Versteck-Modus des Netzwerks morni.net (Paper-Plugin `MorniVersteckerl`, Java 25, Paper 26.x):
Verstecken mit Verwandlung für 4–16 Leute. Die Versteckten werden zu einem Gegenstand der Karte (Heuballen, Fass,
Blumentopf …), die Dorfgendarmen suchen sie. Repo `pmornhart/morni-versteckerl` (GitHub: `Morni-Versteckerl`), Jar
`Morni-Versteckerl.jar`, Netzwerk-Kennung `versteckerl`, englisch „Hide & Seek“.

## Aufbau (`src/main/java/net/morni/versteckerl/`)

- `VersteckerlPlugin` (Start, Welt, Takt), `Settings`, `Texts` (de/en), `Ui`, `Items`, `Holo`, `Sidebar`, `LangBar`,
  `Boards` (Leiste + Tafeln der Gendarmerie), `Commands` (inkl. `/versteckerladmin`).
- `rules/` (ohne Server, mit Tests): `Shape` (15 Gestalten, Blockzustand als Text, „gleiche Art“ für falsche Treffer),
  `Roles` (Gendarmen auslosen: Anteil, Fairness, Partys), `Points`, `Outcome`, `Whistle` (Pfiff-Pflicht), `HallRules`,
  `Ray` (Treffer-Rechnung Blick ↔ Kasten), `Votes`, `Groups`.
- `data/`: `PlayerData`, `Store`, `Leaderboard` (Steckbriefe: Punkte, Funde, unentdeckt).
- `world/` (Baupläne als Text, ohne Server prüfbar): `Plan`, `Blocks` (Zustände zerlegen, Zäune/Mauern verbinden),
  `Decor`, `Kit` (Alpenhaus, Dächer, Möbel), `HallPlan` (Gendarmerie), `DorfplatzPlan`, `WirtshausPlan`,
  `HeustadelPlan`, `MapType` + `MapLayout`, `Construction` (Pläne im Hintergrund, Blöcke Stück für Stück, `VERSION`),
  `VoidGenerator`.
- `game/`: `Game` (Versteckzeit, Suchzeit, Einrasten, Schläge, Hilfen, Ende, Sichtbarkeit), `Disguise`
  (Block-Anzeige + echter Block, Luft für den Besitzer), `Member`, `Gear`, `ShapeMenu`, `Games`.
- `hall/`: `Hall`, `HallMenus`. `menu/`: `Menu`, `MenuListener`. `listen/`: `PlayerListener`, `GameListener`
  (Arm-Schwung → Treffer-Rechnung), `GuardListener`. `net/`: `Network`, `Connected`, `Standalone` (Sprache aus dem Client).
- `test/`: `run.sh`, `versteckerl-test.cjs`, `rcon.cjs`, `fake-network/`.

## Stand (16.11.2026)

- Version 0.1.0 (Branch `claude/erster-stand`, wartet auf Philips Go): alles aus dem README.
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

- Philip: Bilder ansehen, ausprobieren; Go für `main` (dann Release v0.1.0).
- Netzwerk-Repo: `MODE_REPOS`-Eintrag, Lobby-Figur, Webseite (Modus-Seite, Statistik-Schlüssel), Einstellungs-Katalog
  (config.yml).
