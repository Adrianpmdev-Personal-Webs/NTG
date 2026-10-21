/**
 * Tournaments
 *
 * Read-only view of the tournaments of a locke. Lets players pick a tournament and browse its
 * participants, groups, bracket, standings and rewards.
 *
 * @author @adrianpmdev
 */

import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { catchError, forkJoin, of, switchMap } from 'rxjs';
import { UserService } from '@core/services/user';
import { PokemonApi } from '../pokemon-api';
import {
    BracketMatchResponse, BracketStageResponse, GroupMatchResponse, GroupParticipantResponse,
    LockeResponse, RewardResponse, TournamentGroupResponse, TournamentParticipantResponse,
    TournamentResponse, TournamentStandingResponse
} from '../pokemon-models';

type TournamentTab = 'participants' | 'groups' | 'bracket' | 'standings' | 'rewards';

interface GroupView {
    group: TournamentGroupResponse;
    participants: GroupParticipantResponse[];
    matches: GroupMatchResponse[];
}

interface StageView {
    stage: BracketStageResponse;
    matches: BracketMatchResponse[];
}

@Component({
    changeDetection: ChangeDetectionStrategy.OnPush,
    selector: 'app-pokemon-tournaments',
    imports: [RouterLink],
    templateUrl: './tournaments.html',
    styleUrl: './tournaments.scss'
})
export class Tournaments {

    private readonly pokemonApi = inject(PokemonApi);
    private readonly userService = inject(UserService);
    private readonly lockeId = inject(ActivatedRoute).snapshot.paramMap.get('lockeId') ?? '';

    readonly tabs: readonly { id: TournamentTab; label: string }[] = [
        { id: 'participants', label: 'Participantes' },
        { id: 'groups', label: 'Grupos' },
        { id: 'bracket', label: 'Bracket' },
        { id: 'standings', label: 'Clasificación' },
        { id: 'rewards', label: 'Premios' }
    ];

    readonly loading = signal(true);
    readonly failed = signal(false);
    readonly locke = signal<LockeResponse | null>(null);
    readonly tournaments = signal<TournamentResponse[]>([]);
    readonly selected = signal<TournamentResponse | null>(null);
    readonly activeTab = signal<TournamentTab>('participants');

    readonly detailLoading = signal(false);
    readonly participants = signal<TournamentParticipantResponse[]>([]);
    readonly groups = signal<GroupView[]>([]);
    readonly stages = signal<StageView[]>([]);
    readonly standings = signal<TournamentStandingResponse[]>([]);
    readonly rewards = signal<RewardResponse[]>([]);

    private readonly usernames = signal<Map<string, string>>(new Map());

    constructor() {
        this.userService.getAllUsers().pipe(
            catchError(() => of([]))
        ).subscribe(users => this.usernames.set(new Map(users.map(user => [user.id, user.username]))));

        forkJoin({
            locke: this.pokemonApi.getLockeById(this.lockeId),
            tournaments: this.pokemonApi.getTournamentsByLockeId(this.lockeId)
        }).subscribe({
            next: ({ locke, tournaments }) => {
                this.locke.set(locke);
                this.tournaments.set(tournaments);
                this.loading.set(false);

                if (tournaments.length > 0) {
                    this.select(tournaments[0]);
                }
            },
            error: () => {
                this.failed.set(true);
                this.loading.set(false);
            }
        });
    }

    select(tournament: TournamentResponse): void {
        this.selected.set(tournament);
        this.activeTab.set('participants');
        this.loadDetail(tournament.id);
    }

    setTab(tab: TournamentTab): void {
        this.activeTab.set(tab);
    }

    userName(userId: string): string {
        return this.usernames().get(userId) ?? `${userId.slice(0, 8)}...`;
    }

    participantLabel(participantId: string | undefined): string {
        if (!participantId) {
            return 'TBD';
        }

        const participant = this.participants().find(current => current.id === participantId);

        return participant ? this.userName(participant.userId) : `${participantId.slice(0, 8)}...`;
    }

    statusLabel(status: string): string {
        return status.replace(/_/g, ' ').toUpperCase();
    }

    formatDate(date?: string): string {
        return date ? new Date(date).toLocaleDateString('es-ES') : '—';
    }

    sortedGroupParticipants(view: GroupView): GroupParticipantResponse[] {
        return [...view.participants].sort((a, b) => b.points - a.points);
    }

    sortedStandings(): TournamentStandingResponse[] {
        return [...this.standings()].sort((a, b) =>
            (a.finalPosition ?? Number.MAX_SAFE_INTEGER) - (b.finalPosition ?? Number.MAX_SAFE_INTEGER));
    }

    sortedStages(): StageView[] {
        return [...this.stages()].sort((a, b) => a.stage.stageOrder - b.stage.stageOrder);
    }

    private loadDetail(tournamentId: string): void {
        this.detailLoading.set(true);

        forkJoin({
            participants: this.pokemonApi.getParticipantsByTournament(tournamentId),
            groups: this.pokemonApi.getGroupsByTournament(tournamentId).pipe(
                switchMap(groups => groups.length === 0 ? of([] as GroupView[]) : forkJoin(
                    groups.map(group => forkJoin({
                        group: of(group),
                        participants: this.pokemonApi.getGroupParticipants(group.id),
                        matches: this.pokemonApi.getGroupMatches(group.id)
                    }))
                ))
            ),
            stages: this.pokemonApi.getBracketStagesByTournament(tournamentId).pipe(
                switchMap(stages => stages.length === 0 ? of([] as StageView[]) : forkJoin(
                    stages.map(stage => forkJoin({
                        stage: of(stage),
                        matches: this.pokemonApi.getBracketMatchesByStage(stage.id)
                    }))
                ))
            ),
            standings: this.pokemonApi.getStandingsByTournament(tournamentId),
            rewards: this.pokemonApi.getRewardsByTournament(tournamentId)
        }).subscribe({
            next: detail => {
                this.participants.set(detail.participants);
                this.groups.set(detail.groups);
                this.stages.set(detail.stages);
                this.standings.set(detail.standings);
                this.rewards.set([...detail.rewards].sort((a, b) => a.position - b.position));
                this.detailLoading.set(false);
            },
            error: () => this.detailLoading.set(false)
        });
    }
}
