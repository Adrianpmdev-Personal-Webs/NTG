/**
 * tournament-view
 *
 * Pure helpers that shape a TournamentDetail for the screens: players by id, series grouped by stage and
 * round, and the rows of the standings tables.
 *
 * @author @adrianpmdev
 */

import { GroupName, Match, MatchStage, PlayerStanding, Standing, TournamentDetail, TournamentPlayer } from './pokemon-models';

/** One row of a standings table, whatever it ranks. */
export interface StandingRow {
    id: string;
    position: number;
    name: string;
    played: number;
    wins: number;
    losses: number;
    /** "won-lost" games, or null when the table does not count games. */
    games: string | null;
    points: number;
    /** Extra column (titles, tournaments...), or null. */
    extra: string | null;
    highlight: boolean;
}

/** Series of one round. */
export interface RoundView {
    round: number;
    matches: Match[];
}

/** Players of a tournament by tournament player id. */
export function playersById(detail: TournamentDetail): Map<string, TournamentPlayer> {
    return new Map(detail.players.map(player => [player.id, player]));
}

/** Series of a stage, grouped by round and sorted by slot. */
export function roundsOf(matches: Match[], stage: MatchStage): RoundView[] {
    const rounds = new Map<number, Match[]>();

    matches
        .filter(match => match.stage === stage)
        .sort((a, b) => a.round - b.round || a.slot - b.slot)
        .forEach(match => rounds.set(match.round, [...(rounds.get(match.round) ?? []), match]));

    return [...rounds.entries()].map(([round, list]) => ({ round, matches: list }));
}

/** Group series of one group (both players belong to it). */
export function groupMatches(detail: TournamentDetail, group: GroupName): Match[] {
    const players = playersById(detail);

    return detail.matches.filter(match => match.stage === 'group' && players.get(match.player1Id ?? '')?.group === group);
}

export function hasBracket(detail: TournamentDetail): boolean {
    return detail.matches.some(match => match.stage === 'upper' || match.stage === 'lower' || match.stage === 'grand_final');
}

/** Rows of a tournament or group table. */
export function tournamentRows(standings: Standing[], detail: TournamentDetail): StandingRow[] {
    const players = playersById(detail);

    return standings.map(row => ({
        id: row.tournamentPlayerId,
        position: row.position,
        name: players.get(row.tournamentPlayerId)?.name ?? '—',
        played: row.played,
        wins: row.wins,
        losses: row.losses,
        games: `${row.gamesWon}-${row.gamesLost}`,
        points: row.points,
        extra: null,
        highlight: players.get(row.tournamentPlayerId)?.mine ?? false
    }));
}

/** Rows of the standings of a randomlocke or of the global ranking. */
export function playerRows(standings: PlayerStanding[], extra: (row: PlayerStanding) => string): StandingRow[] {
    return standings.map(row => ({
        id: row.playerId,
        position: row.position,
        name: row.name,
        played: row.wins + row.losses,
        wins: row.wins,
        losses: row.losses,
        games: null,
        points: row.points,
        extra: extra(row),
        highlight: false
    }));
}

/** Local date and time for a timestamp, or a dash. */
export function formatDateTime(value: string | null): string {
    return value
        ? new Date(value).toLocaleString('es-ES', { day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit' })
        : '—';
}
