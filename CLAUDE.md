# Morni Versteckerl

@STAND.md

## Regeln für dieses Projekt

- Repo ist **öffentlich**: keine Geheimnisse, Tokens oder privaten Daten committen (Geheimes nur in `.env`, nie im Repo).
  Auch keine Test-Server-Ordner, Welten, Spielerdaten oder RCON-Passwörter.
- Der Modus heißt **Versteckerl** (englisch „Hide & Seek“), das Repo `pmornhart/morni-versteckerl`, die Jar
  `Morni-Versteckerl.jar`, das Plugin `MorniVersteckerl`, die Kennung im Netzwerk `versteckerl`.
- Texte im Spiel auf Deutsch (Standard) und Englisch, freundlich, keine Fachbegriffe ohne Erklärung. Alle Texte stehen
  in `Texts.java` (MiniMessage, beide Sprachen; `TextsTest` prüft Platzhalter, Zeichen, dass jeder Schlüssel aus dem
  Quelltext einen Text hat und dass jede Gestalt und jede Karte einen Namen hat).
- **„Verfasst mit Claude 🤖“** als letzte Zeile in Commit-Messages (durch eine Leerzeile getrennt), in PRs, Issues und
  Release-Notes; einmal als Fußzeile im README.
- Commits mit Stichpunkten `- Neu:` / `- Behoben:` / `- Verbessert:` / `- Entfernt:`.
- Keine fremden Marken, Figuren, Namen, Texte oder Grafiken (keine Mojang-, Nintendo-, Hypixel- oder Inhalte anderer
  Server): nur die Spielidee „Verstecken mit Verwandlung“, eigene Namen (Dorfgendarmen, Pfiff, Spürnase …), eigene
  Karten, eigene Bauten.
- Die Netzwerk-Schnittstelle `src/api/java/de/moinmorni/api/MoinMorni.java` ist eine Kopie aus `pmornhart/moinmorni.net`
  (`plugins/moinmorni-network/src/main/java/de/moinmorni/api/MoinMorni.java`) und muss **genau gleich** bleiben. Sie wird
  nur mitübersetzt und kommt **nicht** in die Jar. Nur `net/Connected` darf sie benutzen.
- Versteckerl muss auch ohne Netzwerk laufen (`softdepend: [MoinMorni]`; Sprache dann aus dem Client).
- Paper 26.x, Java 25. Der Code muss auch gegen Paper 26.1 übersetzen (`mvn -Ppaper-26.1 compile`, läuft in der CI).
  Blocknamen von 26.x benutzen (z. B. `iron_chain`, nicht `chain`; `short_grass`, nicht `grass`) – `MapPlanTest` prüft das.
- Regeln (Rollen, Punkte, Pfiff, Sieg, Abstimmung, Gruppen, Treffer-Rechnung) gehören in `rules/`, Daten in `data/` –
  ohne Server, mit Tests. Werte dort nur zusammen mit den Tests ändern.
- Baupläne (`world/`) arbeiten nur mit Blocknamen als Text – so prüft `MapPlanTest` ohne Server, dass jede Karte genug
  Attrappen jeder Gestalt hat, Startpunkte frei sind und die Gendarmenstube dicht ist.
- Ändern sich Gendarmerie oder Karten: `Construction.VERSION` erhöhen – dann baut der Server beim nächsten Start neu.
- Neue Version: `<version>` in `pom.xml` erhöhen und `docs/releases/v<version>.md` anlegen; ein Push auf `main` baut das
  Release mit `Morni-Versteckerl.jar` – das Netzwerk holt es sich von dort.
- Vor jeder Veröffentlichung (Merge nach `main`) Philips Go abwarten.
- Nach jeder sichtbaren Änderung Bilder wie im echten Minecraft (echte Client-Texturen nur lokal, nie ins Repo).
- „Alles im Panel“: jede Einstellung, die per Befehl geht, muss auch im Panel gehen. Darum hat Versteckerl nur die
  `config.yml` und Verwaltungs-Befehle zum Testen (`/versteckerladmin`); die Anbindung an den Einstellungs-Katalog
  gehört ins Netzwerk-Repo.
- Bedrock-Spieler (Geyser) müssen mitspielen können: Menüs nur als Truhen-Fenster, Hilfen in der Schnellleiste.
- Echter Test nach jeder Änderung am Spielablauf: `test/run.sh` (siehe README).
- `STAND.md` nach jedem größeren Schritt aktualisieren.
