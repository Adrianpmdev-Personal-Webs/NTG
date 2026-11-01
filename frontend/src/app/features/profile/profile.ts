/**
 * ProfilePage
 *
 * The profile of the signed-in user, the only one anybody can see: their account and one block per game they can open,
 * as the backend tells it.
 *
 * @author @adrianpmdev
 */

import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Profile, ProfileApi } from './profile-api';

@Component({
    changeDetection: ChangeDetectionStrategy.OnPush,
    selector: 'app-profile',
    imports: [DatePipe, RouterLink],
    templateUrl: './profile.html',
    styleUrl: './profile.scss',
})
export class ProfilePage implements OnInit {
    private readonly api = inject(ProfileApi);

    protected readonly loading = signal(true);
    protected readonly failed = signal(false);
    protected readonly profile = signal<Profile | null>(null);

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
}
