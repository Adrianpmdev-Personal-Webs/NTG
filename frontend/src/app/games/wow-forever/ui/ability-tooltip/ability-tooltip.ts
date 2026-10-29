/**
 * AbilityTooltip
 *
 * Card that hangs from the pointer over an ability (a racial, for example): icon, name and description, placed like
 * the talent tooltip.
 *
 * @author @adrianpmdev
 */

import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { Ability } from '../../game-data/game-data-models';
import { hideBrokenIcon } from '../../wow-forever-config';
import { tooltipPosition } from '../tooltip-position';

@Component({
    selector: 'app-ability-tooltip',
    templateUrl: './ability-tooltip.html',
    styleUrl: './ability-tooltip.scss',
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AbilityTooltip {
    readonly ability = input.required<Ability>();
    readonly x = input(0);
    readonly y = input(0);

    protected readonly position = computed(() => tooltipPosition(this.x(), this.y()));

    protected readonly hideBrokenIcon = hideBrokenIcon;
}
