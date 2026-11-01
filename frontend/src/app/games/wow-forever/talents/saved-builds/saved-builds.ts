/**
 * SavedBuilds
 *
 * Slots of the talent calculator where a player keeps builds of the selected class in this browser: save the current
 * build, load it back, rename it or delete it. A build that no longer fits the talents of the class after a game patch
 * is marked and cannot be loaded, but its slot can still be overwritten or deleted.
 *
 * @author @adrianpmdev
 */

import { ChangeDetectionStrategy, Component, computed, inject, input, linkedSignal, output } from '@angular/core';
import { NotificationCenter } from '@core/notifications/notification-center';
import {
    SAVED_BUILD_NAME_MAX_LENGTH,
    SavedBuild,
    SavedBuildSlots,
    readSavedBuilds,
    writeSavedBuilds,
} from '../saved-builds-storage';
import { decodeBuild, pointsInTree, totalPoints } from '../talents-logic';
import { TalentTree } from '../talents-models';

type SlotAction = 'overwrite' | 'delete';

interface PendingAction {
    index: number;
    action: SlotAction;
}

@Component({
    selector: 'app-saved-builds',
    templateUrl: './saved-builds.html',
    styleUrl: './saved-builds.scss',
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SavedBuilds {
    private readonly notificationCenter = inject(NotificationCenter);

    readonly classSlug = input.required<string>();
    readonly trees = input.required<readonly TalentTree[]>();
    readonly build = input.required<string | null>();
    readonly level = input.required<number>();
    readonly loadBuild = output<SavedBuild>();

    protected readonly nameMaxLength = SAVED_BUILD_NAME_MAX_LENGTH;

    private readonly slots = linkedSignal<SavedBuildSlots>(() => readSavedBuilds(this.classSlug()));

    protected readonly pending = linkedSignal<string, PendingAction | null>({
        source: this.classSlug,
        computation: () => null,
    });

    protected readonly rows = computed(() =>
        this.slots().map((slot, index) => ({
            index,
            slot,
            distribution: slot ? this.distributionOf(slot) : null,
            current: slot !== null && slot.build === this.build(),
        })),
    );

    protected save(index: number): void {
        const build = this.build();

        if (!build) {
            return;
        }

        const name = this.slots()[index]?.name ?? this.defaultName(index);

        if (this.store(index, { name, level: this.level(), build })) {
            this.notificationCenter.success(`Build guardada en ${name}`);
        }
    }

    protected requestOverwrite(index: number): void {
        if (this.build()) {
            this.pending.set({ index, action: 'overwrite' });
        }
    }

    protected requestDelete(index: number): void {
        this.pending.set({ index, action: 'delete' });
    }

    protected confirmPending(): void {
        const pending = this.pending();

        this.pending.set(null);

        if (pending?.action === 'overwrite') {
            this.save(pending.index);
        } else if (pending?.action === 'delete') {
            this.store(pending.index, null);
        }
    }

    protected rename(index: number, field: HTMLInputElement): void {
        const slot = this.slots()[index];

        if (!slot) {
            return;
        }

        const name = field.value.trim().slice(0, SAVED_BUILD_NAME_MAX_LENGTH) || this.defaultName(index);

        field.value = name;
        this.store(index, { ...slot, name });
    }

    private defaultName(index: number): string {
        return `Build ${index + 1}`;
    }

    private distributionOf(slot: SavedBuild): string | null {
        const trees = this.trees();
        const ranks = decodeBuild(trees, slot.build);

        return totalPoints(trees, ranks) > 0 ? trees.map((tree) => pointsInTree(tree, ranks)).join(' / ') : null;
    }

    private store(index: number, slot: SavedBuild | null): boolean {
        const slots = this.slots().map((current, position) => (position === index ? slot : current));

        if (!writeSavedBuilds(this.classSlug(), slots)) {
            this.notificationCenter.error('No se ha podido guardar en este navegador');
            return false;
        }

        this.slots.set(slots);
        return true;
    }
}
