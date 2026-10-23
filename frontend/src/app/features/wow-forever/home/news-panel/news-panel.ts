/**
 * NewsPanel
 *
 * Parchment panel with the news of the launcher screen.
 *
 * @author @adrianpmdev
 */

import { ChangeDetectionStrategy, Component } from '@angular/core';
import { LAUNCHER_NEWS } from '../../wow-forever-config';

@Component({
    selector: 'app-news-panel',
    templateUrl: './news-panel.html',
    styleUrl: './news-panel.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class NewsPanel {

    readonly news = LAUNCHER_NEWS;
}
