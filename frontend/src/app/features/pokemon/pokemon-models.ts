/**
 * PokemonModels
 *
 * Request and response models of the Pokemon API: games, lockes, tournaments and related entities.
 *
 * @author @adrianpmdev
 */

/**
 * pokemon-models
 *
 * 
 *
 * @author @adrianpmdev
 */

import { LockeRules } from './locke-rules';

// ─── GAME ───
export interface GameResponse {
    id: string;
    name: string;
    description?: string;
    imageUrl: string;
    createdAt: string;
}
export interface GameCreateRequest { name: string; description?: string; imageUrl: string; }
export interface GameUpdateRequest { name?: string; description?: string; imageUrl?: string; }

// ─── LOCKE ───
export interface LockeResponse {
    id: string;
    gameId: string;
    name: string;
    description?: string;
    rules?: LockeRules;
    createdAt: string;
}
export interface LockeCreateRequest { gameId: string; name: string; description?: string; rules?: any; }
export interface LockeUpdateRequest { name?: string; description?: string; rules?: any; }

// ─── TOURNAMENT ───
export interface TournamentResponse {
    id: string;
    lockeId: string;
    name: string;
    description?: string;
    tournamentType: string;
    status: string;
    maxParticipants?: number;
    startDate?: string;
    endDate?: string;
    createdAt: string;
}
export interface TournamentCreateRequest {
    lockeId: string; name: string; description?: string;
    tournamentType: string; status: string;
    maxParticipants?: number; startDate?: string; endDate?: string;
}
export interface TournamentUpdateRequest {
    name?: string; description?: string; tournamentType?: string;
    status?: string; maxParticipants?: number; startDate?: string; endDate?: string;
}

// ─── TOURNAMENT PARTICIPANT ───
export interface TournamentParticipantResponse {
    id: string; tournamentId: string; userId: string; joinedAt: string; status: string;
}
export interface TournamentParticipantCreateRequest { tournamentId: string; userId: string; status: string; }
export interface TournamentParticipantUpdateRequest { status: string; }

// ─── TOURNAMENT GROUP ───
export interface TournamentGroupResponse {
    id: string; tournamentId: string; groupLetter: string; status: string; createdAt: string;
}
export interface TournamentGroupCreateRequest { tournamentId: string; groupLetter: string; status: string; }
export interface TournamentGroupUpdateRequest { groupLetter?: string; status?: string; }

// ─── GROUP PARTICIPANT ───
export interface GroupParticipantResponse {
    id: string; groupId: string; tournamentParticipantId: string;
    groupPosition?: number; wins: number; losses: number; points: number;
}
export interface GroupParticipantCreateRequest {
    groupId: string; tournamentParticipantId: string;
    groupPosition?: number; wins?: number; losses?: number; points?: number;
}
export interface GroupParticipantUpdateRequest { groupPosition?: number; wins?: number; losses?: number; points?: number; }

// ─── GROUP MATCH ───
export interface GroupMatchResponse {
    id: string; groupId: string; participant1Id: string; participant2Id: string;
    winnerId?: string; replayUrl?: string; status: string;
    scheduledAt?: string; completedAt?: string; createdAt: string;
}
export interface GroupMatchCreateRequest {
    groupId: string; participant1Id: string; participant2Id: string;
    status: string; replayUrl?: string; scheduledAt?: string;
}
export interface GroupMatchUpdateRequest { winnerId?: string; replayUrl?: string; status?: string; scheduledAt?: string; completedAt?: string; }

// ─── BRACKET STAGE ───
export interface BracketStageResponse {
    id: string; tournamentId: string; stageName: string; stageOrder: number; status: string; createdAt: string;
}
export interface BracketStageCreateRequest { tournamentId: string; stageName: string; stageOrder: number; status: string; }
export interface BracketStageUpdateRequest { stageName?: string; stageOrder?: number; status?: string; }

// ─── BRACKET MATCH ───
export interface BracketMatchResponse {
    id: string; tournamentId: string; bracketStageId: string; matchNumber: number;
    participant1Id?: string; participant2Id?: string; winnerId?: string;
    replayUrl?: string; status: string; scheduledAt?: string; completedAt?: string; createdAt: string;
}
export interface BracketMatchCreateRequest {
    tournamentId: string; bracketStageId: string; matchNumber: number;
    participant1Id?: string; participant2Id?: string; status: string;
    replayUrl?: string; scheduledAt?: string;
}
export interface BracketMatchUpdateRequest {
    participant1Id?: string; participant2Id?: string; winnerId?: string;
    replayUrl?: string; status?: string; scheduledAt?: string; completedAt?: string;
}

// ─── TOURNAMENT STANDING ───
export interface TournamentStandingResponse {
    id: string; tournamentId: string; tournamentParticipantId: string;
    finalPosition?: number; totalWins: number; totalLosses: number; totalPoints: number; updatedAt: string;
}
export interface TournamentStandingCreateRequest {
    tournamentId: string; tournamentParticipantId: string;
    finalPosition?: number; totalWins?: number; totalLosses?: number; totalPoints?: number;
}
export interface TournamentStandingUpdateRequest { finalPosition?: number; totalWins?: number; totalLosses?: number; totalPoints?: number; }

// ─── REWARD ───
export interface RewardResponse {
    id: string; tournamentId: string; position: number; rewardName: string; rewardValue?: number; imageUrl?: string;
}
export interface RewardCreateRequest { tournamentId: string; position: number; rewardName: string; rewardValue?: number; imageUrl?: string; }
export interface RewardUpdateRequest { position?: number; rewardName?: string; rewardValue?: number; imageUrl?: string; }

// ─── USER REWARD ───
export interface UserRewardResponse {
    id: string; userId: string; rewardId: string; tournamentId: string; claimedAt: string;
}
export interface UserRewardCreateRequest { userId: string; rewardId: string; tournamentId: string; }
export interface UserRewardUpdateRequest { userId?: string; rewardId?: string; }

// ─── GLOBAL STANDING ───
export interface GlobalStandingResponse {
    id: string; userId: string; totalTournaments: number;
    tournamentsWon: number; totalPoints: number; rankingPosition?: number; updatedAt: string;
}
export interface GlobalStandingCreateRequest { userId: string; totalTournaments?: number; tournamentsWon?: number; totalPoints?: number; rankingPosition?: number; }
export interface GlobalStandingUpdateRequest { totalTournaments?: number; tournamentsWon?: number; totalPoints?: number; rankingPosition?: number; }
