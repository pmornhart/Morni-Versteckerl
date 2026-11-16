#!/usr/bin/env bash
# Echter Test von Morni Versteckerl: baut das Plugin und das Test-Netzwerk, richtet einen Paper-Server ein (offline,
# eigener Port, RCON an), startet ihn, schickt Test-Spieler (mineflayer) durch alle Abläufe, startet ihn neu (sind
# Guthaben und Freischaltungen noch da?) und hält ihn wieder an.
#
#   test/run.sh                 # alles
#   KEEP=1 test/run.sh          # Server danach laufen lassen (zum Nachschauen)
#   KEEP=1 SKIP_TEST=1 test/run.sh   # nur Server starten
#
# Erwartet (Vorgaben für die Sandbox von morni.net, alles per Umgebungsvariable änderbar):
#   VERSTECKERL_JAVA    Java 25 (Ordner; sonst die JDK 25 aus dem Cache, sonst JAVA_HOME)
#   PAPER_JAR     Paper 26.1.2 (mineflayer kann bis 26.1)
#   TEMPLATE      ein schon einmal gestarteter Paper-26.1.2-Ordner (libraries/, cache/, versions/ werden kopiert,
#                 damit nichts heruntergeladen werden muss) – optional
#   VERSTECKERL_NODE_MODULES  node_modules mit mineflayer
#   EXTRA_PLUGINS weitere Jars für den Server (z. B. ViaVersion + ViaBackwards für Paper 26.3)
#
# Paper 26.3 (wie im Netzwerk): mineflayer kann nur bis 26.1 und kommt über ViaBackwards hinein, darf sich dort aber
# nicht bewegen (NO_MOVE=1 überspringt die Prüfungen, die laufen müssen):
#   PAPER_JAR=…/paper-26.3.jar TEMPLATE=…/servers/t2 SERVER_DIR=…/servers/versteckerl-test-263 \
#   EXTRA_PLUGINS="…/ViaVersion-5.12.2.jar …/ViaBackwards-5.12.2.jar" NO_MOVE=1 test/run.sh
#
# Verfasst mit Claude 🤖
set -euo pipefail

HERE="$(cd "$(dirname "$0")" && pwd)"
REPO="$(dirname "$HERE")"
CACHE="${MM_CACHE:-/home/user/mm-cache}"
CACHED_JDK="$(ls -d "$CACHE"/jdk/jdk-25* 2>/dev/null | head -1 || true)"
JAVA_HOME="${VERSTECKERL_JAVA:-${CACHED_JDK:-${JAVA_HOME:-}}}"
export JAVA_HOME
JAVA="$JAVA_HOME/bin/java"
PAPER_JAR="${PAPER_JAR:-$CACHE/jars/paper-26.1.2.jar}"
TEMPLATE="${TEMPLATE:-$CACHE/servers/t1}"
SERVER="${SERVER_DIR:-$CACHE/servers/versteckerl-test}"
PORT="${PORT:-25820}"
RCON_PORT="${RCON_PORT:-25920}"
export NODE_PATH="${VERSTECKERL_NODE_MODULES:-$CACHE/node/node_modules}${NODE_PATH:+:$NODE_PATH}"

echo "== Bauen"
(cd "$REPO" && nice -n 15 mvn -q -B -DskipTests package)
(cd "$REPO" && nice -n 15 mvn -q -B -f test/fake-network/pom.xml package)

echo "== Server einrichten: $SERVER"
mkdir -p "$SERVER/plugins/MorniVersteckerl" "$SERVER/plugins/MorniNet"
if [ -d "$TEMPLATE" ] && [ ! -d "$SERVER/libraries" ]; then
  cp -r "$TEMPLATE/libraries" "$TEMPLATE/cache" "$TEMPLATE/versions" "$SERVER/" 2>/dev/null || true
fi
cp "$PAPER_JAR" "$SERVER/paper.jar"
echo "eula=true" > "$SERVER/eula.txt"
RCON_PASS="$(head -c 18 /dev/urandom | base64 | tr -dc 'A-Za-z0-9' | head -c 20)"
printf '%s' "$RCON_PASS" > "$SERVER/.rcon-pass"
chmod 600 "$SERVER/.rcon-pass"
cat > "$SERVER/server.properties" <<EOF
online-mode=false
enforce-secure-profile=false
server-port=$PORT
enable-rcon=true
rcon.port=$RCON_PORT
rcon.password=$RCON_PASS
level-name=world
level-type=minecraft\:flat
generator-settings={"layers"\:[{"block"\:"minecraft\:bedrock","height"\:1}],"biome"\:"minecraft\:plains"}
generate-structures=false
spawn-protection=0
view-distance=6
simulation-distance=4
max-players=20
white-list=false
enforce-whitelist=false
motd=Morni Versteckerl Test
allow-flight=true
EOF
# Testwerte: kurze Countdowns und Zeiten, zwei Treffer bis gefunden, ein falscher Treffer kostet zwei Herzen.
cat > "$SERVER/plugins/MorniVersteckerl/config.yml" <<'EOC'
welt: versteckerl
gendarmerie:
  countdown-sekunden: 6
  mindestens-spieler: 4
runde:
  hoechstens-spieler: 16
  sucher-anteil-prozent: 25
  versteck-sekunden: 40
  such-sekunden: 120
  nachspiel-sekunden: 4
  einrasten-sekunden: 2
  treffer-bis-gefunden: 2
  falscher-treffer-schaden: 4
  verschnaufen-sekunden: 3
hilfen:
  pfiff-abklingzeit-sekunden: 3
  pflicht-pfiff-ab-prozent: 25
  pflicht-pfiff-sekunden: 15
  sprung-sekunden: 4
  sprung-abklingzeit-sekunden: 10
  spuernase-sekunden: 4
# Rundenladen mit kleinen Testpreisen (in der Runde gibt es im Test nur wenige Punkte).
rundenladen:
  lockpfiff:
    preis: 1
    abklingzeit-sekunden: 20
  nebelwolke:
    preis: 3
  gestaltwechsel:
    preis: 2
  laterne:
    preis: 2
  spuernase:
    preis: 3
EOC
# Test-Netzwerk: Bert spricht Englisch, Anna und Carl sind eine Party (Anna führt).
cat > "$SERVER/plugins/MorniNet/fake.properties" <<'EOF'
lang.Bert=en
party.Anna=Anna,Carl
EOF
rm -f "$SERVER/plugins/MorniNet/calls.log"
rm -rf "$SERVER/plugins/MorniVersteckerl/spieler" "$SERVER/plugins/MorniVersteckerl/bestenliste.yml" "$SERVER/vk-laden-dora.txt"
cp "$REPO/target/Morni-Versteckerl.jar" "$SERVER/plugins/"
# nur ein Test-Netzwerk im Server (auch keine ältere Fassung unter anderem Namen)
rm -f "$SERVER"/plugins/*-FakeNetwork.jar
cp "$REPO/test/fake-network/target/MorniNet-FakeNetwork.jar" "$SERVER/plugins/"
for jar in ${EXTRA_PLUGINS:-}; do
  cp "$jar" "$SERVER/plugins/"
done

cd "$SERVER"
PID=""
start_server() {
  echo "== Server starten (Port $PORT, RCON $RCON_PORT)"
  : > console.log
  nice -n 15 "$JAVA" -Xms1G -Xmx2G -jar paper.jar --nogui < /dev/null > console.log 2>&1 &
  PID=$!
  # Bereit erst, wenn Versteckerl steht und RCON lauscht (ist die Welt schon gebaut, meldet sich Versteckerl vor RCON).
  for _ in $(seq 1 300); do
    if grep -q "Versteckerl ist bereit" console.log && grep -q "RCON running on" console.log; then break; fi
    if ! kill -0 "$PID" 2>/dev/null; then echo "Server ist abgestürzt:"; tail -40 console.log; exit 1; fi
    sleep 1
  done
  grep -q "Versteckerl ist bereit" console.log || { echo "Versteckerl wurde nicht bereit:"; tail -40 console.log; exit 1; }
  grep -q "RCON running on" console.log || { echo "RCON lauscht nicht:"; tail -40 console.log; exit 1; }
  grep -E "Fertig gebaut|Versteckerl ist bereit|Netzwerk" console.log || true
}
stop_server() {
  if [ -n "$PID" ] && kill -0 "$PID" 2>/dev/null; then
    node "$HERE/rcon.cjs" "127.0.0.1:$RCON_PORT" "$SERVER/.rcon-pass" stop >/dev/null 2>&1 || kill "$PID" || true
    for _ in $(seq 1 60); do kill -0 "$PID" 2>/dev/null || break; sleep 1; done
    kill -9 "$PID" 2>/dev/null || true
  fi
}
if [ "${KEEP:-0}" != "1" ]; then
  trap stop_server EXIT
fi
start_server

if [ "${SKIP_TEST:-0}" = "1" ]; then
  echo "Server läuft (PID $PID) – anhalten mit: node $HERE/rcon.cjs 127.0.0.1:$RCON_PORT $SERVER/.rcon-pass stop"
  exit 0
fi

echo "== Test-Spieler"
STATUS=0
NO_MOVE="${NO_MOVE:-0}" node "$HERE/versteckerl-test.cjs" 127.0.0.1 "$PORT" "127.0.0.1:$RCON_PORT" "$SERVER/.rcon-pass" "$SERVER" || STATUS=$?
errors() {
  if grep -qE "ERROR|Exception" console.log; then
    grep -E "ERROR|Exception" console.log | head -20
  else
    echo "keine"
  fi
}
echo "== Fehler im Server-Log:"
errors

if [ "${SKIP_RESTART:-0}" != "1" ]; then
  echo "== Neustart: Guthaben, Freischaltungen und Ausrüstung müssen bleiben"
  stop_server
  start_server
  node "$HERE/versteckerl-test.cjs" 127.0.0.1 "$PORT" "127.0.0.1:$RCON_PORT" "$SERVER/.rcon-pass" "$SERVER" --nach-neustart || STATUS=$?
  echo "== Fehler im Server-Log (nach dem Neustart):"
  errors
fi
exit $STATUS
