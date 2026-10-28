/**
 * Login
 *
 * Login page component.
 * Handles user authentication, login errors and access redirection.
 * On init, tries to restore an existing session via the refresh-token cookie.
 * If the cookie is still valid the user goes straight to the dashboard.
 *
 * @author @adrianpmdev
 */

import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthSession } from '@core/auth/auth-session';
import { NotificationCenter } from '@core/notifications/notification-center';

@Component({
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: { class: 'terminal' },
    selector: 'app-login',
    imports: [FormsModule],
    templateUrl: './login.html',
    styleUrl: './login.scss',
})
export class Login implements OnInit {
    private readonly authSession = inject(AuthSession);
    private readonly router = inject(Router);
    private readonly notificationCenter = inject(NotificationCenter);

    private readonly matrixColumnCount = 20;
    private readonly matrixCharacterCount = 40;
    private readonly matrixMinSpeed = 6;
    private readonly matrixMaxSpeedOffset = 10;
    private readonly matrixMaxDelayOffset = 10;
    private readonly katakanaStartCode = 0x30a0;
    private readonly katakanaRange = 96;

    protected email = '';
    protected password = '';

    protected readonly loading = signal(false);
    protected readonly error = signal('');

    protected readonly checkingSession = signal(true);

    protected readonly matrixCols = Array.from({ length: this.matrixColumnCount }, (_, index) => ({
        id: index,
        x: (index / this.matrixColumnCount) * 100,
        speed: this.matrixMinSpeed + Math.random() * this.matrixMaxSpeedOffset,
        delay: -Math.random() * this.matrixMaxDelayOffset,
        chars: this.generateMatrixCharacters(),
    }));

    ngOnInit(): void {
        if (this.authSession.isLoggedIn()) {
            this.router.navigate(['/']);
            return;
        }

        this.authSession.refreshSession().subscribe({
            next: () => {
                this.notificationCenter.success('Sesión restaurada. Bienvenido');
                this.router.navigate(['/']);
            },
            error: () => {
                this.checkingSession.set(false);
            },
        });
    }

    protected signIn(): void {
        if (!this.email || !this.password) {
            this.error.set('Debes introducir las credenciales');
            return;
        }

        this.loading.set(true);
        this.error.set('');

        this.authSession
            .login({
                email: this.email,
                password: this.password,
            })
            .subscribe({
                next: () => this.handleLoginSuccess(),
                error: (error) => this.handleLoginError(error),
            });
    }

    private handleLoginSuccess(): void {
        this.notificationCenter.success('Acceso concedido. Bienvenido');
        this.router.navigate(['/']);
    }

    private handleLoginError(error: unknown): void {
        this.error.set(this.resolveLoginError(error));
        this.loading.set(false);
    }

    private resolveLoginError(error: unknown): string {
        if (error instanceof Error) {
            return error.message;
        }

        if (this.hasApiErrorMessage(error)) {
            return error.error.detail || error.error.message || 'Credenciales inválidas';
        }

        return 'Credenciales inválidas';
    }

    private hasApiErrorMessage(error: unknown): error is { error: { detail?: string; message?: string } } {
        return (
            typeof error === 'object' &&
            error !== null &&
            'error' in error &&
            typeof (error as { error?: unknown }).error === 'object' &&
            (error as { error?: unknown }).error !== null
        );
    }

    private generateMatrixCharacters(): string {
        return Array.from({ length: this.matrixCharacterCount }, () => {
            const characterCode = this.katakanaStartCode + Math.floor(Math.random() * this.katakanaRange);

            return String.fromCharCode(characterCode);
        }).join('');
    }
}
