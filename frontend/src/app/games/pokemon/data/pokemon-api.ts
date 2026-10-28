/**
 * PokemonApi
 *
 * HTTP service of the Pokémon module: one method per endpoint. Every admin action on a tournament
 * answers with the whole updated tournament, so screens just replace their copy with the response.
 * Reads are kept for a minute, so moving between screens does not ask the server again; any write empties
 * that memory, so a change made from this browser always shows up right away.
 *
 * @author @adrianpmdev
 */

import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, catchError, shareReplay, tap, throwError } from 'rxjs';
import { API_BASE_URL } from '@core/api/api';
import { TypeChart } from '../type-calculator/type-calculator-constants';
import {
    Game,
    GameRequest,
    LockeDetail,
    LockeRequest,
    LockeSummary,
    MatchCreateRequest,
    MatchGameInput,
    Player,
    PlayerRequest,
    PlayerStanding,
    RewardRequest,
    TournamentCreateRequest,
    TournamentDetail,
    TournamentPlayerUpdateRequest,
    TournamentUpdateRequest,
} from './pokemon-models';

const READ_TTL_MS = 60_000;

@Injectable({ providedIn: 'root' })
export class PokemonApi {
    private readonly http = inject(HttpClient);
    private readonly base = `${API_BASE_URL}/pokemon`;

    private typeChart$: Observable<TypeChart> | null = null;

    private readonly reads = new Map<string, { expiresAt: number; request: Observable<unknown> }>();

    getGames(): Observable<Game[]> {
        return this.cachedGet<Game[]>(`${this.base}/games`);
    }

    getGame(id: string): Observable<Game> {
        return this.cachedGet<Game>(`${this.base}/games/${id}`);
    }

    createGame(request: GameRequest): Observable<Game> {
        return this.write(this.http.post<Game>(`${this.base}/games`, request));
    }

    updateGame(id: string, request: GameRequest): Observable<Game> {
        return this.write(this.http.put<Game>(`${this.base}/games/${id}`, request));
    }

    deleteGame(id: string): Observable<void> {
        return this.write(this.http.delete<void>(`${this.base}/games/${id}`));
    }

    getPlayers(): Observable<Player[]> {
        return this.cachedGet<Player[]>(`${this.base}/players`);
    }

    createPlayer(request: PlayerRequest): Observable<Player> {
        return this.write(this.http.post<Player>(`${this.base}/players`, request));
    }

    updatePlayer(id: string, request: PlayerRequest): Observable<Player> {
        return this.write(this.http.put<Player>(`${this.base}/players/${id}`, request));
    }

    deletePlayer(id: string): Observable<void> {
        return this.write(this.http.delete<void>(`${this.base}/players/${id}`));
    }

    getLockes(gameId?: string): Observable<LockeSummary[]> {
        const params: Record<string, string> = gameId ? { gameId } : {};

        return this.cachedGet<LockeSummary[]>(`${this.base}/lockes`, params);
    }

    getLocke(id: string): Observable<LockeDetail> {
        return this.cachedGet<LockeDetail>(`${this.base}/lockes/${id}`);
    }

    createLocke(request: LockeRequest): Observable<LockeDetail> {
        return this.write(this.http.post<LockeDetail>(`${this.base}/lockes`, request));
    }

    updateLocke(id: string, request: LockeRequest): Observable<LockeDetail> {
        return this.write(this.http.put<LockeDetail>(`${this.base}/lockes/${id}`, request));
    }

    setLockeRoster(id: string, playerIds: string[]): Observable<LockeDetail> {
        return this.write(this.http.put<LockeDetail>(`${this.base}/lockes/${id}/players`, { playerIds }));
    }

    deleteLocke(id: string): Observable<void> {
        return this.write(this.http.delete<void>(`${this.base}/lockes/${id}`));
    }

    getTournament(id: string): Observable<TournamentDetail> {
        return this.cachedGet<TournamentDetail>(`${this.base}/tournaments/${id}`);
    }

    createTournament(request: TournamentCreateRequest): Observable<TournamentDetail> {
        return this.write(this.http.post<TournamentDetail>(`${this.base}/tournaments`, request));
    }

    updateTournament(id: string, request: TournamentUpdateRequest): Observable<TournamentDetail> {
        return this.write(this.http.put<TournamentDetail>(`${this.base}/tournaments/${id}`, request));
    }

    deleteTournament(id: string): Observable<void> {
        return this.write(this.http.delete<void>(`${this.base}/tournaments/${id}`));
    }

    addTournamentPlayer(tournamentId: string, playerId: string): Observable<TournamentDetail> {
        return this.write(
            this.http.post<TournamentDetail>(`${this.base}/tournaments/${tournamentId}/players`, { playerId }),
        );
    }

    updateTournamentPlayer(id: string, request: TournamentPlayerUpdateRequest): Observable<TournamentDetail> {
        return this.write(this.http.put<TournamentDetail>(`${this.base}/tournament-players/${id}`, request));
    }

    removeTournamentPlayer(id: string): Observable<TournamentDetail> {
        return this.write(this.http.delete<TournamentDetail>(`${this.base}/tournament-players/${id}`));
    }

    generateCalendar(tournamentId: string, randomGroups: boolean): Observable<TournamentDetail> {
        return this.write(
            this.http.post<TournamentDetail>(`${this.base}/tournaments/${tournamentId}/schedule`, { randomGroups }),
        );
    }

    generateBracket(tournamentId: string): Observable<TournamentDetail> {
        return this.write(this.http.post<TournamentDetail>(`${this.base}/tournaments/${tournamentId}/bracket`, {}));
    }

    createMatch(tournamentId: string, request: MatchCreateRequest): Observable<TournamentDetail> {
        return this.write(
            this.http.post<TournamentDetail>(`${this.base}/tournaments/${tournamentId}/matches`, request),
        );
    }

    recordGames(matchId: string, games: MatchGameInput[]): Observable<TournamentDetail> {
        return this.write(this.http.put<TournamentDetail>(`${this.base}/matches/${matchId}/games`, { games }));
    }

    deleteMatch(matchId: string): Observable<TournamentDetail> {
        return this.write(this.http.delete<TournamentDetail>(`${this.base}/matches/${matchId}`));
    }

    createReward(tournamentId: string, request: RewardRequest): Observable<TournamentDetail> {
        return this.write(
            this.http.post<TournamentDetail>(`${this.base}/tournaments/${tournamentId}/rewards`, request),
        );
    }

    updateReward(id: string, request: RewardRequest): Observable<TournamentDetail> {
        return this.write(this.http.put<TournamentDetail>(`${this.base}/rewards/${id}`, request));
    }

    deleteReward(id: string): Observable<TournamentDetail> {
        return this.write(this.http.delete<TournamentDetail>(`${this.base}/rewards/${id}`));
    }

    uploadTeamImage(tournamentPlayerId: string, file: File): Observable<{ url: string }> {
        const body = new FormData();
        body.append('file', file, file.name);

        return this.write(
            this.http.put<{ url: string }>(`${this.base}/tournament-players/${tournamentPlayerId}/team-image`, body),
        );
    }

    deleteTeamImage(tournamentPlayerId: string): Observable<void> {
        return this.write(this.http.delete<void>(`${this.base}/tournament-players/${tournamentPlayerId}/team-image`));
    }

    getRanking(): Observable<PlayerStanding[]> {
        return this.cachedGet<PlayerStanding[]>(`${this.base}/ranking`);
    }

    getTypeChart(): Observable<TypeChart> {
        if (!this.typeChart$) {
            this.typeChart$ = this.http.get<TypeChart>(`${this.base}/types`).pipe(
                catchError((error) => {
                    this.typeChart$ = null;
                    return throwError(() => error);
                }),
                shareReplay({ bufferSize: 1, refCount: false }),
            );
        }

        return this.typeChart$;
    }

    private cachedGet<T>(url: string, params: Record<string, string> = {}): Observable<T> {
        const key = `${url}?${new URLSearchParams(params)}`;
        const known = this.reads.get(key);

        if (known && known.expiresAt > Date.now()) {
            return known.request as Observable<T>;
        }

        const request = this.http.get<T>(url, { params }).pipe(
            catchError((error) => {
                this.reads.delete(key);
                return throwError(() => error);
            }),
            shareReplay({ bufferSize: 1, refCount: false }),
        );

        this.reads.set(key, { expiresAt: Date.now() + READ_TTL_MS, request });

        return request;
    }

    private write<T>(request: Observable<T>): Observable<T> {
        return request.pipe(tap(() => this.reads.clear()));
    }
}
