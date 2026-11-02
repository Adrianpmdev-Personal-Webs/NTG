/**
 * AdminAccounts
 *
 * Accounts of League of Legends: an admin links the Riot ID of each player with the tag and starts the download of their
 * games. Downloads run in the backend one after another, so this screen can be closed. While one is active and this
 * screen is open, it reads the state every few seconds, which costs no call to Riot; it stops by itself when they end.
 * It also manages the seasons: which ones exist and the day each one starts.
 *
 * @author @adrianpmdev
 */

import { ChangeDetectionStrategy, Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { Observable } from 'rxjs';
import { NotificationCenter } from '@core/notifications/notification-center';
import { ModalOverlay } from '@shared/directives/modal-overlay';
import { resolveApiError } from '@shared/utils/api-error';
import { LolApi } from '../data/lol-api';
import { LolAccountState, LolAdminAccount, LolAdminSeason } from '../data/lol-models';

const STATE_LABELS: Readonly<Record<LolAccountState, string>> = {
    NOT_LINKED: 'SIN VINCULAR',
    PENDING: 'PENDIENTE',
    QUEUED: 'EN COLA',
    PREPARING: 'PREPARANDO',
    LOADING: 'DESCARGANDO',
    COMPLETE: 'COMPLETO',
    ERROR: 'ERROR',
    INTERRUPTED: 'INTERRUMPIDA',
};

const ACTIVE_STATES: readonly LolAccountState[] = ['QUEUED', 'PREPARING', 'LOADING'];
const CHECK_EVERY_MS = 10_000;

@Component({
    changeDetection: ChangeDetectionStrategy.OnPush,
    selector: 'app-lol-admin-accounts',
    imports: [ModalOverlay],
    templateUrl: './admin-accounts.html',
    styleUrl: './admin-accounts.scss',
})
export class AdminAccounts implements OnInit {
    private readonly api = inject(LolApi);
    private readonly notifications = inject(NotificationCenter);

    private nextCheck: ReturnType<typeof setTimeout> | null = null;

    protected readonly stateLabels = STATE_LABELS;
    protected readonly loading = signal(true);
    protected readonly busy = signal(false);
    protected readonly accounts = signal<LolAdminAccount[]>([]);
    protected readonly seasons = signal<LolAdminSeason[]>([]);
    protected readonly linking = signal<LolAdminAccount | null>(null);
    protected readonly removing = signal<LolAdminAccount | null>(null);
    protected readonly downloading = computed(() => this.accounts().some((account) => this.isActive(account)));

    constructor() {
        inject(DestroyRef).onDestroy(() => this.cancelCheck());
    }

    ngOnInit(): void {
        this.reload();
    }

    protected reload(): void {
        this.run(this.api.getAccounts(), 'No se han podido cargar las cuentas');
        this.runSeasons(this.api.getSeasons(), 'No se han podido cargar las temporadas');
    }

    protected saveSeason(key: string, date: string): void {
        if (date) {
            this.runSeasons(
                this.api.saveSeason(key, date),
                'No se ha podido guardar la temporada',
                'Temporada guardada',
            );
        }
    }

    protected deleteSeason(key: string): void {
        this.runSeasons(this.api.deleteSeason(key), 'No se ha podido quitar la temporada', 'Temporada quitada');
    }

    protected addSeason(event: Event, keyField: HTMLInputElement, dateField: HTMLInputElement): void {
        event.preventDefault();

        const key = keyField.value.trim();

        if (!/^\d{2}$/.test(key)) {
            this.notifications.warning('La temporada debe ser un número de dos cifras, como 27');
            return;
        }

        if (!dateField.value) {
            this.notifications.warning('Elige el día en que empieza');
            return;
        }

        this.saveSeason(key, dateField.value);
        keyField.value = '';
        dateField.value = '';
    }

    protected download(account: LolAdminAccount): void {
        this.run(this.api.download(account.userId), 'No se ha podido iniciar la descarga');
    }

    protected pause(account: LolAdminAccount): void {
        this.run(this.api.pause(account.userId), 'No se ha podido pausar la descarga');
    }

    protected downloadAll(): void {
        this.run(this.api.downloadAll(), 'No se han podido iniciar las descargas');
    }

    protected saveLink(event: Event, field: HTMLInputElement): void {
        event.preventDefault();

        const account = this.linking();
        const riotId = field.value.trim();

        if (account && riotId) {
            this.run(this.api.linkAccount(account.userId, riotId), 'No se ha podido vincular la cuenta', () =>
                this.linking.set(null),
            );
        }
    }

    protected confirmRemove(): void {
        const account = this.removing();

        if (account) {
            this.run(this.api.unlinkAccount(account.userId), 'No se ha podido quitar la cuenta', () =>
                this.removing.set(null),
            );
        }
    }

    protected canDownload(account: LolAdminAccount): boolean {
        return ['PENDING', 'ERROR', 'INTERRUPTED'].includes(account.state);
    }

    protected isActive(account: LolAdminAccount): boolean {
        return ACTIVE_STATES.includes(account.state);
    }

    protected percent(account: LolAdminAccount): number {
        return account.total ? Math.round(((account.saved ?? 0) * 100) / account.total) : 0;
    }

    protected timeLeft(account: LolAdminAccount): string {
        const minutes = Math.ceil((account.secondsLeft ?? 0) / 60);

        return minutes < 60 ? `${minutes} min` : `${Math.floor(minutes / 60)} h ${minutes % 60} min`;
    }

    private runSeasons(request: Observable<LolAdminSeason[]>, fallback: string, success?: string): void {
        request.subscribe({
            next: (seasons) => {
                this.seasons.set(seasons);

                if (success) {
                    this.notifications.success(success);
                }
            },
            error: (error) => this.notifications.error(resolveApiError(error, fallback)),
        });
    }

    private run(request: Observable<LolAdminAccount[]>, fallback: string, done?: () => void): void {
        this.cancelCheck();
        this.busy.set(true);
        request.subscribe({
            next: (accounts) => {
                this.show(accounts);
                this.busy.set(false);
                done?.();
            },
            error: (error) => {
                this.loading.set(false);
                this.busy.set(false);
                this.notifications.error(resolveApiError(error, fallback));
                this.scheduleCheck();
            },
        });
    }

    private show(accounts: LolAdminAccount[]): void {
        this.accounts.set(accounts);
        this.loading.set(false);
        this.scheduleCheck();
    }

    private scheduleCheck(): void {
        this.cancelCheck();

        if (this.downloading()) {
            this.nextCheck = setTimeout(() => {
                this.api.getAccounts().subscribe({
                    next: (accounts) => this.show(accounts),
                    error: () => this.scheduleCheck(),
                });
            }, CHECK_EVERY_MS);
        }
    }

    private cancelCheck(): void {
        if (this.nextCheck !== null) {
            clearTimeout(this.nextCheck);
            this.nextCheck = null;
        }
    }
}
