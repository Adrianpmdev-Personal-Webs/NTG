/**
 * generate-legacy-data
 *
 * One-off generator of 02-legacy-data.sql, 04-verify.sql and legacy-team-images.json from the CSV export of the old Pokémon
 * database (Supabase project of the previous app) and of the current pokemon.games table.
 *
 * Usage:
 *   node generate-legacy-data.mjs <old csv folder> <current csv folder> <output folder>
 *
 * The old folder must hold juegos, modalidades, torneos, jugadores, clasificacion, batallas (plus
 * batallas_faltantes, the rows cut from the first export), recompensas, grupos, grupo_jugadores and
 * resultados_bracket. The current folder holds games.csv.
 *
 * How the old data maps to the new model
 *   - The three "GenLocke 2025" modalities (FireRed, HeartGold, Platino) are one randomlocke: its points
 *     carry over from one game to the next.
 *   - Every row of batallas / resultados_bracket is one game with the running score of its series; rows
 *     are grouped into best-of-3 series and each score step becomes a match_games row. When a step skips
 *     games (0-0 -> 1-1), the missing games are added without replay.
 *   - Old points were cumulative and, in bracket tournaments, given by final placement. The new model
 *     counts series wins x points per win, so points_adjustment stores the difference per tournament.
 *   - IDs are md5-derived UUIDs of the old ids, so the script is deterministic.
 *
 * @author @adrianpmdev
 */

import { createHash } from 'node:crypto';
import { readFileSync, writeFileSync, existsSync } from 'node:fs';
import { join } from 'node:path';

const [oldDir, currentDir, outDir] = process.argv.slice(2);
if (!oldDir || !currentDir || !outDir) {
    console.error('Usage: node generate-legacy-data.mjs <old csv folder> <current csv folder> <output folder>');
    process.exit(1);
}

// ─── CSV ─────────────────────────────────────────────────────────────────────────────────────────

function readCsv(dir, name) {
    const text = readFileSync(join(dir, `${name}.csv`), 'utf8').replace(/^﻿/, '').replace(/\r/g, '');
    const rows = [];
    let row = [];
    let cell = '';
    let quoted = false;
    for (let i = 0; i < text.length; i++) {
        const c = text[i];
        if (quoted) {
            if (c === '"' && text[i + 1] === '"') { cell += '"'; i++; }
            else if (c === '"') quoted = false;
            else cell += c;
        } else if (c === '"') quoted = true;
        else if (c === ',') { row.push(cell); cell = ''; }
        else if (c === '\n') { row.push(cell); rows.push(row); row = []; cell = ''; }
        else cell += c;
    }
    if (cell || row.length) { row.push(cell); rows.push(row); }
    const [header, ...data] = rows;
    return data.map(values => Object.fromEntries(header.map((key, i) => [key, values[i] === 'null' || values[i] === '' ? null : values[i]])));
}

// ─── Helpers ─────────────────────────────────────────────────────────────────────────────────────

/** Same result as md5(text)::uuid in PostgreSQL. */
function uuid(text) {
    const h = createHash('md5').update(text).digest('hex');
    return `${h.slice(0, 8)}-${h.slice(8, 12)}-${h.slice(12, 16)}-${h.slice(16, 20)}-${h.slice(20)}`;
}

const sql = value => value === null || value === undefined ? 'null' : `'${String(value).replace(/'/g, "''")}'`;
const normalize = name => name.normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase().replace(/pokemon/g, '').trim();

/** Timestamps without zone in the old database were written in Spanish local time. */
const madrid = value => value === null ? 'null' : `(timestamp ${sql(value)} at time zone 'Europe/Madrid')`;
const utc = value => value === null ? 'null' : `${sql(value)}::timestamptz`;

// ─── Load ────────────────────────────────────────────────────────────────────────────────────────

const old = name => readCsv(oldDir, name);
const juegos = old('juegos');
const modalidades = old('modalidades');
const torneos = old('torneos');
const jugadores = old('jugadores');
const clasificacion = old('clasificacion');
const batallas = [...old('batallas'), ...(existsSync(join(oldDir, 'batallas_faltantes.csv')) ? old('batallas_faltantes') : [])];
const recompensas = old('recompensas');
const grupos = old('grupos');
const grupoJugadores = old('grupo_jugadores');
const bracket = old('resultados_bracket');
const games = readCsv(currentDir, 'games');

// Old game id -> current game id, matched by name ("Pokemon FireRed" = "Pokémon FireRed").
const gameIdByOld = Object.fromEntries(juegos.map(juego => {
    const game = games.find(current => normalize(current.name) === normalize(juego.nombre));
    if (!game) throw new Error(`No current game matches ${juego.nombre}`);
    return [juego.id, game.id];
}));

const LOCKE_ID = uuid('ntg-pokemon:locke:genlocke-2025');
const playerId = oldId => uuid(`ntg-pokemon:player:${oldId}`);
const tournamentId = oldId => uuid(`ntg-pokemon:tournament:${oldId}`);
const tournamentPlayerId = (oldTournament, oldPlayer) => uuid(`ntg-pokemon:tournament-player:${oldTournament}:${oldPlayer}`);
const playerName = Object.fromEntries(jugadores.map(j => [j.id, j.nombre]));

// Tournaments in play order: the old ids grow with time.
const orderedTournaments = [...torneos].sort((a, b) => Number(a.id) - Number(b.id));
const groupByTournamentPlayer = {};
for (const gj of grupoJugadores) {
    const grupo = grupos.find(g => g.id === gj.grupo_id);
    groupByTournamentPlayer[`${grupo.torneo_id}:${gj.jugador_id}`] = grupo.nombre;
}
const hasGroups = torneo => grupos.some(g => g.torneo_id === torneo.id);

// Old bracket codes of the 6-player double elimination -> stage, round, slot.
const BRACKET_PLACES = {
    semis1: ['upper', 1, 1],
    semis2: ['upper', 1, 2],
    upperfinal: ['upper', 2, 1],
    lb1: ['lower', 1, 1],
    lb2: ['lower', 2, 1],
    lb3: ['lower', 3, 1],
    lowerfinal: ['lower', 4, 1],
    final: ['grand_final', 1, 1]
};

// ─── Series reconstruction ───────────────────────────────────────────────────────────────────────

/** Groups the per-game rows of a tournament into series and rebuilds their games. */
function buildSeries(rows, placeOf) {
    const series = new Map();
    const sorted = [...rows].sort((a, b) => a.created_at.localeCompare(b.created_at) || Number(a.id) - Number(b.id));
    for (const row of sorted) {
        const pair = [row.jugador1_id, row.jugador2_id].sort().join('-');
        const key = `${placeOf(row)}|${pair}`;
        if (!series.has(key)) {
            // Sides follow the first row of the series.
            series.set(key, { key, place: placeOf(row), side1: row.jugador1_id, side2: row.jugador2_id, firstAt: row.created_at, score: [0, 0], games: [], warnings: [] });
        }
        const s = series.get(key);
        const score1 = Number(row.jugador1_id === s.side1 ? row.resultado_jugador1 : row.resultado_jugador2);
        const score2 = Number(row.jugador1_id === s.side1 ? row.resultado_jugador2 : row.resultado_jugador1);
        const won1 = score1 - s.score[0];
        const won2 = score2 - s.score[1];
        if (won1 < 0 || won2 < 0) throw new Error(`Score goes back in row ${row.id}`);
        if (won1 + won2 === 0) continue; // Placeholder of a pending series.
        if (won1 + won2 > 1) s.warnings.push(`fila ${row.id}: ${won1 + won2} partidas en un solo paso; se añaden sin replay`);
        // Missing games first, the game of this row (the one with the replay) last.
        const lastWinner = won1 > 0 && (won2 === 0 || score1 >= score2) ? 1 : 2;
        const missing = [...Array(won1 - (lastWinner === 1 ? 1 : 0)).fill(1), ...Array(won2 - (lastWinner === 2 ? 1 : 0)).fill(2)];
        for (const side of missing) s.games.push({ side, replay: null, at: row.created_at, oldRow: row.id });
        s.games.push({ side: lastWinner, replay: row.replay_url, at: row.created_at, oldRow: row.id });
        s.score = [score1, score2];
    }
    return [...series.values()];
}

// ─── Build everything ────────────────────────────────────────────────────────────────────────────

const out = [];
const verify = [];
const warnings = [];
const emit = line => out.push(line);

emit(`-- =====================================================================================================
-- Pokémon module · 02 · Data of the old database
--
-- GENERATED by tools/generate-legacy-data.mjs from the CSV export. Do not edit by hand: fix the generator
-- and generate it again. Run after 01-schema.sql. It is idempotent: it deletes the randomlocke it creates
-- before inserting it again.
-- =====================================================================================================

begin;

-- ─── Games (current table, kept as it is) ────────────────────────────────────────────────────────
`);

for (const game of games) {
    emit(`insert into pokemon.games (id, name, description, image_url, created_at) values (${sql(game.id)}, ${sql(game.name)}, ${sql(game.description)}, ${sql(game.image_url)}, ${utc(game.created_at)}) on conflict (id) do nothing;`);
}

emit(`
-- ─── Randomlocke and players ─────────────────────────────────────────────────────────────────────

delete from pokemon.lockes where id = ${sql(LOCKE_ID)};

insert into pokemon.lockes (id, name, description, rules, created_at) values (${sql(LOCKE_ID)}, 'GenLocke 2025', ${sql('Randomlocke generacional jugado en Pokémon FireRed, HeartGold y Platino. Los puntos se acumulan de un juego al siguiente.')}, null, ${madrid(orderedTournaments[0].equipos_fecha_revelacion)});
`);

for (const jugador of jugadores) {
    emit(`insert into pokemon.players (id, name) values (${sql(playerId(jugador.id))}, ${sql(jugador.nombre)}) on conflict (id) do nothing;`);
    emit(`insert into pokemon.locke_players (locke_id, player_id) values (${sql(LOCKE_ID)}, ${sql(playerId(jugador.id))});`);
}

const teamImages = {}; // old file name -> new object name, for upload-legacy-team-images.mjs
const cumulative = {}; // old player id -> cumulative points stored after the previous tournament
const expected = [];

orderedTournaments.forEach((torneo, index) => {
    const modalidad = modalidades.find(m => m.id === torneo.modalidad_id);
    const tId = tournamentId(torneo.id);
    const format = hasGroups(torneo) ? 'groups_bracket' : 'round_robin';
    const tournamentBattles = batallas.filter(b => b.torneo_id === torneo.id);
    const tournamentBracket = bracket.filter(b => b.torneo_id === torneo.id);
    const standings = clasificacion.filter(c => c.torneo_id === torneo.id);
    const finished = index < orderedTournaments.length - 1 || recompensas.some(r => r.torneo_id === torneo.id && r.elegido_por);
    const firstAt = [...tournamentBattles, ...tournamentBracket].map(r => r.created_at).sort()[0] ?? null;

    emit(`
-- ─── Tournament ${index + 1}: ${torneo.nombre} (old id ${torneo.id}) ───
`);
    emit(`insert into pokemon.tournaments (id, locke_id, game_id, position, name, format, status, points_per_win, teams_reveal_at, created_at) values (${sql(tId)}, ${sql(LOCKE_ID)}, ${sql(gameIdByOld[modalidad.juego_id])}, ${index + 1}, ${sql(torneo.nombre)}, '${format}', '${finished ? 'finished' : 'in_progress'}', ${Number(torneo.puntos_por_victoria)}, ${madrid(torneo.equipos_fecha_revelacion)}, ${firstAt ? utc(firstAt) : 'now()'});`);

    const series = [
        ...buildSeries(tournamentBattles, () => (format === 'groups_bracket' ? 'group' : 'league')),
        ...buildSeries(tournamentBracket, row => row.partido_id)
    ];

    // Series wins per old player id, to derive the points adjustment.
    const wins = {};
    for (const s of series) {
        const [a, b] = s.score;
        if (Math.max(a, b) === 2) {
            const winner = a === 2 ? s.side1 : s.side2;
            wins[winner] = (wins[winner] ?? 0) + 1;
        } else if (s.games.length > 0) {
            warnings.push(`Torneo ${torneo.id}: serie ${playerName[s.side1]} vs ${playerName[s.side2]} sin terminar (${a}-${b})`);
        }
        s.warnings.forEach(w => warnings.push(`Torneo ${torneo.id}: ${w}`));
    }

    for (const c of standings) {
        const previous = cumulative[c.jugador_id] ?? 0;
        const earned = Number(c.puntos) - previous;
        const adjustment = earned - (wins[c.jugador_id] ?? 0) * Number(torneo.puntos_por_victoria);
        cumulative[c.jugador_id] = Number(c.puntos);
        const image = c.equipo_imagen_url ? `tournaments/${tId}/${tournamentPlayerId(torneo.id, c.jugador_id)}-legacy.webp` : null;
        if (image) teamImages[c.equipo_imagen_url] = image;
        emit(`insert into pokemon.tournament_players (id, tournament_id, player_id, group_name, team_image, points_adjustment) values (${sql(tournamentPlayerId(torneo.id, c.jugador_id))}, ${sql(tId)}, ${sql(playerId(c.jugador_id))}, ${sql(groupByTournamentPlayer[`${torneo.id}:${c.jugador_id}`] ?? null)}, ${sql(image)}, ${adjustment});`);
        expected.push({ position: index + 1, tournament: torneo.nombre, player: playerName[c.jugador_id], points: Number(c.puntos) });
    }

    // Slots: bracket places come from the old codes, group and league series are numbered in play order.
    const counters = {};
    for (const s of series.sort((x, y) => x.firstAt.localeCompare(y.firstAt))) {
        let stage, round, slot;
        if (BRACKET_PLACES[s.place]) {
            [stage, round, slot] = BRACKET_PLACES[s.place];
        } else if (s.place === 'group' || s.place === 'league') {
            stage = s.place;
            round = 1;
            slot = counters[stage] = (counters[stage] ?? 0) + 1;
        } else {
            throw new Error(`Unknown bracket code ${s.place}`);
        }
        const mId = uuid(`ntg-pokemon:match:${torneo.id}:${s.key}`);
        emit(`insert into pokemon.matches (id, tournament_id, stage, round, slot, player1_id, player2_id, created_at) values (${sql(mId)}, ${sql(tId)}, '${stage}', ${round}, ${slot}, ${sql(tournamentPlayerId(torneo.id, s.side1))}, ${sql(tournamentPlayerId(torneo.id, s.side2))}, ${utc(s.firstAt)});`);
        s.games.forEach((game, i) => {
            emit(`insert into pokemon.match_games (match_id, game_number, winner_side, replay_url, played_at) values (${sql(mId)}, ${i + 1}, ${game.side}, ${sql(game.replay)}, ${utc(game.at)});`);
        });
    }

    // Rewards keep the old order; the picker is matched by name.
    recompensas
        .filter(r => r.torneo_id === torneo.id)
        .sort((a, b) => Number(a.id) - Number(b.id))
        .forEach((reward, i) => {
            const picker = reward.elegido_por ? jugadores.find(j => j.nombre === reward.elegido_por) : null;
            if (reward.elegido_por && !picker) throw new Error(`Unknown reward picker ${reward.elegido_por}`);
            emit(`insert into pokemon.rewards (tournament_id, sort_order, name, chosen_by) values (${sql(tId)}, ${i + 1}, ${sql(reward.nombre)}, ${picker ? sql(tournamentPlayerId(torneo.id, picker.id)) : 'null'});`);
        });
});

emit(`
commit;`);

// ─── Verification ────────────────────────────────────────────────────────────────────────────────

verify.push(`-- =====================================================================================================
-- Pokémon module · 04 · Verification of the migrated data
--
-- GENERATED by tools/generate-legacy-data.mjs. Compares the cumulative points of every player after
-- every tournament, computed from the new tables, with the points stored in the old database.
-- An empty result means everything matches.
-- =====================================================================================================

with expected(position, player, points) as (
    values
${expected.map(e => `        (${e.position}, ${sql(e.player)}, ${e.points})`).join(',\n')}
),
series as (
    select m.id, m.tournament_id, m.player1_id, m.player2_id,
           count(*) filter (where g.winner_side = 1) as wins1,
           count(*) filter (where g.winner_side = 2) as wins2
    from pokemon.matches m
    join pokemon.match_games g on g.match_id = m.id
    group by m.id
),
series_wins as (
    select case when wins1 = 2 then player1_id else player2_id end as tournament_player_id, count(*) as wins
    from series
    where greatest(wins1, wins2) = 2
    group by 1
),
earned as (
    select t.position, p.name as player,
           coalesce(w.wins, 0) * t.points_per_win + tp.points_adjustment as points
    from pokemon.tournament_players tp
    join pokemon.tournaments t on t.id = tp.tournament_id
    join pokemon.players p on p.id = tp.player_id
    left join series_wins w on w.tournament_player_id = tp.id
    where t.locke_id = ${sql(LOCKE_ID)}
),
actual as (
    select position, player, sum(points) over (partition by player order by position) as points
    from earned
)
select e.position, e.player, e.points as expected, a.points as actual
from expected e
left join actual a on a.position = e.position and a.player = e.player
where a.points is distinct from e.points
order by e.position, e.player;
`);

writeFileSync(join(outDir, '02-legacy-data.sql'), out.join('\n') + '\n');
writeFileSync(join(outDir, '04-verify.sql'), verify.join('\n'));
writeFileSync(join(outDir, 'legacy-team-images.json'), JSON.stringify(teamImages, null, 4) + '\n');

const totalGames = out.filter(line => line.startsWith('insert into pokemon.match_games')).length;
const totalMatches = out.filter(line => line.startsWith('insert into pokemon.matches')).length;
console.log(`Series: ${totalMatches} · partidas: ${totalGames} · filas antiguas: ${batallas.length + bracket.length}`);
console.log('Avisos:');
warnings.forEach(w => console.log(`  - ${w}`));
