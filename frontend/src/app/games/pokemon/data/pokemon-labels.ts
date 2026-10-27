/**
 * pokemon-labels
 *
 * Spanish texts of the values the API sends as codes (formats, statuses, stages) and the helpers that
 * name the rounds of a bracket.
 *
 * @author @adrianpmdev
 */

import { MatchStage, MatchStatus, TournamentFormat, TournamentStatus } from './pokemon-models';

export const FORMAT_LABELS: Record<TournamentFormat, string> = {
    groups_bracket: 'Grupos + bracket',
    round_robin: 'Todos contra todos'
};

export const STATUS_LABELS: Record<TournamentStatus, string> = {
    draft: 'Preparación',
    in_progress: 'En curso',
    finished: 'Finalizado'
};

export const MATCH_STATUS_LABELS: Record<MatchStatus, string> = {
    pending: 'Pendiente',
    ready: 'Por jugar',
    finished: 'Jugada',
    bye: 'Pase directo',
    void: 'Sin jugadores'
};

export const STAGE_LABELS: Record<MatchStage, string> = {
    group: 'Fase de grupos',
    league: 'Liga',
    upper: 'Cuadro superior',
    lower: 'Cuadro inferior',
    grand_final: 'Gran final'
};

export const FORMAT_OPTIONS = Object.entries(FORMAT_LABELS).map(([value, label]) => ({ value: value as TournamentFormat, label }));
export const STATUS_OPTIONS = Object.entries(STATUS_LABELS).map(([value, label]) => ({ value: value as TournamentStatus, label }));

/**
 * Name of a bracket round.
 *
 * @param stage upper, lower or grand_final
 * @param round round number
 * @param rounds number of rounds of that side
 */
export function roundLabel(stage: MatchStage, round: number, rounds: number): string {
    if (stage === 'grand_final') {
        return 'Gran final';
    }

    if (stage === 'upper') {
        const fromEnd = rounds - round;

        return fromEnd === 0 ? 'Final superior' : fromEnd === 1 ? 'Semifinales' : fromEnd === 2 ? 'Cuartos' : `Ronda ${round}`;
    }

    if (stage === 'lower') {
        return round === rounds ? 'Final inferior' : `Inferior ${round}`;
    }

    return `Jornada ${round}`;
}
