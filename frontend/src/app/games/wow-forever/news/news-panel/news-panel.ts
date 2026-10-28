/**
 * NewsPanel
 *
 * Parchment panel of the launcher with the latest news of the source feed: the newest one with a large picture, the
 * rest as a list, each opening the full article in the source. Dates are shown relative and absolute, and the relative
 * one moves on every minute while the launcher is open.
 *
 * @author @adrianpmdev
 */

import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { catchError, interval, map, of, startWith } from 'rxjs';
import { hideBrokenIcon } from '../../wow-forever-config';
import { NewsApi } from '../news-api';
import { absoluteDate, relativeDate } from '../news-date';
import { NewsFeed } from '../news-models';

type NewsState = { status: 'loading' } | { status: 'error' } | { status: 'ready'; feed: NewsFeed };

@Component({
    selector: 'app-news-panel',
    templateUrl: './news-panel.html',
    styleUrl: './news-panel.scss',
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class NewsPanel {
    protected readonly state = toSignal(
        inject(NewsApi)
            .getNews()
            .pipe(
                map((feed): NewsState => ({ status: 'ready', feed })),
                catchError(() => of<NewsState>({ status: 'error' })),
            ),
        { initialValue: { status: 'loading' } as NewsState },
    );

    private readonly now = toSignal(
        interval(60_000).pipe(
            map(() => Date.now()),
            startWith(Date.now()),
        ),
        { initialValue: Date.now() },
    );

    protected readonly feed = computed(() => {
        const state = this.state();
        return state.status === 'ready' ? state.feed : null;
    });

    protected readonly items = computed(() =>
        (this.feed()?.items ?? []).map((item) => ({
            ...item,
            relative: relativeDate(item.publishedAt, this.now()),
            absolute: absoluteDate(item.publishedAt),
        })),
    );

    protected readonly hideBrokenIcon = hideBrokenIcon;
}
