-- =====================================================================================================
-- Schema core · platform
--
-- Users of the platform, the tags that unlock each game and the join table between them. Every user
-- shares its id with its Supabase Auth account; passwords live only in Supabase Auth. Also the catalog
-- of games, the public pictures uploaded by admins and their public Storage bucket.
--
-- How to use: run it in the Supabase SQL editor of an empty project. Order of the files:
-- core.sql, pokemon.sql, wow_forever.sql (the game schemas do not depend on core).
-- =====================================================================================================

create schema if not exists core;

-- ─── user ────────────────────────────────────────────────────────────────────────────────────────
-- Quoted because user is a reserved word. role is 'usuario' or 'admin'.

create table core."user" (
    id         uuid      not null,
    username   text      not null unique check (length(username) <= 50),
    avatar_url text      check (length(avatar_url) <= 500),
    created_at timestamp default now(),
    role       text      not null default 'usuario' check (length(role) <= 30),
    email      varchar   not null,
    constraint user_pkey primary key (id)
);

-- ─── tag ─────────────────────────────────────────────────────────────────────────────────────────
-- The key of a game's tag is the game id (pokemon, wow-forever...) and becomes the authority TAG_<KEY>.

create table core.tag (
    key         varchar     not null check (key ~ '^[a-z0-9][a-z0-9_-]{1,29}$'),
    name        varchar     not null,
    description varchar,
    created_at  timestamptz not null default now(),
    constraint tag_pkey primary key (key)
);

-- ─── user_tag ────────────────────────────────────────────────────────────────────────────────────

create table core.user_tag (
    user_id     uuid        not null,
    tag_key     varchar     not null,
    assigned_at timestamptz not null default now(),
    constraint user_tag_pkey primary key (user_id, tag_key),
    constraint user_tag_user_id_fkey foreign key (user_id) references core."user" (id),
    constraint user_tag_tag_key_fkey foreign key (tag_key) references core.tag (key)
);

-- ─── game ────────────────────────────────────────────────────────────────────────────────────────
-- Catalog of the games shown by the web clients. The key is the game id, the key of the tag that
-- unlocks it and its URL segment. An upcoming game is listed as "coming soon" and only admins can
-- open it: the backend ignores its tag for everybody else. The cover is the public picture with key
-- platform/games/<key> (see asset).

create table if not exists core.game (
    key        varchar     not null check (key ~ '^[a-z0-9][a-z0-9_-]{1,29}$'),
    name       varchar(60) not null,
    upcoming   boolean     not null default true,
    sort_order smallint    not null default 0,
    created_at timestamptz not null default now(),
    constraint game_pkey primary key (key)
);

insert into core.game (key, name, upcoming, sort_order) values
    ('pokemon',     'Pokemon',           false, 1),
    ('wow-forever', 'WoW Forever',       false, 2),
    ('lol',         'League of Legends', true,  3),
    ('cs2',         'Counter Strike 2',  true,  4)
on conflict (key) do nothing;

-- ─── asset ───────────────────────────────────────────────────────────────────────────────────────
-- Public pictures uploaded by admins from the web (game covers, WoW talent trees...). The backend
-- compresses them and stores them in the public bucket public-assets; this table maps each key to its
-- object. The URL carries ?v=<updated_at> so it changes every time the picture is replaced.

create table if not exists core.asset (
    key          varchar(150) not null check (key ~ '^[a-z0-9][a-z0-9_-]*(/[a-z0-9][a-z0-9_-]*)*$'),
    object_name  varchar(200) not null,
    content_type varchar(30)  not null,
    size_bytes   integer      not null,
    width        integer      not null,
    height       integer      not null,
    updated_at   timestamptz  not null default now(),
    constraint asset_pkey primary key (key)
);

-- ─── Row level security ──────────────────────────────────────────────────────────────────────────
-- The backend connects as the table owner, which bypasses RLS. Enabling it without policies closes
-- the tables to the Supabase Data API (anon and authenticated roles).

alter table core.game  enable row level security;
alter table core.asset enable row level security;

-- ─── Storage: public pictures ────────────────────────────────────────────────────────────────────
-- Public bucket public-assets: anybody can download a picture through its public URL (they are not
-- private: covers, backgrounds...), but only the backend writes, with the service role key. A
-- RESTRICTIVE policy denies the anon and authenticated roles any operation through the Storage API
-- (upload, replace, delete, list). The bucket refuses files over 2 MB and anything but JPEG and PNG.
-- Object names: <key>.<jpg|png>, for example platform/games/pokemon.jpg
--
-- If the policy fails with "must be owner of table objects", create both from the Storage dashboard:
-- public bucket public-assets (2 MB, image/jpeg, image/png) and a restrictive policy on objects for all
-- operations, roles anon and authenticated, expression bucket_id <> 'public-assets'.

insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values ('public-assets', 'public-assets', true, 2097152, array['image/jpeg', 'image/png'])
on conflict (id) do update
    set public             = true,
        file_size_limit    = excluded.file_size_limit,
        allowed_mime_types = excluded.allowed_mime_types;

drop policy if exists "public-assets: no client writes" on storage.objects;

create policy "public-assets: no client writes"
    on storage.objects
    as restrictive
    for all
    to anon, authenticated
    using (bucket_id <> 'public-assets')
    with check (bucket_id <> 'public-assets');
