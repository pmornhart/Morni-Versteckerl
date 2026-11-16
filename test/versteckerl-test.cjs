/**
 * Echter Test von Morni Versteckerl mit Test-Spielern (mineflayer) auf Paper 26.1.2 – gestartet von test/run.sh.
 *   node versteckerl-test.cjs <host> <port> <rcon-host:port> <rcon-passwort-datei> <server-ordner>
 *
 * Spieler: Anna (deutsch, Party-Leiterin mit Carl), Bert (englisch), Carl (deutsch, kommt in Runde 2 als
 * Party-Nachzügler), Dora (deutsch), Emil (schaut zu). Sprache und Party kommen vom Test-Netzwerk (test/fake-network),
 * das auch Statistik, Zustand und „zur Lobby“ in calls.log mitschreibt.
 *
 * Verfasst mit Claude 🤖
 */
const crypto = require('node:crypto');
const fs = require('node:fs');
const path = require('node:path');
const mineflayer = require('mineflayer');
const { Vec3 } = require('vec3');
const { rcon } = require('./rcon.cjs');

const [HOST = '127.0.0.1', PORT = '25820', RCON = '127.0.0.1:25920', PASS_FILE, SERVER] = process.argv.slice(2);
const PASS = fs.readFileSync(PASS_FILE, 'utf8').trim();
const VERSION = process.env.MC_VERSION ?? '26.1';
const HALL = { x: 0.5, y: 101, z: 7.5 };
const DORF = 1000;
const HEU = 3000;
const FLOOR = 100;

const results = [];
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));
function check(name, ok, detail = '') {
  results.push({ name, ok: Boolean(ok) });
  console.log(`${ok ? '✓' : '✗'} ${name}${detail ? ` – ${detail}` : ''}`);
}

/** Text aus Chat-Komponenten (String, JSON oder NBT) zusammensetzen. */
function plain(v) {
  if (v == null) return '';
  if (typeof v === 'string') {
    if (v.startsWith('{') || v.startsWith('[')) {
      try {
        return plain(JSON.parse(v));
      } catch {
        return v;
      }
    }
    return v;
  }
  if (Array.isArray(v)) return v.map(plain).join('');
  if (typeof v !== 'object') return String(v);
  if (v.type === 'compound') return plain(v.value);
  if (v.type === 'list') return plain(v.value?.value ?? []);
  if (v.type === 'string') return v.value;
  let s = '';
  if ('text' in v) s += plain(v.text);
  if ('' in v) s += plain(v['']);
  if ('translate' in v) s += plain(v.translate);
  if ('extra' in v) s += plain(v.extra);
  return s;
}

function offlineUuid(name) {
  const h = crypto.createHash('md5').update(`OfflinePlayer:${name}`, 'utf8').digest();
  h[6] = (h[6] & 0x0f) | 0x30;
  h[8] = (h[8] & 0x3f) | 0x80;
  const x = h.toString('hex');
  return `${x.slice(0, 8)}-${x.slice(8, 12)}-${x.slice(12, 16)}-${x.slice(16, 20)}-${x.slice(20)}`;
}

const cmd = (...commands) => rcon(RCON, PASS, commands);
const inWorld = (c) => `execute in minecraft:versteckerl run ${c}`;
const playerFile = (name) => path.join(SERVER, 'plugins/MorniVersteckerl/spieler', `${offlineUuid(name)}.yml`);
const readFile = (file) => (fs.existsSync(file) ? fs.readFileSync(file, 'utf8') : '');
const calls = () =>
  readFile(path.join(SERVER, 'plugins/MoinMorni/calls.log'))
    .split('\n')
    .filter(Boolean)
    .map((l) => JSON.parse(l));
const statsOf = (name) => calls().filter((c) => c.t === 'stats' && c.name === name);

async function waitFor(cond, ms = 5000, step = 100) {
  const start = Date.now();
  while (Date.now() - start < ms) {
    if (await cond()) return true;
    await sleep(step);
  }
  return Boolean(await cond());
}

let lastJoin = 0;
async function join(name) {
  const wait = lastJoin + 3500 - Date.now();
  if (wait > 0) await sleep(wait);
  lastJoin = Date.now();
  const bot = mineflayer.createBot({ host: HOST, port: Number(PORT), username: name, version: VERSION, auth: 'offline' });
  bot.chats = [];
  bot.bars = [];
  bot.titles = [];
  bot.subtitles = [];
  bot.sounds = [];
  bot.kickReason = null;
  bot.on('messagestr', (m, position) => (position === 'game_info' ? bot.bars : bot.chats).push(m));
  bot._client.on('action_bar', (p) => bot.bars.push(plain(p.text)));
  bot._client.on('set_title_text', (p) => bot.titles.push(plain(p.text)));
  bot._client.on('set_title_subtitle', (p) => bot.subtitles.push(plain(p.text)));
  bot.on('soundEffectHeard', (n) => bot.sounds.push(n));
  bot.on('kicked', (r) => {
    bot.kickReason = plain(r);
  });
  bot.on('error', (e) => console.log(`  (${name}: ${e.message})`));
  await new Promise((resolve, reject) => {
    bot.once('spawn', resolve);
    bot.once('kicked', (r) => reject(new Error(`${name} gekickt: ${plain(r)}`)));
    setTimeout(() => reject(new Error(`${name}: kein Spawn`)), 20000);
  });
  await sleep(1500);
  return bot;
}

const slot = (bot, i) => bot.inventory.slots[36 + i];
const near = (p, q, d) => Math.abs(p.x - q.x) < d && Math.abs(p.y - q.y) < d && Math.abs(p.z - q.z) < d;
const inHall = (bot) => near(bot.entity.position, HALL, 5) && bot.game.gameMode === 'adventure';
const onMap = (bot, originX) => Math.abs(bot.entity.position.x - originX) < 40;
const sees = (bot, re) => bot.chats.some((c) => re.test(c));
const sawBar = (bot, re) => bot.bars.some((c) => re.test(c));
const sawTitle = (bot, re) => bot.titles.some((t) => re.test(t)) || bot.subtitles.some((t) => re.test(t));
const clear = (...bots) =>
  bots.forEach((b) => {
    b.chats.length = 0;
    b.bars.length = 0;
    b.titles.length = 0;
    b.subtitles.length = 0;
    b.sounds.length = 0;
  });
const hasEffect = (bot, id) => Object.values(bot.entity.effects ?? {}).some((e) => e.id === id);
const JUMP_BOOST = 7;
const INVISIBILITY = 13;
const BLINDNESS = 14;

/** /versteckerladmin status als Werte: {phase, zeit, versteckt, sucher, sieger, members: {Anna: {role, shape, solid, lives, points, finds, whistles, cell}}} */
async function status() {
  const lines = (await cmd('versteckerladmin status'))[0].split('\n');
  const games = {};
  for (const line of lines) {
    if (!line.startsWith('karte=')) continue;
    const s = { members: {} };
    for (const part of line.split(' ')) {
      const [k, v] = part.split('=');
      if (/^[A-Z]/.test(k)) {
        const [role, shape, solid, lives, points, finds, whistles, cell, online] = v.split(':');
        s.members[k] = {
          role, shape, solid: solid === '1', lives: Number(lives), points: Number(points), finds: Number(finds),
          whistles: Number(whistles), cell: cell === '-' ? null : cell.split(',').map(Number), online: online === 'on',
        };
      } else {
        s[k] = /^-?\d+$/.test(v) ? Number(v) : v;
      }
    }
    games[s.karte] = s;
  }
  return games;
}

async function openWithItem(bot, hotbar) {
  bot.setQuickBarSlot(hotbar);
  await sleep(300);
  const opened = new Promise((resolve) => {
    bot.once('windowOpen', resolve);
    setTimeout(() => resolve(null), 4000);
  });
  bot.activateItem();
  return opened;
}

async function useItem(bot, hotbar) {
  bot.setQuickBarSlot(hotbar);
  await sleep(300);
  bot.activateItem();
  await sleep(500);
}

async function closeWindow(bot) {
  if (bot.currentWindow) bot.closeWindow(bot.currentWindow);
  await sleep(250);
}

/** Im offenen Fenster den Gegenstand mit diesem Namen anklicken. */
async function clickNamed(bot, re) {
  const win = bot.currentWindow;
  if (!win) return false;
  const idx = win.slots.findIndex((s, i) => s && i < win.inventoryStart && re.test(plain(s.customName)));
  if (idx < 0) return false;
  await bot.clickWindow(idx, 0, 0);
  await sleep(500);
  return true;
}

/** Spieler per Befehl hinstellen (Blickrichtung wie angegeben) und warten, bis der Test-Spieler dort ist. */
async function tp(bot, name, x, y, z, yaw = 0, pitch = 0) {
  await cmd(inWorld(`tp ${name} ${x} ${y} ${z} ${yaw} ${pitch}`));
  await waitFor(() => near(bot.entity.position, { x, y, z }, 1.2), 3000);
  await sleep(500);
}

/** Auf einen Punkt schauen und zuschlagen (Arm schwingen) – das Spiel rechnet den Treffer selbst aus. */
async function strike(bot, x, y, z) {
  await bot.lookAt(new Vec3(x, y, z), true);
  await sleep(250);
  bot.swingArm('right');
  await sleep(400);
}

async function countDisplays(x, z, r = 40) {
  const out = String(await cmd(inWorld(`execute positioned ${x} ${FLOOR + 1} ${z} if entity @e[type=block_display,tag=versteckerl_gestalt,distance=..${r}]`)));
  const m = out.match(/(\d+)/);
  return m ? Number(m[1]) : 0;
}

async function blockIs(x, y, z, name) {
  const out = String(await cmd(inWorld(`execute if block ${x} ${y} ${z} ${name}`)));
  return /passed/i.test(out);
}

async function main() {
  // ------------------------------------------------------------------ Gendarmerie
  await sleep(6000);
  const holos = String(await cmd(inWorld(`execute positioned 0 ${FLOOR + 1} 0 if entity @e[type=text_display,distance=..30]`)));
  check('Tafeln der Gendarmerie stehen (4 Tafeln × 2 Sprachen)', /: 8\b/.test(holos), holos.trim());
  const anna = await join('Anna');
  const bert = await join('Bert');
  check('Ankunft in der Gendarmerie', inHall(anna), JSON.stringify(anna.entity.position));
  check('Schnellleiste: Kompass, Farbstoff, Papier, Bett',
    slot(anna, 0)?.name === 'compass' && slot(anna, 1)?.name === 'gray_dye' && slot(anna, 4)?.name === 'paper' &&
      slot(anna, 8)?.name === 'red_bed', [0, 1, 4, 8].map((i) => slot(anna, i)?.name).join(','));
  check('Deutsch: „Karte wählen“', plain(slot(anna, 0)?.customName).includes('Karte wählen'), plain(slot(anna, 0)?.customName));
  check('Englisch (Sprache vom Netzwerk): „Vote for a map“', plain(slot(bert, 0)?.customName).includes('Vote for a map'),
    plain(slot(bert, 0)?.customName));
  check('Begrüßung und Erklärung im Chat', sees(anna, /Gendarmerie/) && sees(anna, /rastet ein/));
  check('Leiste rechts (Scoreboard) vorhanden', await waitFor(() => Object.keys(anna.scoreboards ?? {}).length > 0, 3000));
  check('Netzwerk: Zustand „waiting“', calls().some((c) => c.t === 'state' && c.mode === 'versteckerl' && c.state === 'waiting'));

  // ------------------------------------------------------------------ Abstimmung
  let win = await openWithItem(anna, 0);
  check('Kompass öffnet „Karte wählen“', win && plain(win.title).includes('Karte wählen'), win && plain(win.title));
  check('Drei Karten im Menü', ['Dorfplatz', 'Wirtshaus', 'Heustadel'].every((n, i) =>
    plain(win?.slots[[11, 13, 15][i]]?.customName).includes(n)), [11, 13, 15].map((i) => plain(win?.slots[i]?.customName)).join(' / '));
  check('Karte zeigt ihre Gestalten', win?.slots[11]?.customLore?.map(plain).join(' ').includes('Heuballen'),
    win?.slots[11]?.customLore?.map(plain).join(' | '));
  await anna.clickWindow(11, 0, 0);
  await sleep(600);
  check('Stimme für den Dorfplatz', sees(anna, /Deine Stimme: Dorfplatz/));
  bert.chat('/karte dorfplatz');
  await sleep(600);
  check('Bert stimmt per Befehl (englisch)', sees(bert, /Your vote: Village Square/));
  check('Kein Countdown mit 2 von 4 (nicht alle bereit)', await waitFor(() => sawBar(anna, /Warte auf Mitspieler/), 3000) &&
    !anna.bars.some((b) => /Nächste Runde/.test(b)), anna.bars.slice(-1)[0]);
  win = await openWithItem(anna, 4);
  check('Steckbriefe: Statistik-Menü', win && plain(win.title).includes('Steckbriefe'), win && plain(win.title));
  await closeWindow(anna);

  // ------------------------------------------------------------------ Runde 1: Dorfplatz
  const carl = await join('Carl');
  const dora = await join('Dora');
  await cmd('versteckerladmin sucher Dora');
  check('Ab 4 Leuten läuft der Countdown von selbst', await waitFor(() => anna.bars.some((b) => /Nächste Runde in \d+ s · Karte: Dorfplatz/.test(b)), 4000),
    anna.bars.slice(-1)[0]);
  await useItem(carl, 1);
  check('Farbstoff: Carl ist bereit', sees(carl, /Du bist bereit/));
  const started = await waitFor(() => onMap(anna, DORF) && onMap(dora, DORF), 15000);
  check('Runde startet: alle auf dem Dorfplatz', started, JSON.stringify(anna.entity.position));
  check('Netzwerk: Zustand „running“', await waitFor(() => calls().some((c) => c.t === 'state' && c.state === 'running'), 3000));
  let s = (await status()).dorfplatz;
  check('Rollen: Dora sucht, drei verstecken sich', s?.phase === 'HIDE' && s.members.Dora?.role === 'SEEKER' &&
    ['Anna', 'Bert', 'Carl'].every((n) => s.members[n]?.role === 'HIDER'), JSON.stringify(s?.members));
  check('Ansage der Rollen im Chat', sees(anna, /Dorfgendarmen: Dora · 3 verstecken sich/));
  check('Titel: „Versteck dich!“ und „Du bist Dorfgendarm!“', await waitFor(() => sawTitle(anna, /Versteck dich/) &&
    sawTitle(dora, /Dorfgendarm/), 3000));
  check('Englischer Titel: „Go and hide!“', sawTitle(bert, /Go and hide/));
  check('Dora wartet in der Gendarmenstube (unter der Karte, blind)', dora.entity.position.y < FLOOR - 4 &&
    hasEffect(dora, BLINDNESS), `${JSON.stringify(dora.entity.position)} ${JSON.stringify(dora.entity.effects)}`);
  check('Versteckte sind unsichtbar', hasEffect(anna, INVISIBILITY));
  check('Versteckte haben Gestalt, Pfiff, Sprungkraft', slot(anna, 0)?.name === 'armor_stand' &&
    slot(anna, 1)?.name === 'amethyst_shard' && slot(anna, 2)?.name === 'rabbit_foot', [0, 1, 2].map((i) => slot(anna, i)?.name).join(','));
  check('Jeder Versteckte ist eine Block-Anzeige', (await countDisplays(DORF, 0)) === 3);
  const hiddenFromEachOther = !Object.values(anna.entities).some((e) => e.username === 'Bert' || e.username === 'Carl');
  check('Versteckte (Spieler) sieht niemand – nur ihre Gestalt', hiddenFromEachOther &&
    Object.values(anna.entities).filter((e) => e.name === 'block_display').length >= 2);

  // Gestalt wählen (Menü öffnet sich von selbst)
  check('Gestalt-Menü öffnet sich von selbst', await waitFor(() => anna.currentWindow && plain(anna.currentWindow.title).includes('Gestalt'), 3000),
    anna.currentWindow && plain(anna.currentWindow.title));
  const shapeNames = anna.currentWindow ? anna.currentWindow.slots.filter((x, i) => x && i < anna.currentWindow.inventoryStart)
    .map((x) => plain(x.customName)) : [];
  check('Neun Gestalten des Dorfplatzes', ['Heuballen', 'Fass', 'Blumentopf', 'Kürbis', 'Amboss', 'Bienenstock', 'Laterne',
    'Hackstock', 'Kessel'].every((n) => shapeNames.includes(n)), shapeNames.join(','));
  await clickNamed(anna, /^Fass$/);
  check('Anna wird ein Fass', (await status()).dorfplatz.members.Anna.shape === 'fass' && sees(anna, /Neue Gestalt: Fass/),
    anna.chats.slice(-2).join(' | '));
  await waitFor(() => bert.currentWindow, 2000);
  check('Englisches Menü: „Choose your disguise“ mit „Hay bale“', bert.currentWindow &&
    plain(bert.currentWindow.title).includes('Choose your disguise') && (await clickNamed(bert, /^Hay bale$/)),
    bert.currentWindow && plain(bert.currentWindow.title));
  await waitFor(() => carl.currentWindow, 2000);
  await clickNamed(carl, /^Kürbis$/);
  s = (await status()).dorfplatz;
  check('Bert ist ein Heuballen, Carl ein Kürbis', s.members.Bert.shape === 'heuballen' && s.members.Carl.shape === 'kuerbis',
    `${s.members.Bert.shape} ${s.members.Carl.shape}`);
  await closeWindow(anna);
  await closeWindow(bert);
  await closeWindow(carl);

  // Einrasten: Bert und Anna stehen still
  const B = { x: DORF + 3, y: FLOOR + 1, z: -3 };
  const A = { x: DORF - 3, y: FLOOR + 1, z: 3 };
  await tp(bert, 'Bert', B.x + 0.5, B.y, B.z + 0.5, 0, 0);
  await tp(anna, 'Anna', A.x + 0.5, A.y, A.z + 0.5, 0, 0);
  carl.setControlState('jump', true);
  await sleep(3200);
  s = (await status()).dorfplatz;
  check('Bert rastet ein: echter Heuballen am Platz', s.members.Bert.solid && String(s.members.Bert.cell) === String([B.x, B.y, B.z]) &&
    (await blockIs(B.x, B.y, B.z, 'hay_block')), JSON.stringify(s.members.Bert));
  check('Anna rastet ein: echtes Fass', s.members.Anna.solid && (await blockIs(A.x, A.y, A.z, 'barrel')));
  check('Hinweis „Eingerastet“', sawBar(bert, /Locked in/) && sawBar(anna, /Eingerastet/), bert.bars.slice(-2).join(' | '));
  check('Bert selbst sieht an seiner Stelle Luft (kann sich frei bewegen)', bert.blockAt(new Vec3(B.x, B.y, B.z))?.name === 'air',
    bert.blockAt(new Vec3(B.x, B.y, B.z))?.name);
  check('Carl hüpft herum und rastet nicht ein', !s.members.Carl.solid);
  check('Andere sehen die Anzeige eingerasteter Gestalten nicht (nur den echten Block)', !Object.values(carl.entities).some((e) =>
    e.name === 'block_display' && e.position.distanceTo(new Vec3(B.x + 0.5, B.y, B.z + 0.5)) < 0.8));

  // ------------------------------------------------------------------ Suchzeit
  clear(anna, bert, carl, dora);
  await cmd('versteckerladmin zeit 1');
  check('Die Suche beginnt', await waitFor(async () => (await status()).dorfplatz?.phase === 'SEEK', 4000) &&
    await waitFor(() => sawTitle(dora, /Suche beginnt/) && sawTitle(bert, /search begins/), 3000));
  check('Dora kommt aus der Stube an den Ortseingang, nicht mehr blind', await waitFor(() => onMap(dora, DORF) &&
    dora.entity.position.y > FLOOR && Math.abs(dora.entity.position.z - 27.5) < 2, 3000) && !hasEffect(dora, BLINDNESS),
    JSON.stringify(dora.entity.position));
  check('Gendarm hat Gendarmenstab und Spürnase', slot(dora, 0)?.name === 'stick' && slot(dora, 1)?.name === 'bone');
  check('Versteckte: „Gestalt wechseln (1×)“', plain(slot(anna, 0)?.customName).includes('Gestalt wechseln'),
    plain(slot(anna, 0)?.customName));

  // Falscher Treffer: echter Kessel am Brunnen
  await tp(dora, 'Dora', DORF + 4.5, FLOOR + 1, 4.5, 180, 30);
  const hpBefore = dora.health;
  await strike(dora, DORF + 4.5, FLOOR + 1.5, 2.5);
  // 4 = zwei Herzen; zwischendurch kann ein halbes Herz nachwachsen
  check('Falscher Treffer (echter Kessel) kostet Leben', await waitFor(() => dora.health <= hpBefore - 3, 2000) &&
    sawBar(dora, /Daneben!.*Kessel/), `${hpBefore} → ${dora.health} ${dora.bars.filter((b) => /Daneben/.test(b)).join(' | ')}`);
  for (let i = 0; i < 5; i++) {
    await sleep(500);
    await strike(dora, DORF + 4.5, FLOOR + 1.5, 2.5);
  }
  check('Zu oft daneben: Gendarm muss verschnaufen (volle Leben, langsam)', await waitFor(() => sawTitle(dora, /Puh/), 3000) &&
    dora.health >= 19, `${dora.health} ${dora.titles.join('|')}`);
  await sleep(3500);
  check('Wieder bei Puste', await waitFor(() => sawBar(dora, /Wieder bei Puste/), 3000));

  // Treffer auf Berts Block
  await tp(dora, 'Dora', B.x + 0.5, FLOOR + 1, B.z - 3.5, 0, 20);
  await strike(dora, B.x + 0.5, B.y + 0.5, B.z + 0.5);
  s = (await status()).dorfplatz;
  check('Treffer: Berts Block springt auf, ein Leben weniger', s.members.Bert.lives === 1 && !s.members.Bert.solid &&
    (await blockIs(B.x, B.y, B.z, 'air')), JSON.stringify(s.members.Bert));
  check('Bert merkt den Treffer, Dora bekommt Punkte', await waitFor(() => sawBar(bert, /Hit!/), 2000) &&
    s.members.Dora.points > 0, `${bert.bars.slice(-1)[0]} ${s.members.Dora.points}`);
  await sleep(800);
  await strike(dora, B.x + 0.5, B.y + 0.5, B.z + 0.5);
  s = (await status()).dorfplatz;
  check('Zweiter Treffer: Bert ist gefunden und wird Gendarm', s.members.Bert.role === 'SEEKER' && s.members.Dora.finds === 1,
    JSON.stringify(s.members));
  check('Ansage für alle (deutsch und englisch)', await waitFor(() => sees(anna, /Dora hat Bert gefunden – als Heuballen/) &&
    sees(bert, /Dora found Bert – disguised as a Hay bale/), 2000), anna.chats.slice(-1)[0]);
  check('Bert ist sichtbar und hat jetzt Stab und Spürnase', await waitFor(() => slot(bert, 0)?.name === 'stick', 2000) &&
    !hasEffect(bert, INVISIBILITY) && Object.values(dora.entities).some((e) => e.username === 'Bert'));

  // Hilfen der Versteckten
  clear(anna, carl, dora);
  await useItem(anna, 1);
  s = (await status()).dorfplatz;
  check('Pfiff: Punkte für Anna', s.members.Anna.whistles === 1 && s.members.Anna.points >= 1 && sawBar(anna, /Fiu-fiu/),
    `${JSON.stringify(s.members.Anna)} ${anna.bars.slice(-1)[0]}`);
  // Den Namen des Tons kann mineflayer 26.1 nicht immer richtig zuordnen (Liste weicht ab) – ein Notenblock-Ton genügt.
  check('Die Gendarmen hören den Pfiff', await waitFor(() => dora.sounds.some((n) => /note_block/.test(n)), 2000), dora.sounds.join(','));
  await useItem(anna, 1);
  check('Pfiff hat eine Abklingzeit', sawBar(anna, /Gleich wieder/), anna.bars.slice(-1)[0]);
  carl.setControlState('jump', false);
  await sleep(300);
  await useItem(carl, 2);
  check('Sprungkraft', await waitFor(() => hasEffect(carl, JUMP_BOOST), 2000) && sawBar(carl, /Sprungkraft für/));
  win = await openWithItem(anna, 0);
  check('Gestalt wechseln (einmal): Menü', win && plain(win.title).includes('Gestalt wechseln'), win && plain(win.title));
  await clickNamed(anna, /^Laterne$/);
  s = (await status()).dorfplatz;
  check('Anna wird eine Laterne – auch der echte Block wechselt', s.members.Anna.shape === 'laterne' &&
    (await blockIs(A.x, A.y, A.z, 'lantern')), JSON.stringify(s.members.Anna));
  win = await openWithItem(anna, 0);
  check('Ein zweiter Wechsel geht nicht', !win && sees(anna, /schon gewechselt/));
  await closeWindow(anna);

  // Spürnase
  await useItem(dora, 1);
  check('Spürnase: Spur zum nächsten Versteckten', sees(dora, /Spürnase zuckt/), dora.chats.slice(-1)[0]);
  await useItem(dora, 1);
  check('Spürnase nur einmal pro Runde', sees(dora, /schon benutzt/));

  // Carl wird gefunden, bevor er einrastet (Treffer auf die mitlaufende Gestalt)
  carl.setControlState('jump', true);
  await sleep(2600);
  const cp = carl.entity.position.clone();
  await tp(dora, 'Dora', cp.x, FLOOR + 1, cp.z + 2.5, 180, 25);
  carl.setControlState('jump', false);
  await sleep(800);
  await strike(dora, carl.entity.position.x, FLOOR + 1.5, carl.entity.position.z);
  s = (await status()).dorfplatz;
  check('Treffer auf die mitlaufende Gestalt (Carl nicht eingerastet)', s.members.Carl.lives === 1 && !s.members.Carl.solid,
    JSON.stringify(s.members.Carl));
  await sleep(700);
  await strike(dora, carl.entity.position.x, FLOOR + 1.5, carl.entity.position.z);
  s = (await status()).dorfplatz;
  check('Carl ist gefunden', s.members.Carl.role === 'SEEKER' && s.members.Dora.finds === 2, JSON.stringify(s.members.Carl));

  // Pflicht-Pfiff: Anna pfeift zu lange nicht → pfeift von selbst
  check('Pflicht zum Pfeifen wird angesagt', await waitFor(() => sees(anna, /muss jeder Versteckte mindestens alle/), 30000),
    anna.chats.slice(-2).join(' | '));
  check('Pflicht-Pfiff kommt von selbst (ohne Punkte)', await waitFor(() => sees(anna, /Pflicht-Pfiff/), 30000),
    anna.chats.slice(-2).join(' | '));

  // Ende: Zeit um, Anna ist noch versteckt → die Versteckten gewinnen
  clear(anna, bert, carl, dora);
  await cmd('versteckerladmin zeit 1');
  check('Zeit um: „Die Versteckten gewinnen!“', await waitFor(() => sawTitle(anna, /Versteckten gewinnen/), 4000) &&
    sawTitle(bert, /hiders win/) && sawTitle(anna, /Gewonnen/));
  check('Rangliste im Chat', sees(dora, /1\. .* Punkte/) && sees(anna, /Deine Punkte:/));
  check('Annas Gestalt leuchtet zum Aufdecken', await countDisplays(DORF, 0) === 1);
  await sleep(1000);
  let st = statsOf('Anna').at(-1);
  check('Netzwerk-Statistik Anna: Spiel, Sieg als Versteckte, überlebt, Pfiffe, Punkte',
    st?.mode === 'versteckerl' && st.add.games === 1 && st.add.wins_hider === 1 && st.add.survived === 1 &&
      st.add.whistles >= 1 && st.add.points > 20 && st.best.best_points === st.add.points, JSON.stringify(st));
  st = statsOf('Dora').at(-1);
  check('Netzwerk-Statistik Dora: zwei gefunden, kein Sieg', st?.add.found === 2 && st.add.wins_seeker === 0 &&
    st.add.wins_hider === 0, JSON.stringify(st));
  check('Netzwerk-Statistik Bert: gefunden → kein Sieg', statsOf('Bert').at(-1)?.add.survived === 0);
  check('Zurück in der Gendarmerie', await waitFor(() => [anna, bert, carl, dora].every(inHall), 9000),
    [anna, bert, carl, dora].map((b) => JSON.stringify(b.entity.position)).join(' '));
  check('Aufgeräumt: keine Gestalten mehr, Annas Platz ist wieder leer', (await countDisplays(DORF, 0)) === 0 &&
    (await blockIs(A.x, A.y, A.z, 'air')) && (await blockIs(B.x, B.y, B.z, 'air')));
  check('Alle wieder sichtbar und ohne Unsichtbarkeit', !hasEffect(anna, INVISIBILITY) &&
    Object.values(dora.entities).some((e) => e.username === 'Anna'));
  check('Spielerdatei mit Sieg', /siege-versteckt: 1/.test(readFile(playerFile('Anna'))) &&
    /gefunden: 2/.test(readFile(playerFile('Dora'))), readFile(playerFile('Anna')).replace(/\n/g, ' '));
  check('Steckbriefe (Bestenliste) gespeichert', /funde:[\s\S]*Dora/.test(readFile(path.join(SERVER, 'plugins/MorniVersteckerl/bestenliste.yml'))));

  // ------------------------------------------------------------------ Runde 2: Heustadel – Gendarmen gewinnen
  clear(anna, bert, carl, dora);
  carl.quit();
  await sleep(1000);
  for (const b of [anna, bert, dora]) b.chat('/karte heustadel');
  await sleep(800);
  await cmd('versteckerladmin sucher Anna');
  check('Mit 3 von 4 kein Countdown …', !anna.bars.slice(-3).some((b) => /Nächste Runde/.test(b)));
  for (const b of [anna, bert, dora]) b.chat('/bereit');
  check('… aber wenn alle bereit sind', await waitFor(() => anna.bars.some((b) => /Nächste Runde in \d s · Karte: Heustadel/.test(b)), 4000),
    anna.bars.slice(-1)[0]);
  check('Runde 2 auf dem Heustadel', await waitFor(() => onMap(bert, HEU) && onMap(anna, HEU), 12000));
  s = (await status()).heustadel;
  check('Anna sucht, Bert und Dora verstecken sich', s?.members.Anna?.role === 'SEEKER' && s.members.Bert?.role === 'HIDER' &&
    s.members.Dora?.role === 'HIDER', JSON.stringify(s?.members));
  check('Heustadel-Gestalten (englisch): Hay bale … Compost bin', await waitFor(() => bert.currentWindow, 2000) &&
    bert.currentWindow.slots.some((x) => /Compost bin/.test(plain(x?.customName))));
  await clickNamed(bert, /^Pumpkin$/);
  await waitFor(() => dora.currentWindow, 2000);
  await clickNamed(dora, /^Melone$/);
  // Zuschauer
  const emil = await join('Emil');
  emil.chat('/zuschauen heustadel');
  check('Emil schaut zu (Zuschauermodus, sieht die Versteckten)', await waitFor(() => emil.game.gameMode === 'spectator' &&
    onMap(emil, HEU), 4000) && sees(emil, /Du schaust Heustadel zu/) &&
    await waitFor(() => Object.values(emil.entities).some((e) => e.username === 'Bert'), 3000));
  // Party: Carl kommt zurück und hilft als Gendarm
  const carl2 = await join('Carl');
  check('Party-Nachzügler Carl hilft als Gendarm', await waitFor(() => sees(carl2, /Deine Party spielt gerade/), 4000) &&
    await waitFor(async () => (await status()).heustadel?.members.Carl?.role === 'SEEKER', 3000));
  check('Alle erfahren es', sees(anna, /Carl hilft jetzt als Gendarm mit/));
  const P = { x: HEU - 12, y: FLOOR + 1, z: 5 };
  const Q = { x: HEU + 10, y: FLOOR + 1, z: 7 };
  await tp(bert, 'Bert', P.x + 0.5, P.y, P.z + 0.5);
  await tp(dora, 'Dora', Q.x + 0.5, Q.y, Q.z + 0.5);
  await sleep(2800);
  s = (await status()).heustadel;
  check('Bert (Kürbis) und Dora (Melone) eingerastet', s.members.Bert.solid && s.members.Dora.solid &&
    (await blockIs(P.x, P.y, P.z, 'carved_pumpkin')) && (await blockIs(Q.x, Q.y, Q.z, 'melon')), JSON.stringify(s.members));
  await cmd('versteckerladmin zeit 1');
  await waitFor(async () => (await status()).heustadel?.phase === 'SEEK', 4000);
  await sleep(800);
  clear(anna, bert, carl2, dora, emil);
  await tp(anna, 'Anna', P.x + 0.5, FLOOR + 1, P.z + 3.5, 180, 20);
  await strike(anna, P.x + 0.5, P.y + 0.5, P.z + 0.5);
  await sleep(800);
  await strike(anna, P.x + 0.5, P.y + 0.5, P.z + 0.5);
  await tp(anna, 'Anna', Q.x + 0.5, FLOOR + 1, Q.z - 2.5, 0, 20);
  await strike(anna, Q.x + 0.5, Q.y + 0.5, Q.z + 0.5);
  await sleep(800);
  await strike(anna, Q.x + 0.5, Q.y + 0.5, Q.z + 0.5);
  check('Alle gefunden: „Die Gendarmen gewinnen!“', await waitFor(() => sawTitle(anna, /Gendarmen gewinnen/), 4000) &&
    sawTitle(bert, /constables win/), `${anna.titles.join('|')} ${bert.titles.join('|')}`);
  check('Zuschauer Emil sieht das Ende', sawTitle(emil, /Gendarmen gewinnen/));
  await sleep(1000);
  st = statsOf('Anna').at(-1);
  check('Netzwerk-Statistik Anna: Sieg als Gendarm, zwei gefunden', st?.add.wins_seeker === 1 && st.add.found === 2,
    JSON.stringify(st));
  st = statsOf('Carl').at(-1);
  check('Netzwerk-Statistik Carl (Nachzügler): Sieg als Gendarm', st?.add.wins_seeker === 1 && st.add.games === 1,
    JSON.stringify(st));
  st = statsOf('Bert').at(-1);
  check('Netzwerk-Statistik Bert: als Gefundener am Ende Gendarm → Sieg mit', st?.add.wins_seeker === 1 &&
    st.add.survived === 0, JSON.stringify(st));
  check('Zweite Runde: alle zurück in der Gendarmerie', await waitFor(() => [anna, bert, carl2, dora].every(inHall), 9000));
  check('Emil ist nach dem Zuschauen auch zurück', await waitFor(() => inHall(emil), 3000));
  check('Heustadel aufgeräumt (Blöcke weg)', (await blockIs(P.x, P.y, P.z, 'air')) && (await blockIs(Q.x, Q.y, Q.z, 'air')) &&
    (await countDisplays(HEU, 0)) === 0);
  const statsOut = String(await cmd('stats Anna')).replace(/§./g, '');
  check('/stats zeigt Runden und Funde', /Runden: 2/.test(statsOut) && /Gefunden: 2/.test(statsOut), statsOut.replace(/\n/g, ' '));
  const topOut = String(await cmd('top')).replace(/§./g, '');
  check('/top zeigt die Steckbriefe', /Meiste Funde/.test(topOut) && /Anna|Dora/.test(topOut), topOut.replace(/\n/g, ' '));

  // ------------------------------------------------------------------ Lobby
  await useItem(bert, 8);
  check('Bett: zurück zur Lobby (Netzwerk)', await waitFor(() => calls().some((c) => c.t === 'to-lobby' && c.name === 'Bert'), 3000));

  for (const b of [anna, bert, carl2, dora, emil]) {
    try {
      b.quit();
    } catch {
      // schon weg
    }
  }
  await sleep(1500);
  const failed = results.filter((r) => !r.ok);
  console.log(`\n${results.length - failed.length}/${results.length} Prüfungen bestanden.`);
  if (failed.length) {
    console.log('Fehlgeschlagen:');
    failed.forEach((f) => console.log(`  ✗ ${f.name}`));
  }
  process.exit(failed.length ? 1 : 0);
}

main().catch((e) => {
  console.error(e);
  process.exit(2);
});
