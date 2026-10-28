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

import { ChangeDetectionStrategy, Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthSession } from '@core/auth/auth-session';
import { NotificationCenter } from '@core/notifications/notification-center';
import { resolveApiError } from '@shared/utils/api-error';

@Component({
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: { class: 'terminal' },
    selector: 'app-set-password',
    imports: [FormsModule],
    templateUrl: './set-password.html',
    styleUrl: './set-password.scss',
})
export class SetPassword implements OnInit {
    private readonly authSession = inject(AuthSession);
    private readonly router = inject(Router);
    private readonly route = inject(ActivatedRoute);
    private readonly notificationCenter = inject(NotificationCenter);
    private readonly destroyRef = inject(DestroyRef);

    private readonly matrixColumnCount = 20;
    private readonly matrixCharacterCount = 40;
    private readonly matrixMinSpeed = 6;
    private readonly matrixMaxSpeedOffset = 10;
    private readonly matrixMaxDelayOffset = 10;
    private readonly katakanaStartCode = 0x30a0;
    private readonly katakanaRange = 96;

    private readonly minPasswordLength = 8;
    private readonly maxPasswordBytes = 72;
    private readonly redirectDelayMs = 1_500;

    private token = '';

    protected password = '';
    protected confirmPassword = '';

    protected readonly loading = signal(false);
    protected readonly error = signal('');
    protected readonly success = signal(false);
    protected readonly tokenMissing = signal(false);

    protected readonly matrixCols = Array.from({ length: this.matrixColumnCount }, (_, index) => ({
        id: index,
        x: (index / this.matrixColumnCount) * 100,
        speed: this.matrixMinSpeed + Math.random() * this.matrixMaxSpeedOffset,
        delay: -Math.random() * this.matrixMaxDelayOffset,
        chars: this.generateMatrixCharacters(),
    }));

    ngOnInit(): void {
        const fragment = this.route.snapshot.fragment ?? '';
        const params = new URLSearchParams(fragment);

        this.token = params.get('access_token') ?? '';

        if (!this.token) {
            this.tokenMissing.set(true);
            return;
        }

        history.replaceState(null, '', window.location.pathname);
    }

    protected savePassword(): void {
        const validationError = this.validateForm();

        if (validationError) {
            this.error.set(validationError);
            return;
        }

        this.loading.set(true);
        this.error.set('');

        this.authSession.setPassword(this.token, this.password).subscribe({
            next: () => this.handleSuccess(),
            error: (err: unknown) => this.handleError(err),
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
        this.notificationCenter.success('Contraseña establecida. Bienvenido');

        const timeoutId = setTimeout(() => this.router.navigate(['/']), this.redirectDelayMs);

        this.destroyRef.onDestroy(() => clearTimeout(timeoutId));
    }

    private handleError(error: unknown): void {
        this.error.set(resolveApiError(error, 'El enlace no es válido o ha expirado'));
        this.loading.set(false);
    }

    protected goToLogin(): void {
        this.router.navigate(['/login']);
    }

    private generateMatrixCharacters(): string {
        return Array.from({ length: this.matrixCharacterCount }, () => {
            const characterCode = this.katakanaStartCode + Math.floor(Math.random() * this.katakanaRange);

            return String.fromCharCode(characterCode);
        }).join('');
    }
}
