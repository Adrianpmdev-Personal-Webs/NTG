/**
 * PokemonApi
 *
 * Service for all Pokémon tournament system API operations.
 *
 * @author @adrianpmdev
 */

import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '@core/config/api';
import {
    GameResponse, GameCreateRequest, GameUpdateRequest,
    LockeResponse, LockeCreateRequest, LockeUpdateRequest,
    TournamentResponse, TournamentCreateRequest, TournamentUpdateRequest,
    TournamentParticipantResponse, TournamentParticipantCreateRequest, TournamentParticipantUpdateRequest,
    TournamentGroupResponse, TournamentGroupCreateRequest, TournamentGroupUpdateRequest,
    GroupParticipantResponse, GroupParticipantCreateRequest, GroupParticipantUpdateRequest,
    GroupMatchResponse, GroupMatchCreateRequest, GroupMatchUpdateRequest,
    BracketStageResponse, BracketStageCreateRequest, BracketStageUpdateRequest,
    BracketMatchResponse, BracketMatchCreateRequest, BracketMatchUpdateRequest,
    TournamentStandingResponse, TournamentStandingCreateRequest, TournamentStandingUpdateRequest,
    RewardResponse, RewardCreateRequest, RewardUpdateRequest,
    UserRewardResponse, UserRewardCreateRequest, UserRewardUpdateRequest,
    GlobalStandingResponse, GlobalStandingCreateRequest, GlobalStandingUpdateRequest
} from './pokemon-models';

@Injectable({ providedIn: 'root' })
export class PokemonApi {

    private readonly http = inject(HttpClient);
    private readonly base = `${API_BASE_URL}/pokemon`;

    // ── GAMES ──
    getAllGames(): Observable<GameResponse[]> { return this.http.get<GameResponse[]>(`${this.base}/games`); }
    getGameById(id: string): Observable<GameResponse> { return this.http.get<GameResponse>(`${this.base}/games/${id}`); }
    createGame(req: GameCreateRequest): Observable<GameResponse> { return this.http.post<GameResponse>(`${this.base}/games`, req); }
    updateGame(id: string, req: GameUpdateRequest): Observable<GameResponse> { return this.http.put<GameResponse>(`${this.base}/games/${id}`, req); }
    deleteGame(id: string): Observable<void> { return this.http.delete<void>(`${this.base}/games/${id}`); }

    // ── LOCKES ──
    getAllLockes(): Observable<LockeResponse[]> { return this.http.get<LockeResponse[]>(`${this.base}/lockes`); }
    getLockesByGameId(gameId: string): Observable<LockeResponse[]> { return this.http.get<LockeResponse[]>(`${this.base}/lockes/game/${gameId}`); }
    getLockeById(id: string): Observable<LockeResponse> { return this.http.get<LockeResponse>(`${this.base}/lockes/${id}`); }
    createLocke(req: LockeCreateRequest): Observable<LockeResponse> { return this.http.post<LockeResponse>(`${this.base}/lockes`, req); }
    updateLocke(id: string, req: LockeUpdateRequest): Observable<LockeResponse> { return this.http.put<LockeResponse>(`${this.base}/lockes/${id}`, req); }
    deleteLocke(id: string): Observable<void> { return this.http.delete<void>(`${this.base}/lockes/${id}`); }

    // ── TOURNAMENTS ──
    getAllTournaments(): Observable<TournamentResponse[]> { return this.http.get<TournamentResponse[]>(`${this.base}/tournaments`); }
    getTournamentById(id: string): Observable<TournamentResponse> { return this.http.get<TournamentResponse>(`${this.base}/tournaments/${id}`); }
    getTournamentsByLockeId(lockeId: string): Observable<TournamentResponse[]> { return this.http.get<TournamentResponse[]>(`${this.base}/tournaments/locke/${lockeId}`); }
    createTournament(req: TournamentCreateRequest): Observable<TournamentResponse> { return this.http.post<TournamentResponse>(`${this.base}/tournaments`, req); }
    updateTournament(id: string, req: TournamentUpdateRequest): Observable<TournamentResponse> { return this.http.put<TournamentResponse>(`${this.base}/tournaments/${id}`, req); }
    deleteTournament(id: string): Observable<void> { return this.http.delete<void>(`${this.base}/tournaments/${id}`); }

    // ── TOURNAMENT PARTICIPANTS ──
    getParticipantsByTournament(tournamentId: string): Observable<TournamentParticipantResponse[]> { return this.http.get<TournamentParticipantResponse[]>(`${this.base}/tournament-participants/tournament/${tournamentId}`); }
    createParticipant(req: TournamentParticipantCreateRequest): Observable<TournamentParticipantResponse> { return this.http.post<TournamentParticipantResponse>(`${this.base}/tournament-participants`, req); }
    updateParticipant(id: string, req: TournamentParticipantUpdateRequest): Observable<TournamentParticipantResponse> { return this.http.put<TournamentParticipantResponse>(`${this.base}/tournament-participants/${id}`, req); }
    deleteParticipant(id: string): Observable<void> { return this.http.delete<void>(`${this.base}/tournament-participants/${id}`); }

    // ── TOURNAMENT GROUPS ──
    getGroupsByTournament(tournamentId: string): Observable<TournamentGroupResponse[]> { return this.http.get<TournamentGroupResponse[]>(`${this.base}/tournament-groups/tournament/${tournamentId}`); }
    createGroup(req: TournamentGroupCreateRequest): Observable<TournamentGroupResponse> { return this.http.post<TournamentGroupResponse>(`${this.base}/tournament-groups`, req); }
    updateGroup(id: string, req: TournamentGroupUpdateRequest): Observable<TournamentGroupResponse> { return this.http.put<TournamentGroupResponse>(`${this.base}/tournament-groups/${id}`, req); }
    deleteGroup(id: string): Observable<void> { return this.http.delete<void>(`${this.base}/tournament-groups/${id}`); }

    // ── GROUP PARTICIPANTS ──
    getGroupParticipants(groupId: string): Observable<GroupParticipantResponse[]> { return this.http.get<GroupParticipantResponse[]>(`${this.base}/group-participants/group/${groupId}`); }
    createGroupParticipant(req: GroupParticipantCreateRequest): Observable<GroupParticipantResponse> { return this.http.post<GroupParticipantResponse>(`${this.base}/group-participants`, req); }
    updateGroupParticipant(id: string, req: GroupParticipantUpdateRequest): Observable<GroupParticipantResponse> { return this.http.put<GroupParticipantResponse>(`${this.base}/group-participants/${id}`, req); }
    deleteGroupParticipant(id: string): Observable<void> { return this.http.delete<void>(`${this.base}/group-participants/${id}`); }

    // ── GROUP MATCHES ──
    getGroupMatches(groupId: string): Observable<GroupMatchResponse[]> { return this.http.get<GroupMatchResponse[]>(`${this.base}/group-matches/group/${groupId}`); }
    createGroupMatch(req: GroupMatchCreateRequest): Observable<GroupMatchResponse> { return this.http.post<GroupMatchResponse>(`${this.base}/group-matches`, req); }
    updateGroupMatch(id: string, req: GroupMatchUpdateRequest): Observable<GroupMatchResponse> { return this.http.put<GroupMatchResponse>(`${this.base}/group-matches/${id}`, req); }
    deleteGroupMatch(id: string): Observable<void> { return this.http.delete<void>(`${this.base}/group-matches/${id}`); }

    // ── BRACKET STAGES ──
    getBracketStagesByTournament(tournamentId: string): Observable<BracketStageResponse[]> { return this.http.get<BracketStageResponse[]>(`${this.base}/bracket-stages/tournament/${tournamentId}`); }
    createBracketStage(req: BracketStageCreateRequest): Observable<BracketStageResponse> { return this.http.post<BracketStageResponse>(`${this.base}/bracket-stages`, req); }
    updateBracketStage(id: string, req: BracketStageUpdateRequest): Observable<BracketStageResponse> { return this.http.put<BracketStageResponse>(`${this.base}/bracket-stages/${id}`, req); }
    deleteBracketStage(id: string): Observable<void> { return this.http.delete<void>(`${this.base}/bracket-stages/${id}`); }

    // ── BRACKET MATCHES ──
    getBracketMatchesByStage(stageId: string): Observable<BracketMatchResponse[]> { return this.http.get<BracketMatchResponse[]>(`${this.base}/bracket-matches/stage/${stageId}`); }
    createBracketMatch(req: BracketMatchCreateRequest): Observable<BracketMatchResponse> { return this.http.post<BracketMatchResponse>(`${this.base}/bracket-matches`, req); }
    updateBracketMatch(id: string, req: BracketMatchUpdateRequest): Observable<BracketMatchResponse> { return this.http.put<BracketMatchResponse>(`${this.base}/bracket-matches/${id}`, req); }
    deleteBracketMatch(id: string): Observable<void> { return this.http.delete<void>(`${this.base}/bracket-matches/${id}`); }

    // ── TOURNAMENT STANDINGS ──
    getStandingsByTournament(tournamentId: string): Observable<TournamentStandingResponse[]> { return this.http.get<TournamentStandingResponse[]>(`${this.base}/tournament-standings/tournament/${tournamentId}`); }
    createStanding(req: TournamentStandingCreateRequest): Observable<TournamentStandingResponse> { return this.http.post<TournamentStandingResponse>(`${this.base}/tournament-standings`, req); }
    updateStanding(id: string, req: TournamentStandingUpdateRequest): Observable<TournamentStandingResponse> { return this.http.put<TournamentStandingResponse>(`${this.base}/tournament-standings/${id}`, req); }
    deleteStanding(id: string): Observable<void> { return this.http.delete<void>(`${this.base}/tournament-standings/${id}`); }

    // ── REWARDS ──
    getRewardsByTournament(tournamentId: string): Observable<RewardResponse[]> { return this.http.get<RewardResponse[]>(`${this.base}/rewards/tournament/${tournamentId}`); }
    createReward(req: RewardCreateRequest): Observable<RewardResponse> { return this.http.post<RewardResponse>(`${this.base}/rewards`, req); }
    updateReward(id: string, req: RewardUpdateRequest): Observable<RewardResponse> { return this.http.put<RewardResponse>(`${this.base}/rewards/${id}`, req); }
    deleteReward(id: string): Observable<void> { return this.http.delete<void>(`${this.base}/rewards/${id}`); }

    // ── USER REWARDS ──
    getUserRewardsByTournament(tournamentId: string): Observable<UserRewardResponse[]> { return this.http.get<UserRewardResponse[]>(`${this.base}/user-rewards/tournament/${tournamentId}`); }
    createUserReward(req: UserRewardCreateRequest): Observable<UserRewardResponse> { return this.http.post<UserRewardResponse>(`${this.base}/user-rewards`, req); }
    updateUserReward(id: string, req: UserRewardUpdateRequest): Observable<UserRewardResponse> { return this.http.put<UserRewardResponse>(`${this.base}/user-rewards/${id}`, req); }
    deleteUserReward(id: string): Observable<void> { return this.http.delete<void>(`${this.base}/user-rewards/${id}`); }

    // ── GLOBAL STANDINGS ──
    getAllGlobalStandings(): Observable<GlobalStandingResponse[]> { return this.http.get<GlobalStandingResponse[]>(`${this.base}/global-standings`); }
    createGlobalStanding(req: GlobalStandingCreateRequest): Observable<GlobalStandingResponse> { return this.http.post<GlobalStandingResponse>(`${this.base}/global-standings`, req); }
    updateGlobalStanding(id: string, req: GlobalStandingUpdateRequest): Observable<GlobalStandingResponse> { return this.http.put<GlobalStandingResponse>(`${this.base}/global-standings/${id}`, req); }
    deleteGlobalStanding(id: string): Observable<void> { return this.http.delete<void>(`${this.base}/global-standings/${id}`); }
}
