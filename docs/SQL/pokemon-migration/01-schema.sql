-- =====================================================================================================
-- Pokémon module · 01 · New schema
--
-- Replaces the 13 tables of the first version with 9. Run it once in the Supabase SQL editor, before
-- 02-legacy-data.sql. It keeps pokemon.games and its rows (the only data worth keeping) and drops
-- everything else of the old pokemon schema.
--
-- Model
--   games                One Pokémon game (artwork and description).
--   lockes               A randomlocke: name, description and rules. It can span several games.
--   players              Everybody who plays in a randomlocke. user_id links the platform account and
--                        is what lets a player upload their own team picture.
--   locke_players        The fixed roster of a randomlocke.
--   tournaments          A tournament of a randomlocke, played in one game. Two formats: two groups
--                        followed by a double-elimination bracket, or a round robin league.
--   tournament_players   A player inside a tournament: group, team picture, points adjustment.
--   matches              A best-of-3 series, placed by stage, round and slot.
--   match_games          The games of a series. The score of a series is counted from them.
--   rewards              The rewards of a tournament and the player who picked each one.
--
-- Nothing derived is stored: standings, scores and winners are computed from match_games.
-- points_adjustment exists only to keep the historical standings of the old database, whose points
-- were partly given by final placement; new tournaments score points per series win only.
-- =====================================================================================================

begin;

-- ─── Drop the first version (everything but games) ───────────────────────────────────────────────

drop table if exists
    pokemon.user_rewards,
    pokemon.rewards,
    pokemon.global_standings,
    pokemon.tournament_standings,
    pokemon.bracket_matches,
    pokemon.bracket_stages,
    pokemon.group_matches,
    pokemon.group_participants,
    pokemon.tournament_groups,
    pokemon.tournament_participants,
    pokemon.tournaments,
    pokemon.lockes
    cascade;

-- ─── games (kept as it is) ───────────────────────────────────────────────────────────────────────

create table if not exists pokemon.games (
    id          uuid         not null default gen_random_uuid(),
    name        varchar(100) not null,
    description varchar(1000),
    image_url   varchar(500) not null,
    created_at  timestamptz  not null default now(),
    constraint games_pkey primary key (id)
);

-- ─── lockes ──────────────────────────────────────────────────────────────────────────────────────

create table pokemon.lockes (
    id          uuid         not null default gen_random_uuid(),
    name        varchar(100) not null,
    description varchar(1000),
    rules       jsonb,
    created_at  timestamptz  not null default now(),
    constraint lockes_pkey primary key (id)
);

-- ─── players ─────────────────────────────────────────────────────────────────────────────────────

create table pokemon.players (
    id         uuid        not null default gen_random_uuid(),
    name       varchar(50) not null,
    -- Platform account (core.user.id). A plain column, like every user reference in game schemas.
    user_id    uuid,
    created_at timestamptz not null default now(),
    constraint players_pkey primary key (id),
    constraint players_name_key unique (name),
    constraint players_user_id_key unique (user_id)
);

-- ─── locke_players ───────────────────────────────────────────────────────────────────────────────

create table pokemon.locke_players (
    locke_id  uuid not null,
    player_id uuid not null,
    constraint locke_players_pkey primary key (locke_id, player_id),
    constraint locke_players_locke_fkey foreign key (locke_id) references pokemon.lockes (id) on delete cascade,
    constraint locke_players_player_fkey foreign key (player_id) references pokemon.players (id)
);

create index locke_players_player_idx on pokemon.locke_players (player_id);

-- ─── tournaments ─────────────────────────────────────────────────────────────────────────────────

create table pokemon.tournaments (
    id              uuid         not null default gen_random_uuid(),
    locke_id        uuid         not null,
    game_id         uuid         not null,
    -- Order inside the randomlocke; the cumulative standings follow it.
    position        smallint     not null,
    name            varchar(100) not null,
    description     varchar(1000),
    format          varchar(20)  not null,
    status          varchar(20)  not null default 'draft',
    points_per_win  smallint     not null default 1,
    -- Until this moment players only see their own team picture.
    teams_reveal_at timestamptz,
    -- Size of the double-elimination bracket the app generated (4, 8, 16...). Null when there is no
    -- bracket yet, or for the imported 6-player brackets, which are kept as they were played.
    bracket_size    smallint,
    created_at      timestamptz  not null default now(),
    constraint tournaments_pkey primary key (id),
    constraint tournaments_locke_fkey foreign key (locke_id) references pokemon.lockes (id) on delete cascade,
    constraint tournaments_game_fkey foreign key (game_id) references pokemon.games (id),
    constraint tournaments_position_key unique (locke_id, position),
    constraint tournaments_format_check check (format in ('groups_bracket', 'round_robin')),
    constraint tournaments_status_check check (status in ('draft', 'in_progress', 'finished')),
    constraint tournaments_points_check check (points_per_win >= 0),
    constraint tournaments_bracket_size_check check (bracket_size in (4, 8, 16, 32))
);

create index tournaments_game_idx on pokemon.tournaments (game_id);

-- ─── tournament_players ──────────────────────────────────────────────────────────────────────────

create table pokemon.tournament_players (
    id                uuid         not null default gen_random_uuid(),
    tournament_id     uuid         not null,
    player_id         uuid         not null,
    -- A or B in a groups_bracket tournament; null in a league, or for the player that skips the groups.
    group_name        char(1),
    -- Object name in the private Storage bucket pokemon-teams; the backend turns it into a signed URL.
    team_image        varchar(200),
    points_adjustment smallint     not null default 0,
    constraint tournament_players_pkey primary key (id),
    constraint tournament_players_tournament_fkey foreign key (tournament_id) references pokemon.tournaments (id) on delete cascade,
    constraint tournament_players_player_fkey foreign key (player_id) references pokemon.players (id),
    constraint tournament_players_player_key unique (tournament_id, player_id),
    constraint tournament_players_group_check check (group_name in ('A', 'B'))
);

create index tournament_players_player_idx on pokemon.tournament_players (player_id);

-- ─── matches ─────────────────────────────────────────────────────────────────────────────────────

create table pokemon.matches (
    id            uuid        not null default gen_random_uuid(),
    tournament_id uuid        not null,
    -- group and league series; upper, lower and grand_final for the double-elimination bracket.
    stage         varchar(12) not null,
    round         smallint    not null,
    slot          smallint    not null,
    -- Null while the series is waiting for a previous result; a bracket series with a single
    -- player is a bye.
    player1_id    uuid,
    player2_id    uuid,
    created_at    timestamptz not null default now(),
    constraint matches_pkey primary key (id),
    constraint matches_tournament_fkey foreign key (tournament_id) references pokemon.tournaments (id) on delete cascade,
    constraint matches_player1_fkey foreign key (player1_id) references pokemon.tournament_players (id) on delete cascade,
    constraint matches_player2_fkey foreign key (player2_id) references pokemon.tournament_players (id) on delete cascade,
    constraint matches_place_key unique (tournament_id, stage, round, slot),
    constraint matches_stage_check check (stage in ('group', 'league', 'upper', 'lower', 'grand_final')),
    constraint matches_round_check check (round >= 1 and slot >= 1),
    constraint matches_players_check check (player1_id is null or player2_id is null or player1_id <> player2_id)
);

create index matches_player1_idx on pokemon.matches (player1_id);
create index matches_player2_idx on pokemon.matches (player2_id);

-- ─── match_games ─────────────────────────────────────────────────────────────────────────────────

create table pokemon.match_games (
    id          uuid         not null default gen_random_uuid(),
    match_id    uuid         not null,
    game_number smallint     not null,
    -- 1 or 2: which side of the series won the game.
    winner_side smallint     not null,
    replay_url  varchar(500),
    played_at   timestamptz  not null default now(),
    constraint match_games_pkey primary key (id),
    constraint match_games_match_fkey foreign key (match_id) references pokemon.matches (id) on delete cascade,
    constraint match_games_number_key unique (match_id, game_number),
    constraint match_games_number_check check (game_number between 1 and 3),
    constraint match_games_winner_check check (winner_side in (1, 2))
);

-- ─── rewards ─────────────────────────────────────────────────────────────────────────────────────

create table pokemon.rewards (
    id            uuid         not null default gen_random_uuid(),
    tournament_id uuid         not null,
    sort_order    smallint     not null,
    name          varchar(150) not null,
    -- Player who picked it; players pick in the order of the final standings of the tournament.
    chosen_by     uuid,
    constraint rewards_pkey primary key (id),
    constraint rewards_tournament_fkey foreign key (tournament_id) references pokemon.tournaments (id) on delete cascade,
    constraint rewards_chosen_by_fkey foreign key (chosen_by) references pokemon.tournament_players (id) on delete set null,
    constraint rewards_order_key unique (tournament_id, sort_order),
    constraint rewards_chosen_by_key unique (chosen_by)
);

-- ─── Row level security ──────────────────────────────────────────────────────────────────────────
-- The backend connects as the table owner, which bypasses RLS. Enabling it without policies closes
-- the tables to the Supabase Data API (anon and authenticated roles) in case the schema is ever exposed.

alter table pokemon.games              enable row level security;
alter table pokemon.lockes             enable row level security;
alter table pokemon.players            enable row level security;
alter table pokemon.locke_players      enable row level security;
alter table pokemon.tournaments        enable row level security;
alter table pokemon.tournament_players enable row level security;
alter table pokemon.matches            enable row level security;
alter table pokemon.match_games        enable row level security;
alter table pokemon.rewards            enable row level security;

commit;
