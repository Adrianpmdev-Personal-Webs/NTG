/**
 * pokemon-models
 *
 * Types of the Pokémon API (requests and responses), mirroring the backend DTOs.
 *
 * @author @adrianpmdev
 */

import { LockeRules } from './locke-rules';

export interface Ref {
    id: string;
    name: string;
}

export type TournamentFormat = 'groups_bracket' | 'round_robin';
export type TournamentStatus = 'draft' | 'in_progress' | 'finished';
export type MatchStage = 'group' | 'league' | 'upper' | 'lower' | 'grand_final';
export type MatchStatus = 'pending' | 'ready' | 'finished' | 'bye' | 'void';
export type GroupName = 'A' | 'B';

export interface Game {
    id: string;
    name: string;
    description: string | null;
    imageUrl: string;
    lockeCount: number;
}

export interface GameRequest {
    name: string;
    description: string | null;
    imageUrl: string;
}

export interface Player {
    id: string;
    name: string;
    userId: string | null;
}

export interface PlayerRequest {
    name: string;
    userId: string | null;
}

export interface LockeSummary {
    id: string;
    name: string;
    description: string | null;
    games: Ref[];
    tournamentCount: number;
    playerCount: number;
}

export interface TournamentSummary {
    id: string;
    game: Ref;
    position: number;
    name: string;
    format: TournamentFormat;
    status: TournamentStatus;
    champion: Ref | null;
}

export interface PlayerStanding {
    playerId: string;
    name: string;
    position: number;
    points: number;
    wins: number;
    losses: number;
    tournaments: number;
    titles: number;
    lockes: number;
}

export interface LockeDetail {
    id: string;
    name: string;
    description: string | null;
    rules: LockeRules | null;
    games: Ref[];
    roster: Ref[];
    tournaments: TournamentSummary[];
    standings: PlayerStanding[];
}

export interface LockeRequest {
    name: string;
    description: string | null;
    rules: LockeRules | null;
}

export interface TournamentPlayer {
    id: string;
    playerId: string;
    name: string;
    group: GroupName | null;
    pointsAdjustment: number;
    hasTeamImage: boolean;
    teamImageUrl: string | null;
    mine: boolean;
    canEditTeam: boolean;
}

export interface MatchGame {
    number: number;
    winnerSide: 1 | 2;
    replayUrl: string | null;
    playedAt: string;
}

export interface Match {
    id: string;
    stage: MatchStage;
    round: number;
    slot: number;
    player1Id: string | null;
    player2Id: string | null;
    score1: number;
    score2: number;
    winnerId: string | null;
    status: MatchStatus;
    games: MatchGame[];
}

export interface Standing {
    tournamentPlayerId: string;
    position: number;
    played: number;
    wins: number;
    losses: number;
    gamesWon: number;
    gamesLost: number;
    points: number;
}

export interface GroupStandings {
    group: GroupName;
    standings: Standing[];
}

export interface Reward {
    id: string;
    sortOrder: number;
    name: string;
    chosenBy: string | null;
}

export interface TournamentDetail {
    id: string;
    locke: Ref;
    game: Ref;
    position: number;
    name: string;
    description: string | null;
    format: TournamentFormat;
    status: TournamentStatus;
    pointsPerWin: number;
    teamsRevealAt: string | null;
    teamsRevealed: boolean;
    bracketSize: number | null;
    players: TournamentPlayer[];
    matches: Match[];
    standings: Standing[];
    groups: GroupStandings[];
    rewards: Reward[];
}

export interface TournamentCreateRequest {
    lockeId: string;
    gameId: string;
    name: string;
    description: string | null;
    format: TournamentFormat;
    pointsPerWin: number;
    teamsRevealAt: string | null;
}

export interface TournamentUpdateRequest {
    gameId: string;
    name: string;
    description: string | null;
    status: TournamentStatus;
    pointsPerWin: number;
    teamsRevealAt: string | null;
}

export interface TournamentPlayerUpdateRequest {
    group: GroupName | null;
    pointsAdjustment: number;
}

export interface MatchGameInput {
    winnerSide: 1 | 2;
    replayUrl: string | null;
}

export interface MatchCreateRequest {
    stage: MatchStage;
    round: number;
    slot: number;
    player1Id: string;
    player2Id: string;
}

export interface RewardRequest {
    name: string;
    chosenBy: string | null;
}
