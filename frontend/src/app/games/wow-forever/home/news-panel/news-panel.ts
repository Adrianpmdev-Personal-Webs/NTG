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
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class NewsPanel {
    readonly news = input<readonly LauncherNewsItem[]>([]);
}
