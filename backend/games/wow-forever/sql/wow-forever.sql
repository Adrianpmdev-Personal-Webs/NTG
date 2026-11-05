-- =====================================================================================================
-- Schema wow_forever · WoW Forever module
--
-- The tables hold what belongs to each player: their characters, their talent builds and their Legacy
-- challenges. The game data is not in the database: it is built
-- by the backend from the game client (through wago.tools) every time a new build comes out, and the last
-- good copy is kept in a private bucket so a restart does not need to build it again.
--
-- How to use: run it in the Supabase SQL editor, after core.sql; it can be run again on an existing
-- project, it only adds what is missing and keeps the data. A new column of an existing table needs its
-- own "alter table ... add column if not exists".
-- =====================================================================================================

create schema if not exists wow_forever;

-- ─── characters ──────────────────────────────────────────────────────────────────────────────────────
-- The class, the race and the professions are the slugs of the game data. The backend fills them, and the level, from
-- the official armory; they stay empty until the armory shows the character.

create table if not exists wow_forever.characters (
    id          uuid        not null default gen_random_uuid(),
    -- Platform account (core.user.id). A plain column, like every user reference in game schemas.
    user_id     uuid        not null,
    name        varchar(40) not null,
    class_slug  varchar(40),
    race_slug   varchar(40),
    level       smallint,
    -- [{ "slug": "alchemy", "skill": 300, "maxSkill": 300 }]; the skills can be null.
    professions jsonb       not null default '[]',
    notes       varchar(200),
    armory_slug varchar(80),
    armory_updated_at timestamptz,
    created_at  timestamptz not null default now(),
    constraint characters_pkey primary key (id),
    constraint characters_level_check check (level >= 1)
);

-- Names now include a surname; this widens the column of a table created before.
alter table wow_forever.characters alter column name type varchar(40);

-- Characters are now filled in from the armory; this adapts a table created before.
alter table wow_forever.characters alter column class_slug drop not null;
alter table wow_forever.characters alter column race_slug drop not null;
alter table wow_forever.characters alter column level drop not null;
alter table wow_forever.characters add column if not exists armory_slug varchar(80);
alter table wow_forever.characters add column if not exists armory_updated_at timestamptz;

create unique index if not exists characters_user_name_key on wow_forever.characters (user_id, lower(name));

-- ─── saved_builds ────────────────────────────────────────────────────────────────────────────────────
-- The talent builds a player keeps, five per class. The class is a slug of the game data and the build a share
-- code, which a game patch can leave out of date: the slot stays and the screen marks it.

create table if not exists wow_forever.saved_builds (
    user_id    uuid        not null,
    class_slug varchar(40) not null,
    slot       smallint    not null,
    name       varchar(30) not null,
    level      smallint    not null,
    build      varchar(300) not null,
    updated_at timestamptz not null default now(),
    constraint saved_builds_pkey primary key (user_id, class_slug, slot),
    constraint saved_builds_slot_check check (slot between 0 and 4)
);

-- ─── legacy_challenges ───────────────────────────────────────────────────────────────────────────────
-- The Legacy challenges a player ticked, by name, because the game client gives them no stable id.

create table if not exists wow_forever.legacy_challenges (
    user_id      uuid         not null,
    name         varchar(200) not null,
    completed_at timestamptz  not null default now(),
    constraint legacy_challenges_pkey primary key (user_id, name)
);

-- ─── Row level security ──────────────────────────────────────────────────────────────────────────────
-- The backend connects as the table owner, which bypasses RLS. Enabling it without policies closes the
-- tables to the Supabase Data API (anon and authenticated roles) in case the schema is ever exposed.

alter table wow_forever.characters enable row level security;
alter table wow_forever.saved_builds enable row level security;
alter table wow_forever.legacy_challenges enable row level security;

-- ─── Storage: game data ──────────────────────────────────────────────────────────────────────────────
-- Private bucket wow-forever-data. Only the backend reads and writes it, with the service role key (which
-- bypasses RLS); browsers never download from it. It holds one object, data.json.gz (about 200 KB). The
-- bucket refuses files over 5 MB and anything that is not gzip, and a RESTRICTIVE policy denies the anon
-- and authenticated roles any access to it, so a permissive policy added later by mistake cannot open it.
--
-- If the policy fails with "must be owner of table objects", create both from the Storage dashboard:
-- private bucket wow-forever-data (5 MB, application/gzip) and a restrictive policy on objects for all
-- operations, roles anon and authenticated, expression bucket_id <> 'wow-forever-data'.

insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values ('wow-forever-data', 'wow-forever-data', false, 5242880, array['application/gzip'])
on conflict (id) do update
    set public             = false,
        file_size_limit    = excluded.file_size_limit,
        allowed_mime_types = excluded.allowed_mime_types;

drop policy if exists "wow-forever-data: no client access" on storage.objects;

create policy "wow-forever-data: no client access"
    on storage.objects
    as restrictive
    for all
    to anon, authenticated
    using (bucket_id <> 'wow-forever-data')
    with check (bucket_id <> 'wow-forever-data');
