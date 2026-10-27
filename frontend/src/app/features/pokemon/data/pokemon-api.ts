/**
 * PokemonApi
 *
 * HTTP service of the Pokémon module: one method per endpoint. Every admin action on a tournament
 * answers with the whole updated tournament, so screens just replace their copy with the response.
 *
 * @author @adrianpmdev
 */

import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '@core/config/api';
import {
    Game, GameRequest, LockeDetail, LockeRequest, LockeSummary, MatchCreateRequest, MatchGameInput, Player,
    PlayerRequest, PlayerStanding, RewardRequest, TournamentCreateRequest, TournamentDetail,
    TournamentPlayerUpdateRequest, TournamentUpdateRequest
} from './pokemon-models';

@Injectable({ providedIn: 'root' })
export class PokemonApi {

    private readonly http = inject(HttpClient);
    private readonly base = `${API_BASE_URL}/pokemon`;

    // ─── Games ───

    getGames(): Observable<Game[]> {
        return this.http.get<Game[]>(`${this.base}/games`);
    }

    getGame(id: string): Observable<Game> {
        return this.http.get<Game>(`${this.base}/games/${id}`);
    }

    createGame(request: GameRequest): Observable<Game> {
        return this.http.post<Game>(`${this.base}/games`, request);
    }

    updateGame(id: string, request: GameRequest): Observable<Game> {
        return this.http.put<Game>(`${this.base}/games/${id}`, request);
    }

    deleteGame(id: string): Observable<void> {
        return this.http.delete<void>(`${this.base}/games/${id}`);
    }

    // ─── Players ───

    getPlayers(): Observable<Player[]> {
        return this.http.get<Player[]>(`${this.base}/players`);
    }

    createPlayer(request: PlayerRequest): Observable<Player> {
        return this.http.post<Player>(`${this.base}/players`, request);
    }

    updatePlayer(id: string, request: PlayerRequest): Observable<Player> {
        return this.http.put<Player>(`${this.base}/players/${id}`, request);
    }

    deletePlayer(id: string): Observable<void> {
        return this.http.delete<void>(`${this.base}/players/${id}`);
    }

    // ─── Randomlockes ───

    /** Every randomlocke, or only those with a tournament in the given game. */
    getLockes(gameId?: string): Observable<LockeSummary[]> {
        const params: Record<string, string> = gameId ? { gameId } : {};

        return this.http.get<LockeSummary[]>(`${this.base}/lockes`, { params });
    }

    getLocke(id: string): Observable<LockeDetail> {
        return this.http.get<LockeDetail>(`${this.base}/lockes/${id}`);
    }

    createLocke(request: LockeRequest): Observable<LockeDetail> {
        return this.http.post<LockeDetail>(`${this.base}/lockes`, request);
    }

    updateLocke(id: string, request: LockeRequest): Observable<LockeDetail> {
        return this.http.put<LockeDetail>(`${this.base}/lockes/${id}`, request);
    }

    setLockeRoster(id: string, playerIds: string[]): Observable<LockeDetail> {
        return this.http.put<LockeDetail>(`${this.base}/lockes/${id}/players`, { playerIds });
    }

    deleteLocke(id: string): Observable<void> {
        return this.http.delete<void>(`${this.base}/lockes/${id}`);
    }

    // ─── Tournaments ───

    getTournament(id: string): Observable<TournamentDetail> {
        return this.http.get<TournamentDetail>(`${this.base}/tournaments/${id}`);
    }

    createTournament(request: TournamentCreateRequest): Observable<TournamentDetail> {
        return this.http.post<TournamentDetail>(`${this.base}/tournaments`, request);
    }

    updateTournament(id: string, request: TournamentUpdateRequest): Observable<TournamentDetail> {
        return this.http.put<TournamentDetail>(`${this.base}/tournaments/${id}`, request);
    }

    deleteTournament(id: string): Observable<void> {
        return this.http.delete<void>(`${this.base}/tournaments/${id}`);
    }

    addTournamentPlayer(tournamentId: string, playerId: string): Observable<TournamentDetail> {
        return this.http.post<TournamentDetail>(`${this.base}/tournaments/${tournamentId}/players`, { playerId });
    }

    updateTournamentPlayer(id: string, request: TournamentPlayerUpdateRequest): Observable<TournamentDetail> {
        return this.http.put<TournamentDetail>(`${this.base}/tournament-players/${id}`, request);
    }

    removeTournamentPlayer(id: string): Observable<TournamentDetail> {
        return this.http.delete<TournamentDetail>(`${this.base}/tournament-players/${id}`);
    }

    /** Generates the league or the groups (drawn again when randomGroups is true). */
    generateCalendar(tournamentId: string, randomGroups: boolean): Observable<TournamentDetail> {
        return this.http.post<TournamentDetail>(`${this.base}/tournaments/${tournamentId}/schedule`, { randomGroups });
    }

    generateBracket(tournamentId: string): Observable<TournamentDetail> {
        return this.http.post<TournamentDetail>(`${this.base}/tournaments/${tournamentId}/bracket`, {});
    }

    createMatch(tournamentId: string, request: MatchCreateRequest): Observable<TournamentDetail> {
        return this.http.post<TournamentDetail>(`${this.base}/tournaments/${tournamentId}/matches`, request);
    }

    /** Replaces every game of a series. */
    recordGames(matchId: string, games: MatchGameInput[]): Observable<TournamentDetail> {
        return this.http.put<TournamentDetail>(`${this.base}/matches/${matchId}/games`, { games });
    }

    deleteMatch(matchId: string): Observable<TournamentDetail> {
        return this.http.delete<TournamentDetail>(`${this.base}/matches/${matchId}`);
    }

    createReward(tournamentId: string, request: RewardRequest): Observable<TournamentDetail> {
        return this.http.post<TournamentDetail>(`${this.base}/tournaments/${tournamentId}/rewards`, request);
    }

    updateReward(id: string, request: RewardRequest): Observable<TournamentDetail> {
        return this.http.put<TournamentDetail>(`${this.base}/rewards/${id}`, request);
    }

    deleteReward(id: string): Observable<TournamentDetail> {
        return this.http.delete<TournamentDetail>(`${this.base}/rewards/${id}`);
    }

    // ─── Team pictures ───

    /** Uploads (or replaces) a team picture; compress it first with compressTeamImage. */
    uploadTeamImage(tournamentPlayerId: string, file: Blob): Observable<{ url: string }> {
        const body = new FormData();
        body.append('file', file, 'team.webp');

        return this.http.put<{ url: string }>(`${this.base}/tournament-players/${tournamentPlayerId}/team-image`, body);
    }

    deleteTeamImage(tournamentPlayerId: string): Observable<void> {
        return this.http.delete<void>(`${this.base}/tournament-players/${tournamentPlayerId}/team-image`);
    }

    // ─── Ranking ───

    getRanking(): Observable<PlayerStanding[]> {
        return this.http.get<PlayerStanding[]>(`${this.base}/ranking`);
    }
}
