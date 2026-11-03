/**
 * LolModels
 *
 * Models of the League of Legends API. Names and icon URLs come ready to show.
 *
 * @author @adrianpmdev
 */

export type LolAccountState =
    'NOT_LINKED' | 'PENDING' | 'QUEUED' | 'PREPARING' | 'LOADING' | 'COMPLETE' | 'ERROR' | 'INTERRUPTED';

export interface LolPlayerSummary {
    userId: string;
    username: string | null;
    riotId: string;
    state: LolAccountState;
    iconUrl: string | null;
    level: number | null;
}

export interface LolPlayers {
    myUserId: string;
    myRiotId: string | null;
    players: LolPlayerSummary[];
}

export interface LolPeakRank {
    tier: string;
    lp: number;
}

export interface LolRank {
    queue: string;
    tier: string;
    lp: number;
    wins: number;
    losses: number;
    iconUrl: string | null;
    peak: LolPeakRank | null;
}

export interface LolStats {
    games: number;
    wins: number;
    winRate: number;
    kda: number;
    kills: number;
    deaths: number;
    assists: number;
    csPerMinute: number;
}

export interface LolChampionStats {
    name: string;
    icon: string | null;
    games: number;
    winRate: number;
    kda: number;
}

export interface LolMatch {
    id: string;
    playedAt: string;
    durationSeconds: number;
    champion: string;
    championIcon: string | null;
    win: boolean;
    remake: boolean;
    kills: number;
    deaths: number;
    assists: number;
    cs: number;
    damage: number;
    gold: number;
    vision: number;
    level: number;
    items: string[];
    spells: string[];
}

export interface LolQueueBlock {
    id: number;
    label: string;
    emptyNotice: string | null;
    stats: LolStats;
    champions: LolChampionStats[];
    matches: LolMatch[];
}

export interface LolSeasonOption {
    key: string;
    label: string;
}

export interface LolAdminSeason extends LolSeasonOption {
    date: string;
    current: boolean;
}

export interface LolPlayerDetail {
    player: LolPlayerSummary;
    checkedAt: string | null;
    updating: boolean;
    error: string | null;
    season: LolSeasonOption;
    current: boolean;
    seasons: LolSeasonOption[];
    ranks: LolRank[];
    queues: LolQueueBlock[];
}

export interface LolAdminAccount {
    userId: string;
    username: string;
    riotId: string | null;
    state: LolAccountState;
    stored: number;
    saved: number | null;
    total: number | null;
    secondsLeft: number | null;
    error: string | null;
}
