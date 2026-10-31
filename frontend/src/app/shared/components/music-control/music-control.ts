/**
 * MusicControl
 *
 * Floating control of the background music in the top right corner of every screen with a session, on computers
 * only: a button that mutes or unmutes it and, while the pointer is over it, a volume slider.
 *
 * @author @adrianpmdev
 */

import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { MusicPlayer } from '@core/music/music-player';

@Component({
    changeDetection: ChangeDetectionStrategy.OnPush,
    selector: 'app-music-control',
    templateUrl: './music-control.html',
    styleUrl: './music-control.scss',
})
export class MusicControl {
    protected readonly player = inject(MusicPlayer);

    protected onVolume(event: Event): void {
        const value = Number((event.target as HTMLInputElement).value);

        this.player.setVolume(value);

        if (this.player.muted() && value > 0) {
            this.player.toggleMute();
        }
    }
}
