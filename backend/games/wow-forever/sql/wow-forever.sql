-- =====================================================================================================
-- WoW Forever module · Storage
--
-- WoW Forever has no tables. Its game data is built by the backend from the game client (through
-- wago.tools) every time a new build comes out, and the last good copy is kept in a private bucket so a
-- restart does not need to build it again.
--
-- Run once in the Supabase SQL editor. Safe to run again.
-- =====================================================================================================

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
