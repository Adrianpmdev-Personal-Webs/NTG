/**
 * Home
 *
 * Landing page of WoW Forever: faction switch, the tools of the area and the classes and races stored in the database.
 *
 * @author @adrianpmdev
 */

import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { FACTION_LABELS, WOW_FEATURES, hideBrokenIcon, wowIconUrl } from '../wow-forever-config';
import { WowForeverApi } from '../wow-forever-api';
import { WowForeverTheme } from '../wow-forever-theme';
import { Faction, Race, WowClass } from '../wow-forever-models';

@Component({
    selector: 'app-wow-forever-home',
    imports: [RouterLink],
    templateUrl: './home.html',
    styleUrl: './home.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class Home implements OnInit {

    private readonly api = inject(WowForeverApi);
    private readonly theme = inject(WowForeverTheme);

    readonly features = WOW_FEATURES;
    readonly factions: readonly Faction[] = ['ALLIANCE', 'HORDE'];
    readonly factionLabels = FACTION_LABELS;
    readonly faction = this.theme.faction;

    readonly classes = signal<WowClass[]>([]);
    readonly races = signal<Race[]>([]);
    readonly loading = signal(true);
    readonly failed = signal(false);

    readonly allianceRaces = computed(() => this.races().filter(race => race.faction === 'ALLIANCE'));
    readonly hordeRaces = computed(() => this.races().filter(race => race.faction === 'HORDE'));

    readonly iconUrl = wowIconUrl;
    readonly onIconError = hideBrokenIcon;

    ngOnInit(): void {
        forkJoin({ classes: this.api.getClasses(), races: this.api.getRaces() }).subscribe({
            next: ({ classes, races }) => {
                this.classes.set(classes);
                this.races.set(races);
                this.loading.set(false);
            },
            error: () => {
                this.failed.set(true);
                this.loading.set(false);
            }
        });
    }

    setFaction(faction: Faction): void {
        this.theme.setFaction(faction);
    }

    racesOf(faction: Faction): Race[] {
        return faction === 'ALLIANCE' ? this.allianceRaces() : this.hordeRaces();
    }
}
