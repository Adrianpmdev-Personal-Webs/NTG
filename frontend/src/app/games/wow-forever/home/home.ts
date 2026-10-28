/**
 * Home
 *
 * Launcher-style home of WoW Forever: faction artwork, news, the game build and the tools of the area (tools, faction
 * names and the build come from the backend index; the news panel loads its own news). The faction background and the
 * large button that switches it only exist on desktop.
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
import { NewsPanel } from '../news/news-panel/news-panel';
import { LauncherHeader } from './launcher-header/launcher-header';

@Component({
    selector: 'app-wow-forever-home',
    imports: [LegalNotice, WowButton, WowFrame, LauncherHeader, NewsPanel],
    templateUrl: './home.html',
    styleUrl: './home.scss',
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Home {
    private readonly index = toSignal(
        inject(GameDataApi)
            .getIndex()
            .pipe(catchError(() => of(null))),
        { initialValue: null },
    );

    protected readonly features = computed(() => this.index()?.features ?? []);
    protected readonly build = computed(() => this.index()?.credit.build ?? null);
    protected readonly faction = signal<Faction>('ALLIANCE');

    private readonly nextFaction = computed<Faction>(() => (this.faction() === 'ALLIANCE' ? 'HORDE' : 'ALLIANCE'));

    protected readonly nextFactionName = computed(
        () => this.index()?.factions[this.nextFaction()] ?? (this.nextFaction() === 'HORDE' ? 'Horda' : 'Alianza'),
    );

    private readonly images = gameImages();

    protected readonly backgroundImage = computed(() =>
        cssUrl(this.images()[`backgrounds/${this.faction().toLowerCase()}`]),
    );

    protected switchFaction(): void {
        this.faction.set(this.nextFaction());
    }
}
