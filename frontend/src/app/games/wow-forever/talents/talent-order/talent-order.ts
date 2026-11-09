/**
 * TalentOrder
 *
 * Panel of the talent calculator with the order the points were spent in: one line per point with the level it is
 * taken at, so the build can be followed while levelling. A build opened without its order (saved or shared before the
 * order existed) shows how to record it instead of guessing one. The panel folds, and the browser remembers it.
 *
 * @author @adrianpmdev
 */

import { ChangeDetectionStrategy, Component, computed, input, signal } from '@angular/core';
import { hideBrokenIcon } from '../../wow-forever-config';
import { PointOrder } from '../talents-logic';
import { Talent, TalentTree } from '../talents-models';

interface OrderStep {
    level: number;
    talent: Talent;
    rank: number;
}

const OPEN_KEY = 'ntg.wowForever.talentOrderOpen';

@Component({
    selector: 'app-talent-order',
    templateUrl: './talent-order.html',
    styleUrl: './talent-order.scss',
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TalentOrder {
    readonly trees = input.required<readonly TalentTree[]>();
    readonly order = input.required<PointOrder | null>();
    readonly levels = input.required<readonly number[]>();

    protected readonly open = signal(readStored(OPEN_KEY) !== 'false');

    protected readonly steps = computed<OrderStep[]>(() => {
        const talents = new Map(this.trees().flatMap((tree) => tree.talents.map((talent) => [talent.id, talent])));
        const levels = this.levels();
        const ranks = new Map<number, number>();

        return (this.order() ?? []).flatMap((id, index) => {
            const talent = talents.get(id);
            const rank = (ranks.get(id) ?? 0) + 1;

            ranks.set(id, rank);

            return talent ? [{ level: levels[index] ?? levels.at(-1) ?? 0, talent, rank }] : [];
        });
    });

    protected readonly hideBrokenIcon = hideBrokenIcon;

    protected toggle(): void {
        this.open.update((open) => !open);
        writeStored(OPEN_KEY, String(this.open()));
    }
}

function readStored(key: string): string | null {
    try {
        return localStorage.getItem(key);
    } catch {
        return null;
    }
}

function writeStored(key: string, value: string): void {
    try {
        localStorage.setItem(key, value);
    } catch {
        return;
    }
}
