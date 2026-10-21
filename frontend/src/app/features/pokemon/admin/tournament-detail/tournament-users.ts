/**
 * TournamentUsersFacade
 *
 * Tournament users facade.
 * Provides user lookup data used across tournament sections.
 *
 * @author @adrianpmdev
 */

import { Injectable, inject, signal } from '@angular/core';
import { User } from '@shared/models/user';
import { UserService } from '@core/services/user';

@Injectable({ providedIn: 'root' })
export class TournamentUsersFacade {

    private readonly userService = inject(UserService);

    readonly allUsers = signal<User[]>([]);

    loadAllUsers(): void {
        this.userService.getAllUsers().subscribe({
            next: users => this.allUsers.set(users)
        });
    }

    getUserName(userId: string): string {
        const user = this.allUsers().find(currentUser => currentUser.id === userId);

        return user ? user.username : `${userId.slice(0, 8)}...`;
    }
}
