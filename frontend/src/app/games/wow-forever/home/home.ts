/**
 * Home
 *
 * Launcher-style home of WoW Forever: faction artwork, news and the tools of the area (tools, news and faction names
 * come from the backend index).
 *
 * @author @adrianpmdev
 */

import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { catchError, of } from 'rxjs';
import { GameDataApi } from '../game-data/game-data-api';
import { cssUrl, gameImages } from '../game-data/game-images';
import { Faction } from '../wow-forever-models';
import { LegalNotice } from '../ui/legal-notice/legal-notice';
import { WowButton } from '../ui/wow-button/wow-button';
import { WowFrame } from '../ui/wow-frame/wow-frame';
import { LauncherHeader } from './launcher-header/launcher-header';
import { NewsPanel } from './news-panel/news-panel';

@Component({
    selector: 'app-wow-forever-home',
    imports: [LegalNotice, WowButton, WowFrame, LauncherHeader, NewsPanel],
    templateUrl: './home.html',
    styleUrl: './home.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class Home {

    private readonly index = toSignal(inject(GameDataApi).getIndex().pipe(catchError(() => of(null))), { initialValue: null });

    readonly features = computed(() => this.index()?.features ?? []);
    readonly news = computed(() => this.index()?.news ?? []);
    readonly factions = computed(() => this.index()?.factions ?? null);
    readonly faction = signal<Faction>('ALLIANCE');

    private readonly images = gameImages();

    /** Artwork of the selected faction, uploaded by an admin. */
    readonly backgroundImage = computed(() => cssUrl(this.images()[`backgrounds/${this.faction().toLowerCase()}`]));

    onFactionChange(value: string): void {
        this.faction.set(value === 'HORDE' ? 'HORDE' : 'ALLIANCE');
    }
}
