/**
 * AdminAccounts
 *
 * Accounts of League of Legends: an admin links the Riot ID of each player with the tag and starts the download of their
 * games. Downloads run in the backend one after another, so this screen can be closed. Nothing is polled: the state is
 * read when the screen opens, after an action or when the button is pressed.
 *
 * @author @adrianpmdev
 */

import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { Observable } from 'rxjs';
import { NotificationCenter } from '@core/notifications/notification-center';
import { ModalOverlay } from '@shared/directives/modal-overlay';
import { resolveApiError } from '@shared/utils/api-error';
import { LolApi } from '../data/lol-api';
import { LolAccountState, LolAdminAccount } from '../data/lol-models';

const STATE_LABELS: Readonly<Record<LolAccountState, string>> = {
    NOT_LINKED: 'SIN VINCULAR',
    PENDING: 'PENDIENTE',
    LOADING: 'DESCARGANDO',
    COMPLETE: 'COMPLETO',
    ERROR: 'ERROR',
    INTERRUPTED: 'INTERRUMPIDA',
};

@Component({
    changeDetection: ChangeDetectionStrategy.OnPush,
    selector: 'app-lol-admin-accounts',
    imports: [ModalOverlay],
    templateUrl: './admin-accounts.html',
})
export class AdminAccounts implements OnInit {
    private readonly api = inject(LolApi);
    private readonly notifications = inject(NotificationCenter);

    protected readonly stateLabels = STATE_LABELS;
    protected readonly loading = signal(true);
    protected readonly busy = signal(false);
    protected readonly accounts = signal<LolAdminAccount[]>([]);
    protected readonly linking = signal<LolAdminAccount | null>(null);
    protected readonly removing = signal<LolAdminAccount | null>(null);

    ngOnInit(): void {
        this.reload();
    }

    protected reload(): void {
        this.run(this.api.getAccounts(), 'No se han podido cargar las cuentas');
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

    private run(request: Observable<LolAdminAccount[]>, fallback: string, done?: () => void): void {
        this.busy.set(true);
        request.subscribe({
            next: (accounts) => {
                this.accounts.set(accounts);
                this.loading.set(false);
                this.busy.set(false);
                done?.();
            },
            error: (error) => {
                this.loading.set(false);
                this.busy.set(false);
                this.notifications.error(resolveApiError(error, fallback));
            },
        });
    }
}
