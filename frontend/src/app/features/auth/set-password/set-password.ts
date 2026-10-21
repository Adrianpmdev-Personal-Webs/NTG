/**
 * SetPassword
 *
 * Set-password page component.
 * Lets a newly created user (or one whose link was resent) choose their own password.
 * Flow:
 *  1. Supabase sends an email whose button opens /set-password#access_token=XXX&...
 *     (an expired or used link opens /set-password#error=access_denied&error_code=otp_expired).
 *  2. This page reads the access token from the URL fragment and removes it from the address bar.
 *  3. On submit it calls PUT /api/v1/auth/set-password with the token and the new password.
 *  4. The backend signs the user in, so on success the user goes straight to the site.
 *
 * @author @adrianpmdev
 */

import { ChangeDetectionStrategy, Component, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '@core/services/auth';
import { NotificationService } from '@core/services/notification';

@Component({
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: { class: 'terminal' },
    selector: 'app-set-password',
    imports: [FormsModule],
    templateUrl: './set-password.html',
    styleUrl: './set-password.scss'
})
export class SetPassword implements OnInit {

    private readonly authService = inject(AuthService);
    private readonly router = inject(Router);
    private readonly route = inject(ActivatedRoute);
    private readonly notificationService = inject(NotificationService);

    private readonly matrixColumnCount = 20;
    private readonly matrixCharacterCount = 40;
    private readonly matrixMinSpeed = 6;
    private readonly matrixMaxSpeedOffset = 10;
    private readonly matrixMaxDelayOffset = 10;
    private readonly katakanaStartCode = 0x30A0;
    private readonly katakanaRange = 96;

    private readonly minPasswordLength = 8;
    private readonly maxPasswordBytes = 72;
    private readonly redirectDelayMs = 1_500;

    private token = '';

    password = '';
    confirmPassword = '';

    readonly loading = signal(false);
    readonly error = signal('');
    readonly success = signal(false);
    readonly tokenMissing = signal(false);

    readonly matrixCols = Array.from({ length: this.matrixColumnCount }, (_, index) => ({
        id: index,
        x: (index / this.matrixColumnCount) * 100,
        speed: this.matrixMinSpeed + Math.random() * this.matrixMaxSpeedOffset,
        delay: -Math.random() * this.matrixMaxDelayOffset,
        chars: this.generateMatrixCharacters()
    }));

    ngOnInit(): void {
        const fragment = this.route.snapshot.fragment ?? '';
        const params = new URLSearchParams(fragment);

        this.token = params.get('access_token') ?? '';

        if (!this.token) {
            this.tokenMissing.set(true);
            return;
        }

        // The token is a credential: keep it out of the address bar and the browser history.
        history.replaceState(null, '', window.location.pathname);
    }

    onSubmit(): void {
        const validationError = this.validateForm();

        if (validationError) {
            this.error.set(validationError);
            return;
        }

        this.loading.set(true);
        this.error.set('');

        this.authService.setPassword(this.token, this.password).subscribe({
            next: () => this.handleSuccess(),
            error: (err: unknown) => this.handleError(err)
        });
    }

    private validateForm(): string | null {
        if (!this.password) {
            return 'Debes introducir una contraseña';
        }

        if (this.password.length < this.minPasswordLength) {
            return `La contraseña debe tener al menos ${this.minPasswordLength} caracteres`;
        }

        if (new TextEncoder().encode(this.password).length > this.maxPasswordBytes) {
            return 'La contraseña es demasiado larga';
        }

        if (!/\p{L}/u.test(this.password) || !/\d/.test(this.password)) {
            return 'La contraseña debe incluir al menos una letra y un número';
        }

        if (this.password !== this.confirmPassword) {
            return 'Las contraseñas no coinciden';
        }

        return null;
    }

    private handleSuccess(): void {
        this.success.set(true);
        this.loading.set(false);
        this.notificationService.success('Contraseña establecida. Bienvenido');

        setTimeout(() => this.router.navigate(['/']), this.redirectDelayMs);
    }

    private handleError(error: unknown): void {
        this.error.set(this.resolveError(error));
        this.loading.set(false);
    }

    private resolveError(error: unknown): string {
        if (this.hasApiErrorMessage(error)) {
            return (error as { error: { detail?: string; message?: string } }).error.detail
                || (error as { error: { detail?: string; message?: string } }).error.message
                || 'El enlace no es válido o ha expirado';
        }

        return 'El enlace no es válido o ha expirado';
    }

    private hasApiErrorMessage(error: unknown): boolean {
        return typeof error === 'object'
            && error !== null
            && 'error' in error
            && typeof (error as { error?: unknown }).error === 'object'
            && (error as { error?: unknown }).error !== null;
    }

    goToLogin(): void {
        this.router.navigate(['/login']);
    }

    private generateMatrixCharacters(): string {
        return Array.from({ length: this.matrixCharacterCount }, () => {
            const characterCode = this.katakanaStartCode + Math.floor(Math.random() * this.katakanaRange);

            return String.fromCharCode(characterCode);
        }).join('');
    }
}
