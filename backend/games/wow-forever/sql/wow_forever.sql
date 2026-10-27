-- =====================================================================================================
-- Schema wow_forever · WoW Forever module
--
-- Only the module settings live in the database. The game data (classes, races, talents, spellbooks
-- and the Legacy System) is the file backend/wow-forever/src/main/resources/wow-forever/data.json.
-- The game id wow-forever has a hyphen, so the schema uses an underscore.
--
-- How to use: run it in the Supabase SQL editor of an empty project, after core.sql and pokemon.sql.
-- =====================================================================================================

create schema if not exists wow_forever;

-- ─── setting ─────────────────────────────────────────────────────────────────────────────────────
-- Key/value settings. Keys are created here, with SQL; the admin screen only edits values.

create table wow_forever.setting (
    key         varchar     not null,
    value       varchar     not null,
    description varchar,
    updated_at  timestamptz not null default now(),
    constraint setting_pkey primary key (key)
);
