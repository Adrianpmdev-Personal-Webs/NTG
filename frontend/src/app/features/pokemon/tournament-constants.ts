/**
 * tournament-constants
 *
 * Tournament constants.
 * Centralizes tournament types, statuses and default values used by tournament flows.
 *
 * @author @adrianpmdev
 */

export const TOURNAMENT_TYPE_OPTIONS = [
    {
        value: 'grupos',
        label: 'GRUPOS'
    },
    {
        value: 'bracket',
        label: 'BRACKET'
    },
    {
        value: 'todos_vs_todos',
        label: 'TODOS VS TODOS'
    }
] as const;

export const TOURNAMENT_STATUS_OPTIONS = [
    {
        value: 'draft',
        label: 'DRAFT'
    },
    {
        value: 'grupos_en_curso',
        label: 'GRUPOS EN CURSO'
    },
    {
        value: 'grupos_finalizado',
        label: 'GRUPOS FINALIZADO'
    },
    {
        value: 'bracket_en_curso',
        label: 'BRACKET EN CURSO'
    },
    {
        value: 'finalizado',
        label: 'FINALIZADO'
    }
] as const;

export const TOURNAMENT_PARTICIPANT_STATUS_OPTIONS = [
    {
        value: 'activo',
        label: 'ACTIVO'
    },
    {
        value: 'eliminado',
        label: 'ELIMINADO'
    },
    {
        value: 'retirado',
        label: 'RETIRADO'
    }
] as const;

export const TOURNAMENT_GROUP_STATUS_OPTIONS = [
    {
        value: 'en_curso',
        label: 'EN CURSO'
    },
    {
        value: 'finalizado',
        label: 'FINALIZADO'
    }
] as const;

export const GROUP_MATCH_STATUS_OPTIONS = [
    {
        value: 'pendiente',
        label: 'PENDIENTE'
    },
    {
        value: 'en_curso',
        label: 'EN CURSO'
    },
    {
        value: 'completado',
        label: 'COMPLETADO'
    }
] as const;

export const BRACKET_STAGE_STATUS_OPTIONS = [
    {
        value: 'pendiente',
        label: 'PENDIENTE'
    },
    {
        value: 'en_curso',
        label: 'EN CURSO'
    },
    {
        value: 'finalizado',
        label: 'FINALIZADO'
    }
] as const;

export const BRACKET_MATCH_STATUS_OPTIONS = [
    {
        value: 'pendiente',
        label: 'PENDIENTE'
    },
    {
        value: 'en_curso',
        label: 'EN CURSO'
    },
    {
        value: 'completado',
        label: 'COMPLETADO'
    }
] as const;

export const DEFAULT_TOURNAMENT_TYPE = TOURNAMENT_TYPE_OPTIONS[0].value;
export const DEFAULT_TOURNAMENT_STATUS = TOURNAMENT_STATUS_OPTIONS[0].value;
export const DEFAULT_TOURNAMENT_PARTICIPANT_STATUS = TOURNAMENT_PARTICIPANT_STATUS_OPTIONS[0].value;
export const DEFAULT_TOURNAMENT_GROUP_STATUS = TOURNAMENT_GROUP_STATUS_OPTIONS[0].value;
export const DEFAULT_GROUP_MATCH_STATUS = GROUP_MATCH_STATUS_OPTIONS[0].value;
export const DEFAULT_BRACKET_STAGE_STATUS = BRACKET_STAGE_STATUS_OPTIONS[0].value;
export const DEFAULT_BRACKET_MATCH_STATUS = BRACKET_MATCH_STATUS_OPTIONS[0].value;

export type TournamentType = typeof TOURNAMENT_TYPE_OPTIONS[number]['value'];
export type TournamentStatus = typeof TOURNAMENT_STATUS_OPTIONS[number]['value'];
export type TournamentParticipantStatus = typeof TOURNAMENT_PARTICIPANT_STATUS_OPTIONS[number]['value'];
export type TournamentGroupStatus = typeof TOURNAMENT_GROUP_STATUS_OPTIONS[number]['value'];
export type GroupMatchStatus = typeof GROUP_MATCH_STATUS_OPTIONS[number]['value'];
export type BracketStageStatus = typeof BRACKET_STAGE_STATUS_OPTIONS[number]['value'];
export type BracketMatchStatus = typeof BRACKET_MATCH_STATUS_OPTIONS[number]['value'];
