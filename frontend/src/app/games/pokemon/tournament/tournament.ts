/**
 * TournamentPage
 *
 * One tournament for players: teams, groups or league, bracket, final standings and rewards. Everything
 * comes in one response; the page reloads it after the player changes their team picture.
 *
 * @author @adrianpmdev
 */

import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { Subject, merge, map, switchMap } from 'rxjs';
import { PokemonApi } from '../data/pokemon-api';
import { FORMAT_LABELS, STATUS_LABELS } from '../data/pokemon-labels';
import { GroupName, Match, TournamentDetail, TournamentPlayer } from '../data/pokemon-models';
import { StandingRow, formatDateTime, groupMatches, hasBracket, playersById, tournamentRows } from '../data/tournament-view';
import { BracketView } from '../ui/bracket-view/bracket-view';
import { CalendarView } from '../ui/calendar-view/calendar-view';
import { StandingsTable } from '../ui/standings-table/standings-table';
import { TeamGallery } from '../ui/team-gallery/team-gallery';

type TournamentTab = 'teams' | 'calendar' | 'bracket' | 'standings' | 'rewards';

@Component({
    changeDetection: ChangeDetectionStrategy.OnPush,
    selector: 'app-pokemon-tournament',
    imports: [RouterLink, TeamGallery, CalendarView, BracketView, StandingsTable],
    templateUrl: './tournament.html',
    styleUrl: './tournament.scss'
})
export class TournamentPage {

    private readonly api = inject(PokemonApi);
    private readonly route = inject(ActivatedRoute);
    private readonly reload$ = new Subject<void>();

    readonly formatLabels = FORMAT_LABELS;
    readonly statusLabels = STATUS_LABELS;
    readonly groupNames: readonly GroupName[] = ['A', 'B'];

    readonly loading = signal(true);
    readonly failed = signal(false);
    readonly detail = signal<TournamentDetail | null>(null);
    readonly activeTab = signal<TournamentTab>('teams');

    readonly tabs = computed(() => {
        const detail = this.detail();
        const tabs: { id: TournamentTab; label: string }[] = [{ id: 'teams', label: 'Equipos' }];

        if (detail) {
            tabs.push({ id: 'calendar', label: detail.format === 'round_robin' ? 'Liga' : 'Grupos' });

            if (detail.format === 'groups_bracket') {
                tabs.push({ id: 'bracket', label: 'Bracket' });
            }
        }

        tabs.push({ id: 'standings', label: 'Clasificación' }, { id: 'rewards', label: 'Recompensas' });

        return tabs;
    });

    readonly players = computed(() => (this.detail() ? playersById(this.detail()!) : new Map<string, TournamentPlayer>()));
    readonly standingRows = computed(() => (this.detail() ? tournamentRows(this.detail()!.standings, this.detail()!) : []));
    readonly leagueMatches = computed(() => this.detail()?.matches.filter(match => match.stage === 'league') ?? []);
    readonly showsBracket = computed(() => !!this.detail() && hasBracket(this.detail()!));

    constructor() {
        const id$ = this.route.paramMap.pipe(map(params => params.get('tournamentId') ?? ''));

        merge(id$, this.reload$.pipe(map(() => this.detail()?.id ?? '')))
            .pipe(switchMap(id => this.api.getTournament(id)))
            .subscribe({
                next: detail => {
                    this.detail.set(detail);
                    this.failed.set(false);
                    this.loading.set(false);
                },
                error: () => {
                    this.failed.set(true);
                    this.loading.set(false);
                }
            });
    }

    reload(): void {
        this.reload$.next();
    }

    groupRows(group: GroupName): StandingRow[] {
        const detail = this.detail()!;
        const standings = detail.groups.find(current => current.group === group)?.standings ?? [];

        return tournamentRows(standings, detail);
    }

    groupSeries(group: GroupName): Match[] {
        return groupMatches(this.detail()!, group);
    }

    playerName(tournamentPlayerId: string | null): string {
        return tournamentPlayerId ? this.players().get(tournamentPlayerId)?.name ?? '—' : '—';
    }

    revealText(): string {
        return formatDateTime(this.detail()?.teamsRevealAt ?? null);
    }
}
