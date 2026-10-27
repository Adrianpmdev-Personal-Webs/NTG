-- =====================================================================================================
-- Schema core · platform
--
-- Users of the platform, the tags that unlock each game and the join table between them. Every user
-- shares its id with its Supabase Auth account; passwords live only in Supabase Auth.
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
