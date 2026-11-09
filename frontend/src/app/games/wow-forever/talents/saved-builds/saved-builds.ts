/**
 * SavedBuilds
 *
 * Slots of the talent calculator where a player keeps builds of the selected class on their account: save the current
 * build under a chosen name, load it back, rename it or delete it. A build that no longer fits the talents of the class after a game patch
 * is marked and cannot be loaded, but its slot can still be overwritten or deleted. A build kept without the order of
 * its points is marked too, so the player knows to make it again.
 *
 * @author @adrianpmdev
 */

import {
    ChangeDetectionStrategy,
    Component,
    ElementRef,
    computed,
    effect,
    inject,
    input,
    linkedSignal,
    output,
    signal,
    viewChild,
} from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { EMPTY, catchError, switchMap, tap } from 'rxjs';
import { NotificationCenter } from '@core/notifications/notification-center';
import { PlayerProgressApi } from '../../progress/player-progress-api';
import { SAVED_BUILD_NAME_MAX_LENGTH, SLOTS_PER_CLASS, SavedBuild, SavedBuildSlots } from '../saved-builds-models';
import { decodeBuild, pointsInTree, totalPoints } from '../talents-logic';
import { TalentTree } from '../talents-models';

type SlotAction = 'save' | 'delete';

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
    private readonly api = inject(PlayerProgressApi);
    private readonly notificationCenter = inject(NotificationCenter);
    private readonly saveNameField = viewChild<ElementRef<HTMLInputElement>>('saveName');

    readonly classSlug = input.required<string>();
    readonly trees = input.required<readonly TalentTree[]>();
    readonly build = input.required<string | null>();
    readonly order = input.required<string | null>();
    readonly level = input.required<number>();
    readonly loadBuild = output<SavedBuild>();

    protected readonly nameMaxLength = SAVED_BUILD_NAME_MAX_LENGTH;

    protected readonly loading = signal(true);
    protected readonly failed = signal(false);

    private readonly slots = linkedSignal<string, SavedBuildSlots>({
        source: this.classSlug,
        computation: () => Array.from({ length: SLOTS_PER_CLASS }, () => null),
    });

    protected readonly pending = linkedSignal<string, PendingAction | null>({
        source: this.classSlug,
        computation: () => null,
    });

    protected readonly rows = computed(() =>
        this.slots().map((slot, index) => ({
            index,
            slot,
            distribution: slot ? this.distributionOf(slot) : null,
            current: slot !== null && slot.build === this.build() && slot.order === this.order(),
        })),
    );

    constructor() {
        toObservable(this.classSlug)
            .pipe(
                tap(() => {
                    this.loading.set(true);
                    this.failed.set(false);
                }),
                switchMap((classSlug) =>
                    this.api.loadBuilds(classSlug).pipe(
                        catchError(() => {
                            this.failed.set(true);
                            this.loading.set(false);
                            return EMPTY;
                        }),
                    ),
                ),
                takeUntilDestroyed(),
            )
            .subscribe((slots) => {
                this.slots.set(slots);
                this.loading.set(false);
            });

        effect(() => {
            const field = this.saveNameField()?.nativeElement;

            field?.focus();
            field?.select();
        });
    }

    protected requestSave(index: number): void {
        if (this.build()) {
            this.pending.set({ index, action: 'save' });
        }
    }

    protected confirmSave(index: number, value: string): void {
        const build = this.build();

        if (!build) {
            return;
        }

        const name = this.cleanName(index, value);

        this.pending.set(null);
        this.store(index, { name, level: this.level(), build, order: this.order() }, () =>
            this.notificationCenter.success(`Build guardada: ${name}`),
        );
    }

    protected requestDelete(index: number): void {
        this.pending.set({ index, action: 'delete' });
    }

    protected confirmDelete(index: number): void {
        this.pending.set(null);
        this.store(index, null);
    }

    protected rename(index: number, field: HTMLInputElement): void {
        const slot = this.slots()[index];

        if (!slot) {
            return;
        }

        const name = this.cleanName(index, field.value);

        field.value = name;
        this.store(index, { ...slot, name }, undefined, () => (field.value = slot.name));
    }

    protected defaultName(index: number): string {
        return `Build ${index + 1}`;
    }

    private cleanName(index: number, value: string): string {
        return value.trim().slice(0, SAVED_BUILD_NAME_MAX_LENGTH) || this.defaultName(index);
    }

    private distributionOf(slot: SavedBuild): string | null {
        const trees = this.trees();
        const ranks = decodeBuild(trees, slot.build);

        return totalPoints(trees, ranks) > 0 ? trees.map((tree) => pointsInTree(tree, ranks)).join(' / ') : null;
    }

    private store(index: number, slot: SavedBuild | null, saved?: () => void, failed?: () => void): void {
        const classSlug = this.classSlug();
        const request = slot ? this.api.saveBuild(classSlug, index, slot) : this.api.deleteBuild(classSlug, index);

        request.subscribe({
            next: (slots) => {
                if (this.classSlug() === classSlug) {
                    this.slots.set(slots);
                }

                saved?.();
            },
            error: () => {
                failed?.();
                this.notificationCenter.error('No se ha podido guardar en tu cuenta');
            },
        });
    }
}
