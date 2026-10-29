/**
 * NewsModels
 *
 * The launcher news and blue posts as the backend sends them: the latest items of each source, written as the source
 * writes them (in English), and the credit of the source. `publishedAt` is an ISO-8601 instant.
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

export interface BluePost {
    id: number;
    title: string;
    reply: boolean;
    author: string;
    summary: string | null;
    publishedAt: string;
    url: string;
}

export interface BluePostFeed {
    source: string;
    sourceUrl: string;
    items: BluePost[];
}
