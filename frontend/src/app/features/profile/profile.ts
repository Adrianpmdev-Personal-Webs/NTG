/**
 * ProfilePage
 *
 * The profile of the signed-in user, the only one anybody can see. Their account stays on one side; on the other, an
 * overview with one line per game and one tab per game, so the screen keeps its shape however many games there are.
 * From here they also change their name and avatar or ask for the email to choose a new password.
 *
 * @author @adrianpmdev
 */

import { DatePipe, NgTemplateOutlet } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthSession } from '@core/auth/auth-session';
import { ACCEPTED_IMAGE_TYPES, checkImageFile } from '@core/assets/asset-api';
import { NotificationCenter } from '@core/notifications/notification-center';
import { UserApi } from '@core/users/user-api';
import { ModalOverlay } from '@shared/directives/modal-overlay';
import { User } from '@shared/models/user';
import { resolveApiError } from '@shared/utils/api-error';
import { Profile, ProfileApi, ProfileBar } from './profile-api';

const OVERVIEW = 'overview';
const USERNAME_MIN_LENGTH = 3;
const USERNAME_MAX_LENGTH = 50;

@Component({
    changeDetection: ChangeDetectionStrategy.OnPush,
    selector: 'app-profile',
    imports: [DatePipe, ModalOverlay, NgTemplateOutlet, RouterLink],
    templateUrl: './profile.html',
    styleUrl: './profile.scss',
})
export class ProfilePage implements OnInit {
    private readonly api = inject(ProfileApi);
    private readonly userApi = inject(UserApi);
    private readonly authSession = inject(AuthSession);
    private readonly notifications = inject(NotificationCenter);

    protected readonly overview = OVERVIEW;
    protected readonly accepted = ACCEPTED_IMAGE_TYPES;
    protected readonly usernameMaxLength = USERNAME_MAX_LENGTH;

    protected readonly loading = signal(true);
    protected readonly failed = signal(false);
    protected readonly profile = signal<Profile | null>(null);
    protected readonly selected = signal(OVERVIEW);
    protected readonly editing = signal(false);
    protected readonly busy = signal(false);

    protected readonly selectedGame = computed(
        () => this.profile()?.games.find((game) => game.id === this.selected()) ?? null,
    );

    ngOnInit(): void {
        this.api.getOwn().subscribe({
            next: (profile) => {
                this.profile.set(profile);
                this.loading.set(false);
            },
            error: () => {
                this.failed.set(true);
                this.loading.set(false);
            },
        });
    }

    protected barPercent(bar: ProfileBar): number {
        return bar.max > 0 ? Math.min(100, Math.round(((bar.current ?? 0) * 100) / bar.max)) : 0;
    }

    protected hideIcon(event: Event): void {
        (event.target as HTMLElement).style.visibility = 'hidden';
    }

    protected saveUsername(field: HTMLInputElement): void {
        const current = this.profile();
        const username = field.value.trim();

        if (!current || this.busy() || username === current.username) {
            return;
        }

        if (username.length < USERNAME_MIN_LENGTH || username.length > USERNAME_MAX_LENGTH) {
            this.notifications.warning(
                `El nombre debe tener entre ${USERNAME_MIN_LENGTH} y ${USERNAME_MAX_LENGTH} caracteres`,
            );
            return;
        }

        this.busy.set(true);
        this.userApi.updateMe({ username, avatarUrl: current.avatarUrl ?? undefined }).subscribe({
            next: (user) => this.applied(user, 'Nombre actualizado'),
            error: (error) => this.failedAction(error, 'No se ha podido cambiar el nombre'),
        });
    }

    protected uploadAvatar(input: HTMLInputElement): void {
        const file = input.files?.[0];

        input.value = '';

        if (!file || this.busy()) {
            return;
        }

        const problem = checkImageFile(file);

        if (problem) {
            this.notifications.warning(problem);
            return;
        }

        this.busy.set(true);
        this.userApi.uploadMyAvatar(file).subscribe({
            next: (user) => this.applied(user, 'Avatar actualizado'),
            error: (error) => this.failedAction(error, 'No se ha podido subir el avatar'),
        });
    }

    protected removeAvatar(): void {
        if (this.busy()) {
            return;
        }

        this.busy.set(true);
        this.userApi.deleteMyAvatar().subscribe({
            next: (user) => this.applied(user, 'Avatar quitado'),
            error: (error) => this.failedAction(error, 'No se ha podido quitar el avatar'),
        });
    }

    protected sendPasswordEmail(): void {
        const current = this.profile();

        if (!current || this.busy()) {
            return;
        }

        this.busy.set(true);
        this.userApi.sendMyPasswordEmail().subscribe({
            next: () => {
                this.busy.set(false);
                this.notifications.success(`Te hemos enviado el enlace a ${current.email}`);
            },
            error: (error) => this.failedAction(error, 'No se ha podido enviar el email'),
        });
    }

    private applied(user: User, message: string): void {
        this.profile.update(
            (current) => current && { ...current, username: user.username, avatarUrl: user.avatarUrl ?? null },
        );
        this.authSession.reloadUser();
        this.busy.set(false);
        this.notifications.success(message);
    }

    private failedAction(error: unknown, fallback: string): void {
        this.busy.set(false);
        this.notifications.error(resolveApiError(error, fallback));
    }
}
