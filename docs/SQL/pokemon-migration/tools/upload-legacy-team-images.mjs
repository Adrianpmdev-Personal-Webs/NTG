/**
 * upload-legacy-team-images
 *
 * One-off upload of the team pictures of the old database to the private bucket pokemon-teams.
 * The pictures must already be compressed to WebP and laid out by object name (see README.md).
 *
 * Usage (PowerShell):
 *   $env:SUPABASE_URL = 'https://<project>.supabase.co'
 *   $env:SUPABASE_SERVICE_ROLE_KEY = '<service role key>'
 *   node upload-legacy-team-images.mjs ../legacy-team-images.json <folder with the WebP files>
 *
 * The service role key never leaves this machine and is not written anywhere. Uploads use upsert, so
 * the script can be run again safely.
 *
 * @author @adrianpmdev
 */

import { readFileSync } from 'node:fs';
import { join } from 'node:path';

const BUCKET = 'pokemon-teams';
const [manifestPath, folder] = process.argv.slice(2);
const { SUPABASE_URL, SUPABASE_SERVICE_ROLE_KEY } = process.env;

if (!manifestPath || !folder || !SUPABASE_URL || !SUPABASE_SERVICE_ROLE_KEY) {
    console.error('Usage: SUPABASE_URL=... SUPABASE_SERVICE_ROLE_KEY=... node upload-legacy-team-images.mjs <manifest> <webp folder>');
    process.exit(1);
}

const objectNames = Object.values(JSON.parse(readFileSync(manifestPath, 'utf8')));
let failed = 0;

for (const objectName of objectNames) {
    const response = await fetch(`${SUPABASE_URL}/storage/v1/object/${BUCKET}/${objectName}`, {
        method: 'POST',
        headers: {
            Authorization: `Bearer ${SUPABASE_SERVICE_ROLE_KEY}`,
            apikey: SUPABASE_SERVICE_ROLE_KEY,
            'Content-Type': 'image/webp',
            'x-upsert': 'true'
        },
        body: readFileSync(join(folder, objectName))
    });

    if (response.ok) {
        console.log(`ok    ${objectName}`);
    } else {
        failed++;
        console.error(`error ${objectName}: ${response.status} ${await response.text()}`);
    }
}

console.log(`${objectNames.length - failed} of ${objectNames.length} uploaded`);
process.exit(failed > 0 ? 1 : 0);
