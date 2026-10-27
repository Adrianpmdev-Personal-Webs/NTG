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
import { API_BASE_URL } from '@core/config/api';
import { TypeChart } from '../type-calculator/type-calculator-constants';
import {
    Game, GameRequest, LockeDetail, LockeRequest, LockeSummary, MatchCreateRequest, MatchGameInput, Player,
    PlayerRequest, PlayerStanding, RewardRequest, TournamentCreateRequest, TournamentDetail,
    TournamentPlayerUpdateRequest, TournamentUpdateRequest
} from './pokemon-models';

/** How long a read is answered from memory. */
const READ_TTL_MS = 60_000;

@Injectable({ providedIn: 'root' })
export class PokemonApi {

    private readonly http = inject(HttpClient);
    private readonly base = `${API_BASE_URL}/pokemon`;

    private typeChart$: Observable<TypeChart> | null = null;

    /** Recent reads by URL, kept {@link READ_TTL_MS} ms. */
    private readonly reads = new Map<string, { expiresAt: number; request: Observable<unknown> }>();

    // ─── Games ───

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

    // ─── Players ───

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

    // ─── Randomlockes ───

    /** Every randomlocke, or only those with a tournament in the given game. */
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

    // ─── Tournaments ───

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
        return this.write(this.http.post<TournamentDetail>(`${this.base}/tournaments/${tournamentId}/players`, { playerId }));
    }

    updateTournamentPlayer(id: string, request: TournamentPlayerUpdateRequest): Observable<TournamentDetail> {
        return this.write(this.http.put<TournamentDetail>(`${this.base}/tournament-players/${id}`, request));
    }

    removeTournamentPlayer(id: string): Observable<TournamentDetail> {
        return this.write(this.http.delete<TournamentDetail>(`${this.base}/tournament-players/${id}`));
    }

    /** Generates the league or the groups (drawn again when randomGroups is true). */
    generateCalendar(tournamentId: string, randomGroups: boolean): Observable<TournamentDetail> {
        return this.write(this.http.post<TournamentDetail>(`${this.base}/tournaments/${tournamentId}/schedule`, { randomGroups }));
    }

    generateBracket(tournamentId: string): Observable<TournamentDetail> {
        return this.write(this.http.post<TournamentDetail>(`${this.base}/tournaments/${tournamentId}/bracket`, {}));
    }

    createMatch(tournamentId: string, request: MatchCreateRequest): Observable<TournamentDetail> {
        return this.write(this.http.post<TournamentDetail>(`${this.base}/tournaments/${tournamentId}/matches`, request));
    }

    /** Replaces every game of a series. */
    recordGames(matchId: string, games: MatchGameInput[]): Observable<TournamentDetail> {
        return this.write(this.http.put<TournamentDetail>(`${this.base}/matches/${matchId}/games`, { games }));
    }

    deleteMatch(matchId: string): Observable<TournamentDetail> {
        return this.write(this.http.delete<TournamentDetail>(`${this.base}/matches/${matchId}`));
    }

    createReward(tournamentId: string, request: RewardRequest): Observable<TournamentDetail> {
        return this.write(this.http.post<TournamentDetail>(`${this.base}/tournaments/${tournamentId}/rewards`, request));
    }

    updateReward(id: string, request: RewardRequest): Observable<TournamentDetail> {
        return this.write(this.http.put<TournamentDetail>(`${this.base}/rewards/${id}`, request));
    }

    deleteReward(id: string): Observable<TournamentDetail> {
        return this.write(this.http.delete<TournamentDetail>(`${this.base}/rewards/${id}`));
    }

    // ─── Team pictures ───

    /** Uploads (or replaces) a team picture. The original is sent: the backend compresses it. */
    uploadTeamImage(tournamentPlayerId: string, file: File): Observable<{ url: string }> {
        const body = new FormData();
        body.append('file', file, file.name);

        return this.write(this.http.put<{ url: string }>(`${this.base}/tournament-players/${tournamentPlayerId}/team-image`, body));
    }

    deleteTeamImage(tournamentPlayerId: string): Observable<void> {
        return this.write(this.http.delete<void>(`${this.base}/tournament-players/${tournamentPlayerId}/team-image`));
    }

    // ─── Ranking ───

    getRanking(): Observable<PlayerStanding[]> {
        return this.cachedGet<PlayerStanding[]>(`${this.base}/ranking`);
    }

    // ─── Types ───

    /** Types and damage chart of the type calculator. Downloaded once per session; a failed request is retried later. */
    getTypeChart(): Observable<TypeChart> {
        if (!this.typeChart$) {
            this.typeChart$ = this.http.get<TypeChart>(`${this.base}/types`).pipe(
                catchError(error => {
                    this.typeChart$ = null;
                    return throwError(() => error);
                }),
                shareReplay({ bufferSize: 1, refCount: false })
            );
        }

        return this.typeChart$;
    }

    // ─── Read memory ───

    /** A GET answered from memory while it is recent. A failed request is forgotten so the next call asks again. */
    private cachedGet<T>(url: string, params: Record<string, string> = {}): Observable<T> {
        const key = `${url}?${new URLSearchParams(params)}`;
        const known = this.reads.get(key);

        if (known && known.expiresAt > Date.now()) {
            return known.request as Observable<T>;
        }

        const request = this.http.get<T>(url, { params }).pipe(
            catchError(error => {
                this.reads.delete(key);
                return throwError(() => error);
            }),
            shareReplay({ bufferSize: 1, refCount: false })
        );

        this.reads.set(key, { expiresAt: Date.now() + READ_TTL_MS, request });

        return request;
    }

    /** Any change may affect any screen, so it empties the read memory once the server has applied it. */
    private write<T>(request: Observable<T>): Observable<T> {
        return request.pipe(tap(() => this.reads.clear()));
    }
}
