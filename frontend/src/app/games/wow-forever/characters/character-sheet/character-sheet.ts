/**
 * CharacterSheet
 *
 * The armory of one character inside the web: its picture on the background of its race, the equipped items around it
 * with their tooltips, and below its statistics, talents and professions. Everything comes from the official armory
 * through the backend, in English; a character the armory does not show says so.
 *
 * @author @adrianpmdev
 */

import { NgTemplateOutlet } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { WowButton } from '../../ui/wow-button/wow-button';
import { WowNavLink } from '../../ui/wow-nav-menu/wow-nav-menu';
import { WowPage } from '../../ui/wow-page/wow-page';
import { hideBrokenIcon } from '../../wow-forever-config';
import { CharactersApi } from '../characters-api';
import { ARMORY_CREDIT, ARMORY_FAILED, NOT_IN_ARMORY } from '../characters-texts';
import { ArmoryItem, CharacterSheet as SheetData } from '../characters-models';

interface Slot {
    slot: string;
    label: string;
}

const LEFT: readonly Slot[] = [
    { slot: 'HEAD', label: 'Head' },
    { slot: 'NECK', label: 'Neck' },
    { slot: 'SHOULDER', label: 'Shoulder' },
    { slot: 'BACK', label: 'Back' },
    { slot: 'CHEST', label: 'Chest' },
    { slot: 'SHIRT', label: 'Shirt' },
    { slot: 'TABARD', label: 'Tabard' },
    { slot: 'WRIST', label: 'Wrist' },
];

const RIGHT: readonly Slot[] = [
    { slot: 'HANDS', label: 'Hands' },
    { slot: 'WAIST', label: 'Waist' },
    { slot: 'LEGS', label: 'Legs' },
    { slot: 'FEET', label: 'Feet' },
    { slot: 'FINGER_1', label: 'Ring 1' },
    { slot: 'FINGER_2', label: 'Ring 2' },
    { slot: 'TRINKET_1', label: 'Trinket 1' },
    { slot: 'TRINKET_2', label: 'Trinket 2' },
];

const BOTTOM: readonly Slot[] = [
    { slot: 'MAIN_HAND', label: 'Main Hand' },
    { slot: 'OFF_HAND', label: 'Off Hand' },
    { slot: 'RANGED', label: 'Ranged' },
];

@Component({
    selector: 'app-character-sheet',
    imports: [NgTemplateOutlet, WowButton, WowPage],
    templateUrl: './character-sheet.html',
    styleUrl: './character-sheet.scss',
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CharacterSheet implements OnInit {
    private readonly api = inject(CharactersApi);
    private readonly route = inject(ActivatedRoute);

    protected readonly hideBrokenIcon = hideBrokenIcon;
    protected readonly credit = ARMORY_CREDIT;
    protected readonly notInArmory = NOT_IN_ARMORY;
    protected readonly armoryFailed = ARMORY_FAILED;
    protected readonly left = LEFT;
    protected readonly right = RIGHT;
    protected readonly bottom = BOTTOM;
    protected readonly menuLinks: readonly WowNavLink[] = [{ label: 'Personajes', link: '/wow-forever/characters' }];

    protected readonly loading = signal(true);
    protected readonly failed = signal(false);
    protected readonly data = signal<SheetData | null>(null);

    protected readonly sheet = computed(() => this.data()?.sheet ?? null);
    protected readonly heading = computed(() => this.data()?.name ?? 'Personaje');

    private readonly gearBySlot = computed(() => {
        const bySlot = new Map<string, ArmoryItem>();

        this.sheet()?.gear.forEach((item) => bySlot.set(item.slot, item));

        return bySlot;
    });

    ngOnInit(): void {
        this.api.sheet(this.route.snapshot.paramMap.get('id') ?? '').subscribe({
            next: (data) => {
                this.data.set(data);
                this.loading.set(false);
            },
            error: () => {
                this.failed.set(true);
                this.loading.set(false);
            },
        });
    }

    protected itemIn(slot: string): ArmoryItem | null {
        return this.gearBySlot().get(slot) ?? null;
    }

    protected percent(value: number, max: number): number {
        return max > 0 ? Math.min(100, Math.round((value / max) * 100)) : 0;
    }
}
