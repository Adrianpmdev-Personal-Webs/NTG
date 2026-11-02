/**
 * LolHistory
 *
 * The games of each player of the group, by queue, with their official rank and the statistics of the season. What is
 * stored shows at once and the backend looks for new games when a player is opened, or when the update button is
 * pressed; moving between queues costs no call. A selector shows past seasons, with the current one by default and the
 * rank each player finished with. Hextech look: navy, gold and teal, filling the whole screen.
 *
 * @author @adrianpmdev
 */

import { DecimalPipe, NgTemplateOutlet } from '@angular/common';
import { ChangeDetectionStrategy, Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Subscription } from 'rxjs';
import { NotificationCenter } from '@core/notifications/notification-center';
import { resolveApiError } from '@shared/utils/api-error';
import { LolApi } from '../data/lol-api';
import { LolMatch, LolPlayerDetail, LolPlayers } from '../data/lol-models';

const MINUTE = 60_000;
const HOUR = 60 * MINUTE;
const DAY = 24 * HOUR;

@Component({
    selector: 'app-lol-history',
    imports: [DecimalPipe, NgTemplateOutlet, RouterLink],
    templateUrl: './lol-history.html',
    styleUrl: './lol-history.scss',
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LolHistory implements OnInit {
    private readonly api = inject(LolApi);
    private readonly notifications = inject(NotificationCenter);

    private detailLoad: Subscription | null = null;
    private refreshLoad: Subscription | null = null;

    protected readonly loading = signal(true);
    protected readonly failed = signal(false);
    protected readonly saving = signal(false);
    protected readonly updating = signal(false);
    protected readonly confirmUnlink = signal(false);
    protected readonly data = signal<LolPlayers | null>(null);
    protected readonly selectedId = signal<string | null>(null);
    protected readonly seasonKey = signal<string | null>(null);
    protected readonly detail = signal<LolPlayerDetail | null>(null);
    protected readonly queueId = signal(420);

    protected readonly players = computed(() => this.data()?.players ?? []);
    protected readonly queue = computed(() => {
        const queues = this.detail()?.queues ?? [];

        return queues.find((candidate) => candidate.id === this.queueId()) ?? queues[0] ?? null;
    });

    constructor() {
        inject(DestroyRef).onDestroy(() => {
            this.detailLoad?.unsubscribe();
            this.refreshLoad?.unsubscribe();
        });
    }

    ngOnInit(): void {
        this.api.getPlayers().subscribe({
            next: (data) => {
                this.loading.set(false);
                this.applyPlayers(data);
            },
            error: () => {
                this.failed.set(true);
                this.loading.set(false);
            },
        });
    }

    protected select(userId: string): void {
        this.selectedId.set(userId);
        this.seasonKey.set(null);
        this.detail.set(null);
        this.loadDetail(userId);
        this.update();
    }

    protected selectSeason(key: string): void {
        const userId = this.selectedId();

        this.seasonKey.set(key);

        if (userId) {
            this.loadDetail(userId);
        }
    }

    protected update(): void {
        const userId = this.selectedId();

        if (!userId) {
            return;
        }

        const season = this.seasonKey();

        this.updating.set(true);
        this.refreshLoad?.unsubscribe();
        this.refreshLoad = this.api.refresh(userId, season).subscribe({
            next: (detail) => {
                if (this.seasonKey() === season) {
                    this.detailLoad?.unsubscribe();
                    this.detail.set(detail);
                } else {
                    this.loadDetail(userId);
                }

                this.updating.set(false);
                this.data.update(
                    (data) =>
                        data && {
                            ...data,
                            players: data.players.map((player) =>
                                player.userId === detail.player.userId ? detail.player : player,
                            ),
                        },
                );

                if (detail.error) {
                    this.notifications.warning(detail.error);
                }
            },
            error: (error) => {
                this.updating.set(false);
                this.notifications.error(resolveApiError(error, 'No se ha podido actualizar'));
            },
        });
    }

    protected link(event: Event, field: HTMLInputElement): void {
        event.preventDefault();

        const riotId = field.value.trim();

        if (!riotId || this.saving()) {
            return;
        }

        this.saving.set(true);
        this.api.linkOwn(riotId).subscribe({
            next: (data) => {
                this.saving.set(false);
                field.value = '';
                this.applyPlayers(data, data.myUserId);
                this.notifications.success('Cuenta vinculada');
            },
            error: (error) => {
                this.saving.set(false);
                this.notifications.error(resolveApiError(error, 'No se ha podido vincular la cuenta'));
            },
        });
    }

    protected unlink(): void {
        if (this.saving()) {
            return;
        }

        this.saving.set(true);
        this.api.unlinkOwn().subscribe({
            next: (data) => {
                this.saving.set(false);
                this.confirmUnlink.set(false);
                this.applyPlayers(data);
            },
            error: (error) => {
                this.saving.set(false);
                this.notifications.error(resolveApiError(error, 'No se ha podido desvincular la cuenta'));
            },
        });
    }

    protected playerName(player: { username: string | null; riotId: string }): string {
        return player.username ?? player.riotId;
    }

    protected ago(moment: string): string {
        const elapsed = Date.now() - new Date(moment).getTime();

        if (elapsed < HOUR) {
            return `hace ${Math.max(1, Math.floor(elapsed / MINUTE))} min`;
        }

        if (elapsed < DAY) {
            return `hace ${Math.floor(elapsed / HOUR)} h`;
        }

        return `hace ${Math.floor(elapsed / DAY)} d`;
    }

    protected duration(seconds: number): string {
        return `${Math.floor(seconds / 60)} min`;
    }

    protected csPerMinute(match: LolMatch): number {
        return match.durationSeconds > 0 ? (match.cs * 60) / match.durationSeconds : 0;
    }

    protected matchKda(match: LolMatch): number {
        return (match.kills + match.assists) / Math.max(1, match.deaths);
    }

    protected hideIcon(event: Event): void {
        (event.target as HTMLElement).style.visibility = 'hidden';
    }

    private loadDetail(userId: string): void {
        this.detailLoad?.unsubscribe();
        this.detailLoad = this.api.getPlayer(userId, this.seasonKey()).subscribe({
            next: (detail) => this.detail.set(detail),
            error: (error) => this.notifications.error(resolveApiError(error, 'No se ha podido cargar el jugador')),
        });
    }

    private applyPlayers(data: LolPlayers, preferred?: string): void {
        this.data.set(data);

        const ids = data.players.map((player) => player.userId);
        const current = this.selectedId();
        const next =
            preferred && ids.includes(preferred)
                ? preferred
                : current && ids.includes(current)
                  ? current
                  : ids.includes(data.myUserId)
                    ? data.myUserId
                    : (ids[0] ?? null);

        if (next && (next !== current || preferred)) {
            this.select(next);
        } else if (!next) {
            this.selectedId.set(null);
            this.detail.set(null);
        }
    }
}
