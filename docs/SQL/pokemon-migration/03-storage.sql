-- =====================================================================================================
-- Pokémon module · 03 · Storage for team pictures
--
-- Private bucket pokemon-teams. Only the backend reads and writes it, with the service role key (which
-- bypasses RLS). Browsers never talk to Storage: they receive short-lived signed URLs from the API.
--
-- Two barriers besides the backend checks:
--   - The bucket itself refuses files over 1 MB and anything that is not WebP, JPEG or PNG.
--   - A RESTRICTIVE policy denies the anon and authenticated roles any access to this bucket. Because it
--     is restrictive, a permissive policy added later by mistake cannot open it.
--
-- Object names: tournaments/<tournament id>/<tournament player id>-<random>.webp
-- =====================================================================================================

begin;

insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values ('pokemon-teams', 'pokemon-teams', false, 1048576, array['image/webp', 'image/jpeg', 'image/png'])
on conflict (id) do update
    set public             = false,
        file_size_limit    = excluded.file_size_limit,
        allowed_mime_types = excluded.allowed_mime_types;

drop policy if exists "pokemon-teams: no client access" on storage.objects;

create policy "pokemon-teams: no client access"
    on storage.objects
    as restrictive
    for all
    to anon, authenticated
    using (bucket_id <> 'pokemon-teams')
    with check (bucket_id <> 'pokemon-teams');

commit;
