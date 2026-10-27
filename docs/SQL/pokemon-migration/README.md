# Pokémon module · migration to the new model

Moves the Pokémon module from the 13 tables of its first version to 9 tables, and loads the data of the
old Pokémon app (randomlocke GenLocke 2025: 6 tournaments, 72 series, 165 games, 36 rewards and 31 team
pictures). The current `pokemon.games` rows are kept.

## Files

| File | What it does |
|---|---|
| `01-schema.sql` | Drops the first version (except `games`) and creates the new tables |
| `02-legacy-data.sql` | Inserts the old data. Generated locally, idempotent |
| `03-storage.sql` | Creates the private bucket `pokemon-teams` and its restrictive policy |
| `04-verify.sql` | Compares the cumulative points with the old database. Generated. Empty result = OK |
| `legacy-team-images.json` | Old picture file name → new object name. Generated |
| `tools/generate-legacy-data.mjs` | Builds the three generated files from the CSV export |
| `tools/upload-legacy-team-images.mjs` | Uploads the compressed pictures to the bucket |

The generated files hold player names, points and replays, so they are git-ignored: this repository is
public. Generate them locally (see Regenerating) before running the migration.

## Order

1. Deploy nothing yet: the old backend stops working as soon as `01-schema.sql` runs.
2. In the Supabase SQL editor run `01-schema.sql`, `02-legacy-data.sql` and `03-storage.sql`.
3. Run `04-verify.sql`: it must return no rows.
4. Upload the pictures (see below).
5. Deploy the new backend and frontend.
6. In the admin area, link each player to their platform account, so they can upload their own team.

If `03-storage.sql` fails with "must be owner of table objects", create the bucket from the Storage
dashboard with the same settings (private, 1 MB, WebP/JPEG/PNG) and add the policy from the dashboard's
policy editor: restrictive, all operations, roles `anon` and `authenticated`, expression
`bucket_id <> 'pokemon-teams'`.

## How the old data was transformed

- The three "GenLocke 2025" modalities (FireRed, HeartGold, Platino) become one randomlocke, because
  their points carry over from game to game. Each tournament records the game it was played in.
- Each old row was one game with the running score of its series. Rows are grouped into best-of-3
  series. Three rows skipped games (rows 8 and 11 of tournament "Torneo 2 - LvL 43", row 273 of
  "Torneo Final - LvL 50" in HeartGold); the missing games are added without replay.
- The old bracket was a 6-player double elimination. Its codes map to stage, round and slot:
  `semis1`/`semis2` upper 1, `upperfinal` upper 2, `lb1`..`lb3` lower 1..3, `lowerfinal` lower 4,
  `final` grand final.
- Old points were cumulative and, in bracket tournaments, given by final placement. The new model counts
  series wins × points per win, so `points_adjustment` keeps the difference. The first FireRed
  tournament ("Torneo 1") was never recorded; its points live in the adjustment of "Torneo 2".
- Known gaps kept as they are: one series of "Torneo 2 - LvL 43" stops at 1-0 in the
  data, and two games of "Torneo Final - LvL 50" in HeartGold reuse a replay of another tournament.
- The Platino tournament is in progress: its six group series are pending and its rewards unpicked.

## Regenerating

```bash
node tools/generate-legacy-data.mjs "<old csv folder>" "<current csv folder>" .
```

The CSV folders are the export of the old database (one file per table, plus `batallas_faltantes.csv`
with the rows cut by the 100-row export limit) and `games.csv` of the current one. They are not kept in
the repository.

## Team pictures

The 31 old pictures were compressed to WebP (longest side 1600 px, quality 80, the same limits the app
applies in the browser), from 6.0 MB to 2.0 MB, and saved under their new object names. To upload them:

```powershell
$env:SUPABASE_URL = 'https://<project>.supabase.co'
$env:SUPABASE_SERVICE_ROLE_KEY = '<service role key>'
node tools/upload-legacy-team-images.mjs legacy-team-images.json "<folder with the WebP files>"
```
