-- =====================================================================================================
-- Schema lol · League of Legends module
--
--   accounts   The Riot account linked to each platform user (one per user), its official rank and how
--              the download of its games is going.
--   matches    A summary of each game of a player, never the whole file of the game. Games stay from one
--              season to the next; each one belongs to a season by its date.
--   seasons    The seasons, one per year, with the day each one starts.
--   season_ranks  The last official rank known of each player in each season.
--
-- How to use: run it in the Supabase SQL editor, after core.sql; it can be run again on an existing
-- project, it only adds what is missing and keeps the data. A new column of an existing table needs its
-- own "alter table ... add column if not exists".
-- =====================================================================================================

create schema if not exists lol;

insert into core.tag (key, name)
values ('lol', 'League of Legends')
on conflict (key) do nothing;

-- ─── accounts ────────────────────────────────────────────────────────────────────────────────────────

create table if not exists lol.accounts (
    -- Platform account (core.user.id). A plain column, like every user reference in game schemas.
    user_id      uuid         not null,
    game_name    varchar(16)  not null,
    tag_line     varchar(5)   not null,
    puuid        varchar(100) not null,
    -- PENDING until an admin starts the download, LOADING during it, COMPLETE after it, ERROR if it failed.
    state        varchar(10)  not null default 'PENDING',
    -- Official rank by queue, as the backend last read it.
    ranks        jsonb        not null default '[]',
    -- Profile icon and summoner level, as the backend last read them.
    profile_icon_id integer,
    summoner_level  integer,
    -- Games are complete up to this moment; the next check only asks for newer ones.
    synced_until timestamptz,
    checked_at   timestamptz,
    last_error   varchar(300),
    created_at   timestamptz  not null default now(),
    constraint accounts_pkey primary key (user_id),
    constraint accounts_puuid_key unique (puuid),
    constraint accounts_state_check check (state in ('PENDING', 'LOADING', 'COMPLETE', 'ERROR'))
);

alter table lol.accounts add column if not exists profile_icon_id integer;
alter table lol.accounts add column if not exists summoner_level integer;

-- ─── matches ─────────────────────────────────────────────────────────────────────────────────────────

create table if not exists lol.matches (
    user_id          uuid        not null,
    match_id         varchar(30) not null,
    queue_id         smallint    not null,
    played_at        timestamptz not null,
    duration_seconds integer     not null,
    champion_id      integer     not null,
    champion_name    varchar(40) not null,
    win              boolean     not null,
    -- A game ended by an early surrender: shown in the history, left out of the statistics.
    remake           boolean     not null,
    kills            smallint    not null,
    deaths           smallint    not null,
    assists          smallint    not null,
    cs               smallint    not null,
    damage           integer     not null,
    gold             integer     not null,
    vision           smallint    not null,
    champion_level   smallint    not null,
    -- Item ids of the six slots and the trinket, separated by commas.
    items            varchar(60) not null,
    spell1           smallint    not null,
    spell2           smallint    not null,
    constraint matches_pkey primary key (user_id, match_id),
    constraint matches_account_fkey foreign key (user_id) references lol.accounts (user_id) on delete cascade
);

create index if not exists matches_history_idx on lol.matches (user_id, queue_id, played_at desc);

-- ─── seasons ─────────────────────────────────────────────────────────────────────────────────────────
-- One per year, managed by admins from the web. A game belongs to a season by its date.

create table if not exists lol.seasons (
    key        varchar(5) not null,
    start_date date       not null,
    constraint seasons_pkey primary key (key),
    constraint seasons_start_key unique (start_date)
);

insert into lol.seasons (key, start_date)
values ('26', '2026-01-08')
on conflict (key) do nothing;

-- ─── season_ranks ────────────────────────────────────────────────────────────────────────────────────
-- Riot only tells the current rank, so each update keeps a copy: the last one of a season is how it ended.

create table if not exists lol.season_ranks (
    user_id    uuid        not null,
    season     varchar(5)  not null,
    ranks      jsonb       not null default '[]',
    updated_at timestamptz not null default now(),
    constraint season_ranks_pkey primary key (user_id, season),
    constraint season_ranks_account_fkey foreign key (user_id) references lol.accounts (user_id) on delete cascade
);

-- ─── Row level security ──────────────────────────────────────────────────────────────────────────────
-- The backend connects as the table owner, which bypasses RLS. Enabling it without policies closes the
-- tables to the Supabase Data API (anon and authenticated roles) in case the schema is ever exposed.

alter table lol.accounts enable row level security;
alter table lol.matches  enable row level security;
alter table lol.seasons enable row level security;
alter table lol.season_ranks enable row level security;
