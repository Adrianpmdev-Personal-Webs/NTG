/**
 * RulesView
 *
 * Rules document of a randomlocke: numbered rules with their conditions, exceptions, level caps and
 * tournaments.
 *
 * @author @adrianpmdev
 */

import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { LockeRules } from '../../data/locke-rules';

@Component({
    changeDetection: ChangeDetectionStrategy.OnPush,
    selector: 'app-pokemon-rules-view',
    templateUrl: './rules-view.html',
    styleUrl: './rules-view.scss'
})
export class RulesView {

    readonly rules = input<LockeRules | null>(null);
}
