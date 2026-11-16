/**
 * Echter Test von Morni Versteckerl mit Test-Spielern (mineflayer) auf Paper 26.1.2 – gestartet von test/run.sh.
 *   node versteckerl-test.cjs <host> <port> <rcon-host:port> <rcon-passwort-datei> <server-ordner>
 *
 * Spieler: Anna (deutsch, Party-Leiterin mit Carl), Bert (englisch), Carl (deutsch, kommt in Runde 2 als
 * Party-Nachzügler), Dora (deutsch), Emil (schaut zu). Sprache und Party kommen vom Test-Netzwerk (test/fake-network),
 * das auch Statistik, Zustand und „zur Lobby“ in calls.log mitschreibt.
 *
 * Mit „--nach-neustart“ (zweiter Teil, nach einem Neustart des Servers): sind Guthaben, Freischaltungen und
 * Ausrüstung noch da?
 *
 * Verfasst mit Claude 🤖
 */
const crypto = require('node:crypto');
const fs = require('node:fs');
const path = require('node:path');
const mineflayer = require('mineflayer');
const { Vec3 } = require('vec3');
const { rcon } = require('./rcon.cjs');

const [HOST = '127.0.0.1', PORT = '25820', RCON = '127.0.0.1:25920', PASS_FILE, SERVER, PHASE = ''] = process.argv.slice(2);
const AFTER_RESTART = PHASE === '--nach-neustart';
const PASS = fs.readFileSync(PASS_FILE, 'utf8').trim();
const VERSION = process.env.MC_VERSION ?? '26.1';
const HALL = { x: 0.5, y: 101, z: 7.5 };
const KRAEMER = { x: 11.5, y: 101, z: -5.5 };
const WALLET_FILE = () => path.join(SERVER, 'vk-laden-dora.txt');
const DORF = 1000;
const HEU = 3000;
const FLOOR = 100;
/** rueckkehr-sekunden im Test (test/run.sh). */
const COMEBACK = 20;

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
/** Netzwerk-Aufrufe einer Art für einen Namen (ab Stelle {from} in calls.log). */
const callsOf = (t, name, from = 0) => calls().slice(from).filter((c) => c.t === t && c.name === name);

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
  bot.scoreLines = [];
  bot.kickReason = null;
  bot.on('messagestr', (m, position) => (position === 'game_info' ? bot.bars : bot.chats).push(m));
  bot._client.on('action_bar', (p) => bot.bars.push(plain(p.text)));
  bot._client.on('set_title_text', (p) => bot.titles.push(plain(p.text)));
  bot._client.on('set_title_subtitle', (p) => bot.subtitles.push(plain(p.text)));
  bot.on('soundEffectHeard', (n) => bot.sounds.push(n));
  // Die Leiste rechts hat eigene Zeilennamen (mineflayer merkt sie sich nicht) – direkt aus dem Paket lesen.
  bot._client.on('scoreboard_score', (p) => {
    if (p.display_name != null) bot.scoreLines.push(plain(p.display_name));
  });
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
        const [role, shape, solid, lives, points, finds, whistles, cell, online, spent, waiting] = v.split(':');
        s.members[k] = {
          role, shape, solid: solid === '1', lives: Number(lives), points: Number(points), finds: Number(finds),
          whistles: Number(whistles), cell: cell === '-' ? null : cell.split(',').map(Number), online: online === 'on',
          spent: Number(spent ?? 0), waiting: Number(waiting ?? 0),
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

/** /versteckerladmin laden <name> als Werte: {guthaben, gesamt, serie, frei: [...], aus: {pfiff: 'kuhglocke', …}} */
async function wallet(name) {
  const line = String((await cmd(`versteckerladmin laden ${name}`))[0]).replace(/§./g, '').trim();
  const w = { line };
  for (const part of line.split(' ')) {
    const [k, v = ''] = part.split('=');
    if (k === 'frei') w.frei = v ? v.split(',') : [];
    else if (k === 'aus') w.aus = Object.fromEntries(v.split(',').map((x) => x.split(':')));
    else w[k] = /^\d+$/.test(v) ? Number(v) : v;
  }
  return w;
}

const loreOf = (item) => (item?.customLore ?? []).map(plain).join(' | ');
const windowItem = (bot, re) => {
  const win = bot.currentWindow;
  if (!win) return null;
  return win.slots.find((x, i) => x && i < win.inventoryStart && re.test(plain(x.customName))) ?? null;
};

/** Im offenen Fenster anklicken und warten, bis sich das Fenster (neu) öffnet. */
async function clickAndWait(bot, re) {
  const opened = new Promise((resolve) => {
    bot.once('windowOpen', resolve);
    setTimeout(() => resolve(null), 3000);
  });
  if (!(await clickNamed(bot, re))) return null;
  return opened;
}

async function blockIs(x, y, z, name) {
  const out = String(await cmd(inWorld(`execute if block ${x} ${y} ${z} ${name}`)));
  return /passed/i.test(out);
}

async function main() {
  // ------------------------------------------------------------------ Gendarmerie
  await sleep(6000);
  const holos = String(await cmd(inWorld(`execute positioned 0 ${FLOOR + 1} 0 if entity @e[type=text_display,distance=..30]`)));
  check('Tafeln der Gendarmerie stehen (4 Tafeln + Schild des Krämers, je 2 Sprachen)', /: 10\b/.test(holos), holos.trim());
  const wastl = String(await cmd(inWorld(`execute positioned ${KRAEMER.x} ${KRAEMER.y} ${KRAEMER.z} if entity @e[type=villager,tag=versteckerl_kraemer,distance=..1.5]`)));
  check('Krämer Wastl steht am Kramerstand', /: 1\b/.test(wastl), wastl.trim());
  let anna = await join('Anna');
  const bert = await join('Bert');
  check('Ankunft in der Gendarmerie', inHall(anna), JSON.stringify(anna.entity.position));
  check('Schnellleiste: Kompass, Farbstoff, Papier, Bett',
    slot(anna, 0)?.name === 'compass' && slot(anna, 1)?.name === 'gray_dye' && slot(anna, 4)?.name === 'paper' &&
      slot(anna, 8)?.name === 'red_bed', [0, 1, 4, 8].map((i) => slot(anna, i)?.name).join(','));
  check('Deutsch: „Karte wählen“', plain(slot(anna, 0)?.customName).includes('Karte wählen'), plain(slot(anna, 0)?.customName));
  check('Schnellleiste: Smaragd = Kramerladen', slot(anna, 2)?.name === 'emerald' &&
    plain(slot(anna, 2)?.customName).includes('Kramerladen'), plain(slot(anna, 2)?.customName));
  check('Englisch (Sprache vom Netzwerk): „Vote for a map“', plain(slot(bert, 0)?.customName).includes('Vote for a map'),
    plain(slot(bert, 0)?.customName));
  check('Begrüßung und Erklärung im Chat', sees(anna, /Gendarmerie/) && sees(anna, /rastet ein/));
  check('Hinweis auf den Kramerladen', sees(anna, /Krämer Wastl/) && sees(bert, /Wastl the shopkeeper/));
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
  check('Steckbrief zeigt Guthaben und vier Bestenlisten', /Guthaben: 0/.test(loreOf(win?.slots[10])) &&
    /Punkte insgesamt/.test(plain(win?.slots[14]?.customName)), `${loreOf(win?.slots[10])} / ${plain(win?.slots[14]?.customName)}`);
  await closeWindow(anna);

  // ------------------------------------------------------------------ Kramerladen (noch ohne Guthaben)
  win = await openWithItem(anna, 2);
  check('Smaragd öffnet den Kramerladen', win && plain(win.title) === 'Kramerladen', win && plain(win.title));
  check('Guthaben oben: 0 ✦', /Guthaben: 0/.test(plain(win?.slots[4]?.customName)), plain(win?.slots[4]?.customName));
  const kinds = [19, 20, 21, 23, 24, 25].map((i) => plain(win?.slots[i]?.customName));
  check('Sechs Abteilungen', ['Gestalten', 'Pfiff-Klänge', 'Fund-Effekte', 'Spuren', 'Sieg-Effekte', 'Titel']
    .every((k, i) => kinds[i] === k), kinds.join(','));
  check('„So verdienst du Punkte“ mit den Werten', /Fund \+10/.test(loreOf(win?.slots[31])), loreOf(win?.slots[31]));
  win = await clickAndWait(anna, /^Pfiff-Klänge$/);
  check('Abteilung Pfiff-Klänge', win && plain(win.title) === 'Kramerladen · Pfiff-Klänge', win && plain(win.title));
  const bell = windowItem(anna, /^Kuhglocke$/);
  check('Kuhglocke: selten, laut (+2), 160 ✦, zu teuer', /Selten/.test(loreOf(bell)) && /\+2 Punkte/.test(loreOf(bell)) &&
    /Preis: 160/.test(loreOf(bell)) && /fehlen noch 160/.test(loreOf(bell)), loreOf(bell));
  check('Flötenpfiff ist frei und dabei', /Dabei/.test(loreOf(windowItem(anna, /^Flötenpfiff$/))), loreOf(windowItem(anna, /^Flötenpfiff$/)));
  await clickNamed(anna, /^Kuhglocke$/);
  check('Zu wenig Guthaben: nichts gekauft', await waitFor(() => sees(anna, /fehlen dir noch 160/), 2000) &&
    (await wallet('Anna')).frei.length === 0, anna.chats.slice(-1)[0]);
  await closeWindow(anna);
  await tp(bert, 'Bert', 9.5, 101, -5.5, -90, 0);
  const villager = Object.values(bert.entities).find((e) => e.name === 'villager' &&
    e.position.distanceTo(new Vec3(KRAEMER.x, KRAEMER.y, KRAEMER.z)) < 1.5);
  const shopOpen = new Promise((resolve) => {
    bert.once('windowOpen', resolve);
    setTimeout(() => resolve(null), 4000);
  });
  if (villager) {
    await bert.lookAt(villager.position.offset(0, 1.5, 0), true);
    bert.activateEntity(villager);
  }
  win = await shopOpen;
  check('Rechtsklick auf Wastl öffnet den Laden (englisch)', win && plain(win.title) === 'General store',
    `${Boolean(villager)} ${win && plain(win.title)}`);
  await closeWindow(bert);
  await tp(bert, 'Bert', HALL.x, HALL.y, HALL.z, 180, 0);

  // ------------------------------------------------------------------ Runde 1: Dorfplatz
  let carl = await join('Carl');
  const dora = await join('Dora');
  await cmd('versteckerladmin sucher Dora');
  check('Ab 4 Leuten läuft der Countdown von selbst', await waitFor(() => anna.bars.some((b) => /Nächste Runde in \d+ s · Karte: Dorfplatz/.test(b)), 4000),
    anna.bars.slice(-1)[0]);
  await useItem(carl, 1);
  check('Farbstoff: Carl ist bereit', sees(carl, /Du bist bereit/));
  const started = await waitFor(() => onMap(anna, DORF) && onMap(dora, DORF), 15000);
  check('Runde startet: alle auf dem Dorfplatz', started, JSON.stringify(anna.entity.position));
  check('Netzwerk: Zustand „running“', await waitFor(() => calls().some((c) => c.t === 'state' && c.state === 'running'), 3000));
  check('Netzwerk: alle vier „spielen gerade“ (Nachfrage vor der Lobby)', ['Anna', 'Bert', 'Carl', 'Dora'].every((n) =>
    callsOf('playing', n).some((c) => c.playing === true)), calls().filter((c) => c.t === 'playing').map((c) => `${c.name}:${c.playing}`).join(' '));
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

  // ------------------------------------------------------------------ Zurück ins Spiel (rechtzeitig): Anna
  await cmd('versteckerladmin zeit 45');
  s = (await status()).dorfplatz;
  const annaBefore = { ...s.members.Anna, cell: String(s.members.Anna.cell) };
  let mark = calls().length;
  clear(bert, carl, dora);
  anna.quit();
  check(`Anna verliert die Verbindung: Platz bleibt ${COMEBACK} s frei (Netzwerk: holdSlot ${COMEBACK})`,
    await waitFor(() => callsOf('hold-slot', 'Anna', mark).some((c) => c.seconds === COMEBACK), 5000), JSON.stringify(calls().slice(mark)));
  check('Die anderen erfahren es (de/en)', await waitFor(() => sees(carl, new RegExp(`Anna hat die Verbindung verloren – der Platz bleibt ${COMEBACK} Sekunden frei`)) &&
    sees(bert, /Anna lost the connection/), 3000), carl.chats.slice(-1)[0]);
  s = (await status()).dorfplatz;
  check('Die Runde wartet auf Anna (noch eingeplant, weg) – ihr Fass ist so lange fort', s.members.Anna?.online === false &&
    s.members.Anna.role === 'HIDER' && s.members.Anna.waiting > 0 && (await blockIs(A.x, A.y, A.z, 'air')), JSON.stringify(s.members.Anna));
  await sleep(4000);
  anna = await join('Anna');
  check('Anna ist rechtzeitig zurück: „Willkommen zurück!“', await waitFor(() => sawTitle(anna, /Willkommen zurück/) &&
    sees(anna, /Willkommen zurück! Du bist wieder in der Runde/), 4000), `${anna.titles.join(' | ')} / ${anna.chats.slice(-2).join(' | ')}`);
  s = (await status()).dorfplatz;
  check('Gleiche Rolle, Gestalt, Leben und Punkte – und wieder als echtes Fass an ihrem Platz eingerastet',
    s.members.Anna.online && s.members.Anna.role === 'HIDER' && s.members.Anna.shape === annaBefore.shape &&
      s.members.Anna.points === annaBefore.points && s.members.Anna.lives === annaBefore.lives && s.members.Anna.solid &&
      String(s.members.Anna.cell) === annaBefore.cell && (await blockIs(A.x, A.y, A.z, 'barrel')), JSON.stringify(s.members.Anna));
  check('Anna ist wieder unsichtbar, an derselben Stelle, mit ihren Knöpfen', hasEffect(anna, INVISIBILITY) &&
    near(anna.entity.position, { x: A.x + 0.5, y: A.y, z: A.z + 0.5 }, 1) && slot(anna, 0)?.name === 'armor_stand',
  `${JSON.stringify(anna.entity.position)} ${slot(anna, 0)?.name}`);
  check('Netzwerk: Platz wieder frei (holdSlot 0) und Anna „spielt gerade“', callsOf('hold-slot', 'Anna', mark).some((c) => c.seconds === 0) &&
    callsOf('playing', 'Anna', mark).some((c) => c.playing === true), JSON.stringify(calls().slice(mark)));
  check('Die anderen sehen „Anna ist zurück in der Runde“', await waitFor(() => sees(carl, /Anna ist zurück in der Runde/), 3000));
  check('Die anderen sehen Anna (die Spielerin) weiterhin nicht', await waitFor(() =>
    !Object.values(dora.entities).some((e) => e.username === 'Anna') && !Object.values(carl.entities).some((e) => e.username === 'Anna'), 3000));

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
  // Mut: Gendarm steht ganz nah → Extrapunkte
  await tp(dora, 'Dora', A.x + 2.5, FLOOR + 1, A.z + 0.5, 90, 0);
  await sleep(3200);
  let before = (await status()).dorfplatz.members.Anna.points;
  anna.bars.length = 0;
  await useItem(anna, 1);
  s = (await status()).dorfplatz;
  check('Mutiger Pfiff (Gendarm ganz nah): Extrapunkte', s.members.Anna.points - before >= 5 && sawBar(anna, /mutig \+4/),
    `${before} → ${s.members.Anna.points} ${anna.bars.slice(-1)[0]}`);
  // Rundenladen: Lockpfiff (Testpreis 1), dann Abklingzeit
  check('Versteckte haben die Kramerkiste', slot(anna, 3)?.name === 'chest' && plain(slot(anna, 3)?.customName).includes('Kramerkiste'));
  win = await openWithItem(anna, 3);
  check('Kramerkiste: Hilfen für die Runde', win && plain(win.title).includes('Kramerkiste') &&
    ['Lockpfiff', 'Nebelwolke', 'Noch ein Gestaltwechsel'].every((n) => windowItem(anna, new RegExp(`^${n}$`))),
    win && plain(win.title));
  before = (await status()).dorfplatz.members.Anna;
  dora.sounds.length = 0;
  await clickNamed(anna, /^Lockpfiff$/);
  s = (await status()).dorfplatz;
  check('Lockpfiff gekauft (bezahlt mit Rundenpunkten)', s.members.Anna.spent === 1 &&
    await waitFor(() => sees(anna, /Lockpfiff für 1 Punkte/), 2000), `${JSON.stringify(s.members.Anna)} ${anna.chats.slice(-1)[0]}`);
  check('Der Lockpfiff ist zu hören', await waitFor(() => dora.sounds.some((n) => /note_block/.test(n)), 2000), dora.sounds.join(','));
  win = await openWithItem(anna, 3);
  check('Lockpfiff im Menü: Abklingzeit', /noch \d+ s/.test(loreOf(windowItem(anna, /^Lockpfiff$/))), loreOf(windowItem(anna, /^Lockpfiff$/)));
  await clickNamed(anna, /^Lockpfiff$/);
  check('Lockpfiff gleich nochmal: Abklingzeit, nichts abgebucht', await waitFor(() => sees(anna, /geht gleich wieder/), 2000) &&
    (await status()).dorfplatz.members.Anna.spent === 1, anna.chats.slice(-1)[0]);
  await closeWindow(anna);
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
  win = await openWithItem(anna, 3);
  const reopened = new Promise((resolve) => {
    anna.once('windowOpen', resolve);
    setTimeout(() => resolve(null), 3000);
  });
  await clickNamed(anna, /^Noch ein Gestaltwechsel$/);
  win = await reopened;
  check('Rundenladen: noch ein Gestaltwechsel öffnet das Menü', win && plain(win.title).includes('Gestalt wechseln'),
    `${win && plain(win.title)} ${anna.chats.slice(-1)[0]}`);
  await clickNamed(anna, /^Fass$/);
  s = (await status()).dorfplatz;
  check('… und Anna ist wieder ein Fass (auch der echte Block)', s.members.Anna.shape === 'fass' && s.members.Anna.spent === 3 &&
    (await blockIs(A.x, A.y, A.z, 'barrel')), JSON.stringify(s.members.Anna));
  await closeWindow(anna);

  // Spürnase
  await useItem(dora, 1);
  check('Spürnase: Spur zum nächsten Versteckten', sees(dora, /Spürnase zuckt/), dora.chats.slice(-1)[0]);
  await useItem(dora, 1);
  check('Spürnase nur einmal pro Runde', sees(dora, /schon benutzt/));
  check('Gendarmen haben die Kramerkiste', slot(dora, 2)?.name === 'chest');
  win = await openWithItem(dora, 2);
  check('Kramerkiste der Gendarmen: Laterne, Spürnase', win && windowItem(dora, /^Laterne$/) &&
    windowItem(dora, /^Noch eine Spürnase$/) && !windowItem(dora, /^Lockpfiff$/), win && plain(win.title));
  dora.chats.length = 0;
  await clickNamed(dora, /^Noch eine Spürnase$/);
  check('Rundenladen: noch eine Spürnase', await waitFor(() => sees(dora, /Noch eine Spürnase für 3 Punkte/) &&
    sees(dora, /Spürnase zuckt/), 2000), dora.chats.join(' | '));
  win = await openWithItem(dora, 2);
  await clickNamed(dora, /^Laterne$/);
  s = (await status()).dorfplatz;
  check('Rundenladen: Laterne leuchtet', await waitFor(() => sawBar(dora, /Laterne leuchtet/), 2000) && s.members.Dora.spent === 5,
    `${JSON.stringify(s.members.Dora)} ${dora.bars.slice(-1)[0]}`);
  await closeWindow(dora);

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

  // Zu spät: Carl (jetzt Gendarm) verliert die Verbindung und kommt nicht rechtzeitig zurück – dann wie bisher.
  mark = calls().length;
  carl.quit();
  check(`Carl (Gendarm) geht: Platz bleibt erst ${COMEBACK} s frei`, await waitFor(() =>
    callsOf('hold-slot', 'Carl', mark).some((c) => c.seconds === COMEBACK), 5000), JSON.stringify(calls().slice(mark)));
  s = (await status()).dorfplatz;
  check('Die Runde wartet auf Carl (noch eingeplant, weg)', s.members.Carl?.online === false && s.members.Carl.role === 'SEEKER',
    JSON.stringify(s.members.Carl));

  // Pflicht-Pfiff: Anna pfeift zu lange nicht → pfeift von selbst
  check('Pflicht zum Pfeifen wird angesagt', await waitFor(() => sees(anna, /muss jeder Versteckte mindestens alle/), 30000),
    anna.chats.slice(-2).join(' | '));
  check('Pflicht-Pfiff kommt von selbst (ohne Punkte)', await waitFor(() => sees(anna, /Pflicht-Pfiff/), 30000),
    anna.chats.slice(-2).join(' | '));

  check(`Nach ${COMEBACK} s: Carl ist nicht rechtzeitig zurück und hat die Runde verlassen`, await waitFor(() =>
    sees(anna, /Carl ist nicht rechtzeitig zurückgekommen und hat die Runde verlassen/), (COMEBACK + 15) * 1000, 500),
  anna.chats.slice(-2).join(' | '));
  check('Netzwerk: Angebot zurückgenommen (holdSlot 0), Runde für Carl gezählt (kein Sieg)',
    callsOf('hold-slot', 'Carl', mark).some((c) => c.seconds === 0) && statsOf('Carl').at(-1)?.add.games === 1 &&
      statsOf('Carl').at(-1)?.add.wins_seeker === 0 && !(await status()).dorfplatz.members.Carl, JSON.stringify(statsOf('Carl').at(-1)));
  carl = await join('Carl');
  check('Carl kommt zu spät: wie bisher ein neuer Anfang (als Nachzügler der Party hilft er als Gendarm)', await waitFor(async () =>
    sees(carl, /Deine Party spielt gerade/) && (await status()).dorfplatz?.members.Carl?.role === 'SEEKER', 6000),
  carl.chats.slice(-2).join(' | '));

  // Ende: Zeit um, Anna ist noch versteckt → die Versteckten gewinnen
  clear(anna, bert, carl, dora);
  await cmd('versteckerladmin zeit 1');
  check('Zeit um: „Die Versteckten gewinnen!“', await waitFor(() => sawTitle(anna, /Versteckten gewinnen/), 4000) &&
    sawTitle(bert, /hiders win/) && sawTitle(anna, /Gewonnen/));
  // Titel und Rangliste gehen im selben Takt raus, kommen bei den Test-Spielern unter Last aber nacheinander an.
  check('Rangliste im Chat', await waitFor(() => sees(dora, /1\. .* Punkte/) && sees(anna, /Deine Punkte:/), 3000),
    `${dora.chats.slice(-3).join(' | ')} / ${anna.chats.slice(-3).join(' | ')}`);
  check('Annas Gestalt leuchtet zum Aufdecken', await countDisplays(DORF, 0) === 1);
  check('Netzwerk: nach der Runde spielt niemand mehr', await waitFor(() => ['Anna', 'Bert', 'Carl', 'Dora'].every((n) =>
    callsOf('playing', n).at(-1)?.playing === false), 3000),
  ['Anna', 'Bert', 'Carl', 'Dora'].map((n) => `${n}:${callsOf('playing', n).at(-1)?.playing}`).join(' '));
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
  // Unter Last kommt das Paket, das Anna für Dora wieder zeigt, etwas nach dem Teleport an – darum kurz warten.
  check('Alle wieder sichtbar und ohne Unsichtbarkeit', await waitFor(() => !hasEffect(anna, INVISIBILITY) &&
    Object.values(dora.entities).some((e) => e.username === 'Anna'), 4000),
  `unsichtbar=${hasEffect(anna, INVISIBILITY)} dora-sieht-anna=${Object.values(dora.entities).some((e) => e.username === 'Anna')}`);
  check('Spielerdatei mit Sieg', /siege-versteckt: 1/.test(readFile(playerFile('Anna'))) &&
    /gefunden: 2/.test(readFile(playerFile('Dora'))), readFile(playerFile('Anna')).replace(/\n/g, ' '));
  check('Steckbriefe (Bestenliste) gespeichert', /funde:[\s\S]*Dora/.test(readFile(path.join(SERVER, 'plugins/MorniVersteckerl/bestenliste.yml'))) &&
    /gesamt:[\s\S]*Anna/.test(readFile(path.join(SERVER, 'plugins/MorniVersteckerl/bestenliste.yml'))));
  st = statsOf('Anna').at(-1);
  check('Abrechnung für Anna: Runde, Ausgaben, erster Sieg heute, Guthaben', sees(anna, /Abrechnung/) &&
    sees(anna, /für Hilfen ausgegeben/) && sees(anna, /erster Sieg heute/) && sees(anna, /aufs Guthaben · jetzt \d+ ✦/),
    anna.chats.filter((c) => /Abrechnung|\+\d+|−\d+|= \d+/.test(c)).join(' | '));
  const annaW = await wallet('Anna');
  check('Guthaben = Rest der Runde + Tagesbonus', annaW.guthaben === st.add.points - 3 + 25 &&
    st.add.earned === annaW.guthaben && st.add.bonus === 25 && st.add.helps === 2 && annaW.serie === 1,
    `${annaW.line} ${JSON.stringify(st.add)}`);
  check('Punkte insgesamt = Runde + Bonus (Ausgaben ziehen nichts ab)', annaW.gesamt === st.add.points + 25, annaW.line);
  check('Abrechnung auch in der Leiste: Runde, Hilfen, Tagesbonus, = +N ✦, Guthaben jetzt',
    anna.scoreLines.includes('Abrechnung') && anna.scoreLines.includes(`Aus der Runde: +${st.add.points}`) &&
      anna.scoreLines.includes('Für Hilfen: −3') && anna.scoreLines.includes('Erster Sieg heute: +25') &&
      anna.scoreLines.includes(`= +${annaW.guthaben} ✦ aufs Guthaben`) &&
      anna.scoreLines.includes(`Guthaben jetzt: ${annaW.guthaben} ✦`),
    anna.scoreLines.filter((l) => /Abrechnung|Runde:|Hilfen:|heute|aufs Guthaben|jetzt/.test(l)).join(' | '));
  check('Spielerdatei: guthaben, serie, tagesbonus', /guthaben: \d+/.test(readFile(playerFile('Anna'))) &&
    /tagesbonus: '?20\d\d-\d\d-\d\d/.test(readFile(playerFile('Anna'))), readFile(playerFile('Anna')).replace(/\n/g, ' '));
  check('Dora verlor: Abrechnung ohne Tagesbonus', sees(dora, /Abrechnung/) && !sees(dora, /erster Sieg heute/) &&
    (await wallet('Dora')).serie === 0, dora.chats.filter((c) => /Abrechnung|Guthaben/.test(c)).join(' | '));
  check('Leiste in der Gendarmerie zeigt das Guthaben', await waitFor(() => anna.scoreLines.some((l) =>
    new RegExp(`Guthaben: ${annaW.guthaben} ✦`).test(l)) && anna.scoreLines.includes(`Punkte insgesamt: ${annaW.gesamt}`),
  3000), anna.scoreLines.filter((l) => /Guthaben|insgesamt/.test(l)).slice(-2).join(' | '));

  // ------------------------------------------------------------------ Runde 2: Heustadel – Gendarmen gewinnen
  clear(anna, bert, carl, dora);
  carl.quit();
  await sleep(1000);
  // ------------------------------------------------------------------ Kramerladen: kaufen und ausrüsten
  await cmd('versteckerladmin guthaben Dora 1000');
  win = await openWithItem(dora, 2);
  check('Dora hat 1000 ✦', /Guthaben: 1000/.test(plain(win?.slots[4]?.customName)), plain(win?.slots[4]?.customName));
  win = await clickAndWait(dora, /^Gestalten$/);
  const bench = windowItem(dora, /^Werkbank$/);
  check('Gestalten im Laden: Werkbank (gewöhnlich, 150 ✦, Dorfplatz/Wirtshaus/Heustadel)', /Gewöhnlich/.test(loreOf(bench)) &&
    /Preis: 150/.test(loreOf(bench)) && /Heustadel/.test(loreOf(bench)), loreOf(bench));
  dora.chats.length = 0;
  await clickNamed(dora, /^Werkbank$/);
  check('Erster Klick fragt nach', await waitFor(() => sees(dora, /Nochmal klicken zum Kaufen/), 2000) &&
    (await wallet('Dora')).guthaben === 1000 && /Nochmal klicken/.test(loreOf(windowItem(dora, /^Werkbank$/))),
    dora.chats.slice(-1)[0]);
  await clickNamed(dora, /^Werkbank$/);
  let doraW = await wallet('Dora');
  check('Zweiter Klick kauft: Werkbank für 150 ✦', await waitFor(() => sees(dora, /Gekauft: Werkbank/), 2000) &&
    doraW.guthaben === 850 && doraW.frei.includes('gestalt:werkbank') && doraW.aus.gestalt === 'werkbank', doraW.line);
  check('Werkbank im Laden: Lieblingsgestalt', /Lieblingsgestalt/.test(loreOf(windowItem(dora, /^Werkbank$/))),
    loreOf(windowItem(dora, /^Werkbank$/)));
  await clickNamed(dora, /^Werkbank$/);
  check('Werkbank nochmal: nichts abgebucht (nur Lieblingsgestalt aus)', (await wallet('Dora')).guthaben === 850 &&
    sees(dora, /Keine Lieblingsgestalt mehr/), dora.chats.slice(-1)[0]);
  await clickNamed(dora, /^Werkbank$/);
  check('Werkbank wieder als Lieblingsgestalt', (await wallet('Dora')).aus.gestalt === 'werkbank');
  win = await clickAndWait(dora, /Zurück/);
  win = await clickAndWait(dora, /^Pfiff-Klänge$/);
  await clickNamed(dora, /^Kuhglocke$/);
  await clickNamed(dora, /^Kuhglocke$/);
  win = await clickAndWait(dora, /Zurück/);
  win = await clickAndWait(dora, /^Titel$/);
  const pfeif = windowItem(dora, /Pfeifkönig/);
  check('Titel mit Bedingung: Pfeifkönig erst ab 50 Pfiffen', /Erst ab 50 Pfiffen/.test(loreOf(pfeif)) &&
    /Noch nicht zu haben/.test(loreOf(pfeif)), loreOf(pfeif));
  await clickNamed(dora, /Dorfkind/);
  await clickNamed(dora, /Dorfkind/);
  doraW = await wallet('Dora');
  check('Kuhglocke (160) und Titel Dorfkind (60) gekauft und ausgerüstet', doraW.guthaben === 630 &&
    doraW.aus.pfiff === 'kuhglocke' && doraW.aus.titel === 'dorfkind' && doraW.frei.length === 3, doraW.line);
  await closeWindow(dora);
  check('Netzwerk: Freischaltungen gemeldet', statsOf('Dora').filter((c) => c.add.unlocks === 1).length === 3 &&
    statsOf('Dora').filter((c) => c.add.unlocks === 1).reduce((a, c) => a + c.add.spent, 0) === 370);
  check('Spielerdatei: freigeschaltet und ausgerüstet', /freigeschaltet:[\s\S]*gestalt:werkbank/.test(readFile(playerFile('Dora'))) &&
    /ausgeruestet:[\s\S]*pfiff: kuhglocke/.test(readFile(playerFile('Dora'))), readFile(playerFile('Dora')).replace(/\n/g, ' '));
  win = await openWithItem(bert, 2);
  win = await clickAndWait(bert, /^Whistle sounds$/);
  const bertBefore = (await wallet('Bert')).guthaben;
  bert.chats.length = 0;
  await clickNamed(bert, /^Alphorn$/);
  check('Bert (englisch) hat zu wenig für das Alphorn', await waitFor(() => sees(bert, /You need \d+ ✦ more for Alphorn/), 2000) &&
    (await wallet('Bert')).guthaben === bertBefore, bert.chats.slice(-1)[0]);
  await closeWindow(bert);
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
  check('Dora startet mit ihrer Lieblingsgestalt: Werkbank', s.members.Dora.shape === 'werkbank', JSON.stringify(s.members.Dora));
  const lockedBench = windowItem(bert, /^Workbench$/);
  check('Bert sieht die Werkbank, aber gesperrt (Preis)', lockedBench?.name === 'gray_dye' && /Not unlocked yet – 150/.test(loreOf(lockedBench)),
    `${lockedBench?.name} ${loreOf(lockedBench)}`);
  bert.chats.length = 0;
  await clickNamed(bert, /^Workbench$/);
  check('Gesperrte Gestalt: freundlicher Hinweis, keine Verwandlung', await waitFor(() => sees(bert, /Unlock the Workbench disguise/), 2000) &&
    (await status()).heustadel.members.Bert.shape !== 'werkbank', bert.chats.slice(-1)[0]);
  await waitFor(() => bert.currentWindow, 1000);
  if (!bert.currentWindow) await openWithItem(bert, 0);
  await clickNamed(bert, /^Pumpkin$/);
  await waitFor(() => dora.currentWindow, 2000);
  check('Doras Menü: Werkbank freigeschaltet und gewählt', /Das bist du gerade/.test(loreOf(windowItem(dora, /^Werkbank$/))),
    loreOf(windowItem(dora, /^Werkbank$/)));
  await closeWindow(dora);
  // Zuschauer
  const emil = await join('Emil');
  emil.chat('/zuschauen heustadel');
  const watching = await waitFor(() => emil.game.gameMode === 'spectator' && onMap(emil, HEU), 6000);
  const seesBert = await waitFor(() => Object.values(emil.entities).some((e) => e.username === 'Bert'), 5000);
  check('Emil schaut zu (Zuschauermodus, sieht die Versteckten)', watching && sees(emil, /Du schaust Heustadel zu/) && seesBert,
    `${emil.game.gameMode} ${JSON.stringify(emil.entity.position)} bert=${seesBert} ${emil.chats.slice(-2).join(' | ')}`);
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
  check('Bert (Kürbis) und Dora (Werkbank aus dem Laden) eingerastet', s.members.Bert.solid && s.members.Dora.solid &&
    (await blockIs(P.x, P.y, P.z, 'carved_pumpkin')) && (await blockIs(Q.x, Q.y, Q.z, 'crafting_table')), JSON.stringify(s.members));
  await cmd('versteckerladmin zeit 1');
  await waitFor(async () => (await status()).heustadel?.phase === 'SEEK', 4000);
  await sleep(800);
  clear(anna, bert, carl2, dora, emil);
  // Pfiff-Klang aus dem Laden: Kuhglocke ist laut → +2
  await useItem(dora, 1);
  s = (await status()).heustadel;
  check('Kuhglocke: lauter Pfiff mit Extrapunkten (1 + 2)', s.members.Dora.points === 3 && s.members.Dora.whistles === 1 &&
    sawBar(dora, /laut \+2/), `${JSON.stringify(s.members.Dora)} ${dora.bars.slice(-1)[0]}`);
  check('Die Gendarmen hören die Kuhglocke', await waitFor(() => anna.sounds.some((n) => /note_block|bell/.test(n)), 2000),
    anna.sounds.join(','));
  // Rundenladen: zu wenig Punkte (Bert hat noch keine)
  win = await openWithItem(bert, 3);
  bert.chats.length = 0;
  await clickNamed(bert, /^Fog cloud$/);
  check('Rundenladen: zu wenig Punkte für die Nebelwolke (englisch)', await waitFor(() => sees(bert, /You need 3 more points/), 2000) &&
    (await status()).heustadel.members.Bert.spent === 0, bert.chats.slice(-1)[0]);
  await closeWindow(bert);
  // Verwaltung zum Testen (Owner/Admin): Rundenpunkte geben – Bert bekommt 5
  bert.chats.length = 0;
  const givePoints = String((await cmd('versteckerladmin punkte Bert 5'))[0]).replace(/§./g, '');
  check('Verwaltung: /versteckerladmin punkte gibt Rundenpunkte (Bestätigung, Nachricht auf Englisch)',
    /Bert hat 5 Rundenpunkte bekommen – jetzt 5/.test(givePoints) && await waitFor(() => sees(bert, /You got 5 round points – now 5/), 2000) &&
      (await status()).heustadel.members.Bert.points === 5, `${givePoints} / ${bert.chats.slice(-1)[0]}`);
  const noRound = String((await cmd('versteckerladmin punkte Emil 5'))[0]).replace(/§./g, '');
  check('… nur wer gerade eine Runde spielt (Emil schaut nur zu)', /Emil spielt gerade keine Runde/.test(noRound), noRound);
  // Nebelwolke: Anna steht nah dran und sieht kurz nichts
  await tp(anna, 'Anna', Q.x + 0.5, FLOOR + 1, Q.z - 2.5, 0, 20);
  win = await openWithItem(dora, 3);
  await clickNamed(dora, /^Nebelwolke$/);
  check('Nebelwolke: Gendarmin daneben sieht kurz nichts', await waitFor(() => hasEffect(anna, BLINDNESS), 2000) &&
    sawBar(anna, /Nebel/) && (await status()).heustadel.members.Dora.spent === 3, `${anna.bars.slice(-1)[0]}`);
  await sleep(2200);
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
  check('Zweiter Sieg in Folge: Siegesserie, kein zweiter Tagesbonus', sees(anna, /Siegesserie \(2 in Folge\)/) &&
    !sees(anna, /erster Sieg heute/) && st.best.best_streak === 2 && st.add.bonus === 3, JSON.stringify(st));
  check('Erster Fund der Runde: Extrapunkte', sees(anna, /Erster Fund der Runde/));
  check('Titel in der Rangliste: „Dora ☘ Dorfkind“', sees(anna, /Dora ☘ Dorfkind/), anna.chats.filter((c) => /Dora/.test(c)).join(' | '));
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
  // Verwaltung zum Testen (Owner/Admin): Guthaben geben – an Bert und an sich selbst (ohne Namen)
  const bertW0 = (await wallet('Bert')).guthaben;
  bert.chats.length = 0;
  const giveMoney = String((await cmd('versteckerladmin geld Bert 250'))[0]).replace(/§./g, '');
  check('Verwaltung: /versteckerladmin geld gibt Guthaben (Bestätigung, Nachricht auf Englisch)',
    /Bert hat 250 ✦ Guthaben bekommen/.test(giveMoney) && await waitFor(() => sees(bert, /You got 250 ✦ credit/), 2000) &&
      (await wallet('Bert')).guthaben === bertW0 + 250, `${giveMoney} / ${bert.chats.slice(-1)[0]}`);
  await cmd('op Anna');
  const annaW0 = (await wallet('Anna')).guthaben;
  anna.chats.length = 0;
  anna.chat('/versteckerladmin geld 100');
  check('… und sich selbst (/versteckerladmin geld 100)', await waitFor(async () => sees(anna, /Du hast 100 ✦ Guthaben bekommen/) &&
    (await wallet('Anna')).guthaben === annaW0 + 100, 3000), anna.chats.slice(-1)[0]);
  await cmd('deop Anna');
  const statsOut = String(await cmd('stats Anna')).replace(/§./g, '');
  check('/stats zeigt Runden und Funde', /Runden: 2/.test(statsOut) && /Gefunden: 2/.test(statsOut), statsOut.replace(/\n/g, ' '));
  const topOut = String(await cmd('top')).replace(/§./g, '');
  check('/top zeigt die Steckbriefe', /Meiste Funde/.test(topOut) && /Anna|Dora/.test(topOut), topOut.replace(/\n/g, ' '));

  fs.writeFileSync(WALLET_FILE(), (await wallet('Dora')).line);

  // ------------------------------------------------------------------ Lobby
  await useItem(bert, 8);
  check('Bett: zurück zur Lobby – fragt das Netzwerk (in der Gendarmerie ohne Nachfrage)', await waitFor(() =>
    calls().some((c) => c.t === 'ask-lobby' && c.name === 'Bert' && c.asked === false), 3000), JSON.stringify(calls().slice(-2)));

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

/** Zweiter Teil nach einem Neustart: alles noch da? */
async function afterRestart() {
  await sleep(4000);
  const before = readFile(WALLET_FILE()).trim();
  const now = (await wallet('Dora')).line;
  check('Nach dem Neustart: Guthaben, Freischaltungen und Ausrüstung wie vorher', before.length > 0 && now === before,
    `vorher: ${before} | jetzt: ${now}`);
  const holos = String(await cmd(inWorld(`execute positioned 0 ${FLOOR + 1} 0 if entity @e[type=text_display,distance=..30]`)));
  const wastl = String(await cmd(inWorld(`execute positioned ${KRAEMER.x} ${KRAEMER.y} ${KRAEMER.z} if entity @e[type=villager,tag=versteckerl_kraemer,distance=..1.5]`)));
  check('Nach dem Neustart: Wastl und die Tafeln stehen wieder (genau einmal)', /: 1\b/.test(wastl) && /: 10\b/.test(holos),
    `${wastl.trim()} ${holos.trim()}`);
  const dora = await join('Dora');
  const balance = /guthaben=(\d+)/.exec(before)?.[1] ?? '?';
  let win = await openWithItem(dora, 2);
  check(`Laden nach dem Neustart: Guthaben ${balance} ✦`, plain(win?.slots[4]?.customName).includes(`Guthaben: ${balance} ✦`),
    plain(win?.slots[4]?.customName));
  win = await clickAndWait(dora, /^Gestalten$/);
  check('Werkbank ist noch Lieblingsgestalt', /Lieblingsgestalt/.test(loreOf(windowItem(dora, /^Werkbank$/))),
    loreOf(windowItem(dora, /^Werkbank$/)));
  await closeWindow(dora);
  // Verwaltung: wer im Zuschauermodus war, ist nicht mehr eingeplant – /verlassen holt zurück in die Gendarmerie
  await cmd('gamemode spectator Dora');
  await sleep(1600);
  const away = String((await cmd('versteckerladmin status'))[0]);
  await cmd('gamemode adventure Dora');
  dora.chat('/verlassen');
  await sleep(1600);
  const back = String((await cmd('versteckerladmin status'))[0]);
  check('Nach dem Zuschauermodus holt /verlassen zurück in die Gendarmerie', /gendarmerie=0/.test(away) &&
    /gendarmerie=1/.test(back) && dora.entity.position.distanceTo(new Vec3(0.5, FLOOR + 1, 7.5)) < 1.5,
  `${away.split('\n')[0]} → ${back.split('\n')[0]}`);
  dora.quit();
  await sleep(1000);
  const failed = results.filter((r) => !r.ok);
  console.log(`\n${results.length - failed.length}/${results.length} Prüfungen nach dem Neustart bestanden.`);
  failed.forEach((f) => console.log(`  ✗ ${f.name}`));
  process.exit(failed.length ? 1 : 0);
}

(AFTER_RESTART ? afterRestart() : main()).catch((e) => {
  console.error(e);
  process.exit(2);
});
