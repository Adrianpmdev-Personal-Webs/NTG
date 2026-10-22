/**
 * WowForeverTheme
 *
 * Selected faction of the WoW Forever area. It drives the background and is remembered between visits.
 *
 * @author @adrianpmdev
 */

import { Injectable, signal } from '@angular/core';
import { Faction } from './wow-forever-models';

const STORAGE_KEY = 'wow-forever-faction';

@Injectable({ providedIn: 'root' })
export class WowForeverTheme {

    readonly faction = signal<Faction>(this.readStoredFaction());

    setFaction(faction: Faction): void {
        this.faction.set(faction);

        try {
            localStorage.setItem(STORAGE_KEY, faction);
        } catch {
            // Storage can be unavailable (private mode): the choice just is not remembered.
        }
    }

    private readStoredFaction(): Faction {
        try {
            return localStorage.getItem(STORAGE_KEY) === 'HORDE' ? 'HORDE' : 'ALLIANCE';
        } catch {
            return 'ALLIANCE';
        }
    }
}
