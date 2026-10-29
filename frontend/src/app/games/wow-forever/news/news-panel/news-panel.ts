/**
 * NewsPanel
 *
 * Parchment panel of the launcher with two tabs: the latest news of the source feed (the newest one with a large
 * picture, the rest as a list) and the latest blue posts of the official forums. Each item opens the full article or
 * post in its source. The blue posts are only asked for the first time their tab is opened. Dates are shown relative
 * and absolute, and the relative one moves on every minute while the launcher is open.
 *
 * @author @adrianpmdev
 */

import {
    ChangeDetectionStrategy,
    Component,
    ElementRef,
    computed,
    inject,
    signal,
    viewChild,
    viewChildren,
} from '@angular/core';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { Observable, catchError, filter, interval, map, of, startWith, switchMap, take } from 'rxjs';
import { hideBrokenIcon } from '../../wow-forever-config';
import { NewsApi } from '../news-api';
import { absoluteDate, relativeDate } from '../news-date';

type FeedState<T> = { status: 'loading' } | { status: 'error' } | { status: 'ready'; feed: T };

type TabId = 'news' | 'blue-posts';

const TABS = [
    {
        id: 'news',
        label: 'Noticias',
        loading: 'Cargando noticias…',
        error: 'No se han podido cargar las noticias.',
        empty: 'No hay noticias por ahora.',
    },
    {
        id: 'blue-posts',
        label: 'Blue posts',
        loading: 'Cargando blue posts…',
        error: 'No se han podido cargar los blue posts.',
        empty: 'No hay blue posts por ahora.',
    },
] as const satisfies readonly { id: TabId; label: string; loading: string; error: string; empty: string }[];

const load = <T>(request: Observable<T>): Observable<FeedState<T>> =>
    request.pipe(
        map((feed): FeedState<T> => ({ status: 'ready', feed })),
        catchError(() => of<FeedState<T>>({ status: 'error' })),
        startWith<FeedState<T>>({ status: 'loading' }),
    );

@Component({
    selector: 'app-news-panel',
    templateUrl: './news-panel.html',
    styleUrl: './news-panel.scss',
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class NewsPanel {
    private readonly newsApi = inject(NewsApi);
    private readonly paper = viewChild.required<ElementRef<HTMLElement>>('paper');
    private readonly tabButtons = viewChildren<ElementRef<HTMLButtonElement>>('tabButton');

    protected readonly tabs = TABS;
    protected readonly activeTab = signal<TabId>('news');

    private readonly newsState = toSignal(load(this.newsApi.getNews()), { initialValue: { status: 'loading' } });

    private readonly bluePostsState = toSignal(
        toObservable(this.activeTab).pipe(
            filter((tab) => tab === 'blue-posts'),
            take(1),
            switchMap(() => load(this.newsApi.getBluePosts())),
        ),
        { initialValue: { status: 'loading' } },
    );

    private readonly now = toSignal(
        interval(60_000).pipe(
            map(() => Date.now()),
            startWith(Date.now()),
        ),
        { initialValue: Date.now() },
    );

    protected readonly tab = computed(() => TABS.find((tab) => tab.id === this.activeTab()) ?? TABS[0]);

    protected readonly status = computed(() =>
        this.activeTab() === 'news' ? this.newsState().status : this.bluePostsState().status,
    );

    protected readonly source = computed(() => {
        const state = this.activeTab() === 'news' ? this.newsState() : this.bluePostsState();
        return state.status === 'ready' ? state.feed : null;
    });

    protected readonly newsItems = computed(() => {
        const state = this.newsState();
        return (state.status === 'ready' ? state.feed.items : []).map((item) => ({
            ...item,
            relative: relativeDate(item.publishedAt, this.now()),
            absolute: absoluteDate(item.publishedAt),
        }));
    });

    protected readonly bluePosts = computed(() => {
        const state = this.bluePostsState();
        return (state.status === 'ready' ? state.feed.items : []).map((post) => ({
            ...post,
            heading: post.reply ? `Re: ${post.title}` : post.title,
            relative: relativeDate(post.publishedAt, this.now()),
            absolute: absoluteDate(post.publishedAt),
        }));
    });

    protected readonly isEmpty = computed(() =>
        this.activeTab() === 'news' ? !this.newsItems().length : !this.bluePosts().length,
    );

    protected readonly hideBrokenIcon = hideBrokenIcon;

    protected selectTab(id: TabId): void {
        if (id !== this.activeTab()) {
            this.activeTab.set(id);
            this.paper().nativeElement.scrollTop = 0;
        }
    }

    protected moveTab(event: KeyboardEvent, index: number): void {
        const last = TABS.length - 1;
        const targets: Record<string, number> = {
            ArrowRight: index === last ? 0 : index + 1,
            ArrowLeft: index === 0 ? last : index - 1,
            Home: 0,
            End: last,
        };
        const target = targets[event.key];

        if (target === undefined) {
            return;
        }

        event.preventDefault();
        this.selectTab(TABS[target].id);
        this.tabButtons()[target]?.nativeElement.focus();
    }
}
