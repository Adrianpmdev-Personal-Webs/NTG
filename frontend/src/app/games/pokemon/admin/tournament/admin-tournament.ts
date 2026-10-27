/**
 * AdminTournament
 *
 * Management of one tournament, in tabs:
 *   - Jugadores: add or remove players, set their group and points adjustment, upload any team picture.
 *   - Calendario: generate the league or the groups, record results, add or delete a series by hand.
 *   - Bracket: generate the double-elimination bracket from the groups and record its results.
 *   - Clasificación: final standings and group tables.
 *   - Recompensas: rewards and who picked each one (players pick in the order of the final standings).
 *   - Ajustes: name, game, status, points per win, reveal moment; delete the tournament.
 * Every action answers with the whole tournament, which replaces the copy shown here.
 *
 * @author @adrianpmdev
 */

import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Observable, forkJoin, switchMap } from 'rxjs';
import { NotificationService } from '@core/services/notification';
import { resolveApiError } from '@shared/utils/api-error';
import { PokemonApi } from '../../data/pokemon-api';
import { FORMAT_LABELS, STAGE_LABELS, STATUS_OPTIONS } from '../../data/pokemon-labels';
import {
    Game, GroupName, Match, MatchCreateRequest, MatchGameInput, MatchStage, Player, Reward, TournamentDetail,
    TournamentPlayer, TournamentUpdateRequest
} from '../../data/pokemon-models';
import { StandingRow, groupMatches, hasBracket, playersById, tournamentRows } from '../../data/tournament-view';
import { BracketView } from '../../ui/bracket-view/bracket-view';
import { CalendarView } from '../../ui/calendar-view/calendar-view';
import { SeriesEditor } from '../../ui/series-editor/series-editor';
import { StandingsTable } from '../../ui/standings-table/standings-table';
import { TeamGallery } from '../../ui/team-gallery/team-gallery';

type AdminTab = 'players' | 'calendar' | 'bracket' | 'standings' | 'rewards' | 'settings';

interface Confirmation {
    title: string;
    message: string;
    action: () => void;
}

@Component({
    changeDetection: ChangeDetectionStrategy.OnPush,
    selector: 'app-pokemon-admin-tournament',
    imports: [FormsModule, RouterLink, TeamGallery, CalendarView, BracketView, StandingsTable, SeriesEditor],
    templateUrl: './admin-tournament.html'
})
export class AdminTournament {

    private readonly api = inject(PokemonApi);
    private readonly notifications = inject(NotificationService);
    private readonly router = inject(Router);

    readonly formatLabels = FORMAT_LABELS;
    readonly statusOptions = STATUS_OPTIONS;
    readonly stageLabels = STAGE_LABELS;
    readonly groupNames: readonly GroupName[] = ['A', 'B'];
    readonly manualStages: readonly MatchStage[] = ['group', 'league', 'upper', 'lower', 'grand_final'];

    readonly loading = signal(true);
    readonly saving = signal(false);
    readonly detail = signal<TournamentDetail | null>(null);
    readonly allPlayers = signal<Player[]>([]);
    readonly games = signal<Game[]>([]);
    readonly activeTab = signal<AdminTab>('players');
    readonly editing = signal<Match | null>(null);
    readonly confirmation = signal<Confirmation | null>(null);

    readonly tabs = computed(() => {
        const tabs: { id: AdminTab; label: string }[] = [
            { id: 'players', label: 'JUGADORES' },
            { id: 'calendar', label: this.detail()?.format === 'round_robin' ? 'LIGA' : 'GRUPOS' }
        ];

        if (this.detail()?.format === 'groups_bracket') {
            tabs.push({ id: 'bracket', label: 'BRACKET' });
        }

        tabs.push({ id: 'standings', label: 'CLASIFICACIÓN' }, { id: 'rewards', label: 'RECOMPENSAS' }, { id: 'settings', label: 'AJUSTES' });

        return tabs;
    });

    readonly players = computed(() => (this.detail() ? playersById(this.detail()!) : new Map<string, TournamentPlayer>()));
    readonly standingRows = computed(() => (this.detail() ? tournamentRows(this.detail()!.standings, this.detail()!) : []));
    readonly leagueMatches = computed(() => this.detail()?.matches.filter(match => match.stage === 'league') ?? []);
    readonly showsBracket = computed(() => !!this.detail() && hasBracket(this.detail()!));
    readonly hasCalendar = computed(() => this.detail()?.matches.some(match => match.stage === 'group' || match.stage === 'league') ?? false);
    readonly ungrouped = computed(() => this.detail()?.players.filter(player => !player.group) ?? []);
    readonly addablePlayers = computed(() => {
        const inTournament = new Set(this.detail()?.players.map(player => player.playerId));

        return this.allPlayers().filter(player => !inTournament.has(player.id));
    });

    /** Players in the order they pick their rewards. */
    readonly pickOrder = computed(() => this.detail()?.standings.map(row => this.players().get(row.tournamentPlayerId)).filter(Boolean) as TournamentPlayer[] ?? []);

    playerToAdd: string | null = null;
    randomGroups = true;
    newReward = '';
    settings: TournamentUpdateRequest = { gameId: '', name: '', description: '', status: 'draft', pointsPerWin: 1, teamsRevealAt: null };
    revealAt = '';
    manual: MatchCreateRequest = { stage: 'league', round: 1, slot: 1, player1Id: '', player2Id: '' };

    constructor() {
        inject(ActivatedRoute).paramMap
            .pipe(switchMap(params => forkJoin({
                detail: this.api.getTournament(params.get('tournamentId') ?? ''),
                players: this.api.getPlayers(),
                games: this.api.getGames()
            })))
            .subscribe({
                next: ({ detail, players, games }) => {
                    this.allPlayers.set(players);
                    this.games.set(games);
                    this.setDetail(detail);
                    this.loading.set(false);
                },
                error: error => {
                    this.loading.set(false);
                    this.notifications.error(resolveApiError(error, 'No se ha podido cargar el torneo'));
                }
            });
    }

    // ─── Players ───

    addPlayer(): void {
        if (this.playerToAdd) {
            this.update(this.api.addTournamentPlayer(this.detail()!.id, this.playerToAdd), 'Jugador añadido');
            this.playerToAdd = null;
        }
    }

    setGroup(player: TournamentPlayer, group: GroupName | null): void {
        this.update(this.api.updateTournamentPlayer(player.id, { group, pointsAdjustment: player.pointsAdjustment }), 'Grupo actualizado');
    }

    setAdjustment(player: TournamentPlayer, value: string): void {
        const adjustment = Number(value);

        if (!Number.isInteger(adjustment) || adjustment === player.pointsAdjustment) {
            return;
        }

        this.update(this.api.updateTournamentPlayer(player.id, { group: player.group, pointsAdjustment: adjustment }), 'Ajuste guardado');
    }

    removePlayer(player: TournamentPlayer): void {
        this.confirm('QUITAR JUGADOR', `Se quita a ${player.name} del torneo junto con sus series.`,
            () => this.update(this.api.removeTournamentPlayer(player.id), 'Jugador quitado'));
    }

    reload(): void {
        this.api.getTournament(this.detail()!.id).subscribe(detail => this.setDetail(detail));
    }

    // ─── Calendar and bracket ───

    generateCalendar(): void {
        const run = () => this.update(this.api.generateCalendar(this.detail()!.id, this.randomGroups), 'Calendario generado');

        if (this.hasCalendar()) {
            this.confirm('REGENERAR CALENDARIO', 'Se borran las series actuales (y el bracket) y se crean de nuevo.', run);
        } else {
            run();
        }
    }

    generateBracket(): void {
        const run = () => this.update(this.api.generateBracket(this.detail()!.id), 'Bracket generado');

        if (this.showsBracket()) {
            this.confirm('REGENERAR BRACKET', 'Se borra el bracket actual y se genera de nuevo desde los grupos.', run);
        } else {
            run();
        }
    }

    groupSeries(group: GroupName): Match[] {
        return groupMatches(this.detail()!, group);
    }

    groupRows(group: GroupName): StandingRow[] {
        const detail = this.detail()!;

        return tournamentRows(detail.groups.find(current => current.group === group)?.standings ?? [], detail);
    }

    saveGames(games: MatchGameInput[]): void {
        const match = this.editing();

        if (match) {
            this.update(this.api.recordGames(match.id, games), 'Resultado guardado', () => this.editing.set(null));
        }
    }

    /** Series of a generated bracket are rebuilt by the bracket itself, so they cannot be deleted. */
    canRemove(match: Match): boolean {
        return !(this.detail()?.bracketSize && ['upper', 'lower', 'grand_final'].includes(match.stage));
    }

    removeMatch(): void {
        const match = this.editing();

        if (match) {
            this.update(this.api.deleteMatch(match.id), 'Serie eliminada', () => this.editing.set(null));
        }
    }

    createManualMatch(): void {
        this.update(this.api.createMatch(this.detail()!.id, this.manual), 'Serie añadida');
    }

    // ─── Rewards ───

    addReward(): void {
        if (this.newReward.trim()) {
            this.update(this.api.createReward(this.detail()!.id, { name: this.newReward.trim(), chosenBy: null }), 'Recompensa añadida');
            this.newReward = '';
        }
    }

    setRewardName(reward: Reward, name: string): void {
        if (name.trim() && name.trim() !== reward.name) {
            this.update(this.api.updateReward(reward.id, { name: name.trim(), chosenBy: reward.chosenBy }), 'Recompensa actualizada');
        }
    }

    setRewardOwner(reward: Reward, chosenBy: string | null): void {
        this.update(this.api.updateReward(reward.id, { name: reward.name, chosenBy }), chosenBy ? 'Elección registrada' : 'Elección borrada');
    }

    removeReward(reward: Reward): void {
        this.update(this.api.deleteReward(reward.id), 'Recompensa eliminada');
    }

    /** Players who have not picked yet, plus the current owner of this reward. */
    pickers(reward: Reward): TournamentPlayer[] {
        const taken = new Set(this.detail()?.rewards.map(current => current.chosenBy).filter(id => id && id !== reward.chosenBy));

        return this.pickOrder().filter(player => !taken.has(player.id));
    }

    positionOf(playerId: string): number {
        return (this.detail()?.standings.findIndex(row => row.tournamentPlayerId === playerId) ?? -1) + 1;
    }

    // ─── Settings ───

    saveSettings(): void {
        const request: TournamentUpdateRequest = {
            ...this.settings,
            description: this.settings.description?.trim() || null,
            teamsRevealAt: this.revealAt ? new Date(this.revealAt).toISOString() : null
        };

        this.update(this.api.updateTournament(this.detail()!.id, request), 'Torneo actualizado');
    }

    deleteTournament(): void {
        const detail = this.detail()!;

        this.confirm('ELIMINAR TORNEO', `Se elimina ${detail.name} con sus series, recompensas y equipos subidos.`, () => {
            this.saving.set(true);
            this.api.deleteTournament(detail.id).subscribe({
                next: () => {
                    this.saving.set(false);
                    this.notifications.success('Torneo eliminado');
                    this.router.navigate(['/admin/pokemon/lockes', detail.locke.id]);
                },
                error: error => {
                    this.saving.set(false);
                    this.notifications.error(resolveApiError(error, 'No se ha podido eliminar el torneo'));
                }
            });
        });
    }

    // ─── Confirmation ───

    confirm(title: string, message: string, action: () => void): void {
        this.confirmation.set({ title, message, action });
    }

    runConfirmed(): void {
        const confirmation = this.confirmation();
        this.confirmation.set(null);
        confirmation?.action();
    }

    // ─── Helpers ───

    private update(call: Observable<TournamentDetail>, message: string, done?: () => void): void {
        this.saving.set(true);
        call.subscribe({
            next: detail => {
                this.saving.set(false);
                this.setDetail(detail);
                this.notifications.success(message);
                done?.();
            },
            error: error => {
                this.saving.set(false);
                this.notifications.error(resolveApiError(error, 'No se ha podido guardar'));
            }
        });
    }

    private setDetail(detail: TournamentDetail): void {
        this.detail.set(detail);
        this.settings = {
            gameId: detail.game.id,
            name: detail.name,
            description: detail.description ?? '',
            status: detail.status,
            pointsPerWin: detail.pointsPerWin,
            teamsRevealAt: detail.teamsRevealAt
        };
        this.revealAt = toLocalInput(detail.teamsRevealAt);
        this.manual = { ...this.manual, stage: detail.format === 'round_robin' ? 'league' : 'group' };
    }
}

/** ISO instant to the value of a datetime-local input (local time, minutes). */
function toLocalInput(iso: string | null): string {
    if (!iso) {
        return '';
    }

    const date = new Date(iso);
    const local = new Date(date.getTime() - date.getTimezoneOffset() * 60_000);

    return local.toISOString().slice(0, 16);
}
