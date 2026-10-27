/**
 * Dashboard
 *
 * Dashboard page component.
 * Displays admin statistics or user game library depending on the current role.
 *
 * @author @adrianpmdev
 */

import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { UserService } from '@core/services/user';
import { StatCard } from './dashboard-stats';
import { User } from '@shared/models/user';
import { AuthService } from '@core/services/auth';
import { GameCatalogService } from '@core/services/game-catalog';

@Component({
    changeDetection: ChangeDetectionStrategy.OnPush,
    selector: 'app-dashboard',
    imports: [RouterLink],
    templateUrl: './dashboard.html',
    styleUrl: './dashboard.scss',
})
export class Dashboard implements OnInit {

    private readonly userService = inject(UserService);
    readonly authService = inject(AuthService)

    loading = signal(true);
    isAdmin = this.authService.isAdmin();

    stats = signal<StatCard[]>([]);
    recentUsers = signal<User[]>([]);
    totalUsers = signal(0);
    regularUsersCount = signal(0);
    adminsCount = signal(0);
    uptime = signal('00:00:00');

    availableGames = inject(GameCatalogService).games;

    readonly circumference = 2 * Math.PI * 70;

    usersArc = signal(0);
    adminsArc = signal(0);

    activityLogs = signal([
        { id: 1, time: this.ts(), level: 'info', msg: 'Sistema iniciado correctamente' },
        { id: 2, time: this.ts(), level: 'success', msg: 'Conexión a base de datos establecida' },
        { id: 3, time: this.ts(), level: 'info', msg: 'Módulo de autenticación activo' },
        { id: 4, time: this.ts(), level: 'warn', msg: 'Token de refresh próximo a expirar' },
        { id: 5, time: this.ts(), level: 'info', msg: 'Carga de usuarios completada' },
        { id: 6, time: this.ts(), level: 'success', msg: 'Admin panel operativo' },
    ]);

    systemMetrics = signal([
        { label: 'CPU', value: 23, color: 'var(--neon-green)' },
        { label: 'MEMORIA', value: 61, color: 'var(--neon-cyan)' },
        { label: 'DISCO', value: 38, color: 'var(--neon-yellow)' },
        { label: 'RED', value: 82, color: '#ff8c00' },
        { label: 'API CALLS', value: 45, color: 'var(--neon-green)' },
    ]);

    private startTime = Date.now();

    ngOnInit(): void {
        this.startUptimeTimer();

        if (this.isAdmin) {
            this.loadData();
            this.startLogStream();

            return;
        }

        this.loading.set(false);
    }

    getBarWidth(value: number | string): string {
        const numericValue = typeof value === 'string' ? parseInt(value) : value;
        const max = 100;

        return Math.min((numericValue / max) * 100, 100) + '%';
    }

    isAdminRole(role: string): boolean {
        return role.toLowerCase().replace('role_', '') === 'admin';
    }

    private loadData(): void {
        this.userService.getAllUsers().subscribe({
            next: (users: User[]) => {
                const admins = users.filter(user => this.isAdminRole(user.role));
                const regularUsers = users.filter(user => !this.isAdminRole(user.role));
                const total = users.length;

                this.totalUsers.set(total);
                this.adminsCount.set(admins.length);
                this.regularUsersCount.set(regularUsers.length);

                const usersArc = total > 0 ? (regularUsers.length / total) * this.circumference : 0;
                const adminsArc = total > 0 ? (admins.length / total) * this.circumference : 0;

                setTimeout(() => {
                    this.usersArc.set(usersArc);
                    this.adminsArc.set(adminsArc);
                }, 300);

                this.stats.set([
                    { label: 'TOTAL USUARIOS', value: total, icon: '◎', color: 'var(--neon-green)' },
                    { label: 'ADMINS', value: admins.length, icon: '◈', color: 'var(--neon-cyan)' },
                    { label: 'USUARIOS ACTIVOS', value: regularUsers.length, icon: '◉', color: 'var(--neon-green)' },
                    { label: 'MÓDULOS ACTIVOS', value: 4, icon: '◇', color: 'var(--neon-yellow)' },
                ]);

                this.recentUsers.set(users.slice(0, 8));
                this.loading.set(false);
            },
            error: () => {
                this.stats.set([
                    { label: 'TOTAL USUARIOS', value: 0, icon: '◎', color: 'var(--neon-green)' },
                    { label: 'ADMINS', value: 0, icon: '◈', color: 'var(--neon-cyan)' },
                    { label: 'USUARIOS ACTIVOS', value: 0, icon: '◉', color: 'var(--neon-green)' },
                    { label: 'MÓDULOS ACTIVOS', value: 4, icon: '◇', color: 'var(--neon-yellow)' },
                ]);

                this.loading.set(false);
            }
        });
    }

    private startUptimeTimer(): void {
        setInterval(() => {
            const elapsed = Math.floor((Date.now() - this.startTime) / 1000);
            const hours = Math.floor(elapsed / 3600).toString().padStart(2, '0');
            const minutes = Math.floor((elapsed % 3600) / 60).toString().padStart(2, '0');
            const seconds = (elapsed % 60).toString().padStart(2, '0');

            this.uptime.set(`${hours}:${minutes}:${seconds}`);
        }, 1000);
    }

    private startLogStream(): void {
        const messages = [
            { level: 'info', msg: 'Petición GET /api/v1/users procesada' },
            { level: 'success', msg: 'Autenticación JWT validada correctamente' },
            { level: 'info', msg: 'Consulta a base de datos ejecutada' },
            { level: 'warn', msg: 'Intento de acceso desde IP desconocida' },
            { level: 'info', msg: 'Sincronización con Supabase completada' },
            { level: 'success', msg: 'Cache actualizada correctamente' },
        ];

        let index = 0;

        setInterval(() => {
            const message = messages[index % messages.length];

            this.activityLogs.update(logs => [
                { id: Date.now(), time: this.ts(), ...message },
                ...logs.slice(0, 9)
            ]);

            index++;
        }, 3500);
    }

    private ts(): string {
        return new Date().toLocaleTimeString('es-ES', { hour12: false });
    }
}