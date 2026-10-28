/**
 * NewsModels
 *
 * The launcher news as the backend sends them: the latest items of the source feed, written as the source writes
 * them (in English), and the credit of the source. `publishedAt` is an ISO-8601 instant.
 *
 * @author @adrianpmdev
 */

export interface NewsItem {
    id: number;
    title: string;
    summary: string | null;
    publishedAt: string;
    image: string | null;
    url: string;
}

export interface NewsFeed {
    source: string;
    sourceUrl: string;
    items: NewsItem[];
}
