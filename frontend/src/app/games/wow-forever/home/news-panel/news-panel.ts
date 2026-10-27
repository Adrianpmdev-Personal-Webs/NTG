/**
 * NewsPanel
 *
 * Parchment panel with the news of the launcher screen.
 *
 * @author @adrianpmdev
 */

import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { LauncherNewsItem } from '../../wow-forever-models';

@Component({
    selector: 'app-news-panel',
    templateUrl: './news-panel.html',
    styleUrl: './news-panel.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class NewsPanel {

    /** News of the launcher, from the backend index. */
    readonly news = input<readonly LauncherNewsItem[]>([]);
}
