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
import { AuthService } from '@core/services/auth';
import { NotificationService } from '@core/services/notification';

@Component({
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: { class: 'terminal' },
    selector: 'app-login',
    imports: [FormsModule],
    templateUrl: './login.html',
    styleUrl: './login.scss'
})
export class Login implements OnInit {

    private readonly authService = inject(AuthService);
    private readonly router = inject(Router);
    private readonly notificationService = inject(NotificationService);

    private readonly matrixColumnCount = 20;
    private readonly matrixCharacterCount = 40;
    private readonly matrixMinSpeed = 6;
    private readonly matrixMaxSpeedOffset = 10;
    private readonly matrixMaxDelayOffset = 10;
    private readonly katakanaStartCode = 0x30A0;
    private readonly katakanaRange = 96;

    email = '';
    password = '';

    readonly loading = signal(false);
    readonly error = signal('');

    /** True while the silent session-restore attempt is in progress. */
    readonly checkingSession = signal(true);

    readonly matrixCols = Array.from({ length: this.matrixColumnCount }, (_, index) => ({
        id: index,
        x: (index / this.matrixColumnCount) * 100,
        speed: this.matrixMinSpeed + Math.random() * this.matrixMaxSpeedOffset,
        delay: -Math.random() * this.matrixMaxDelayOffset,
        chars: this.generateMatrixCharacters()
    }));

    ngOnInit(): void {
        // If there is already a valid access token in memory (e.g. the user
        // navigated to /login manually), skip straight to the dashboard.
        if (this.authService.isLoggedIn()) {
            this.router.navigate(['/']);
            return;
        }

        // Otherwise try to silently restore the session using the httpOnly
        // refresh-token cookie. The form stays hidden while this is running
        // so the user never sees a flash of the login form if they have
        // a valid session.
        this.authService.refreshSession().subscribe({
            next: () => {
                this.notificationService.success('Sesión restaurada. Bienvenido');
                this.router.navigate(['/']);
            },
            error: () => {
                // Cookie missing or expired — show the login form normally.
                this.checkingSession.set(false);
            }
        });
    }

    onLogin(): void {
        if (!this.email || !this.password) {
            this.error.set('Debes introducir las credenciales');
            return;
        }

        this.loading.set(true);
        this.error.set('');

        this.authService.login({
            email: this.email,
            password: this.password
        }).subscribe({
            next: () => this.handleLoginSuccess(),
            error: error => this.handleLoginError(error)
        });
    }

    private handleLoginSuccess(): void {
        this.notificationService.success('Acceso concedido. Bienvenido');
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
        return typeof error === 'object'
            && error !== null
            && 'error' in error
            && typeof (error as { error?: unknown }).error === 'object'
            && (error as { error?: unknown }).error !== null;
    }

    private generateMatrixCharacters(): string {
        return Array.from({ length: this.matrixCharacterCount }, () => {
            const characterCode = this.katakanaStartCode + Math.floor(Math.random() * this.katakanaRange);

            return String.fromCharCode(characterCode);
        }).join('');
    }
}
