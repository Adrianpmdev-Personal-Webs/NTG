/**
 * TradeOffer
 *
 * Pokelike trade offer component.
 * Renders an asset-driven section of the Pokelike run interface.
 *
 * @author @adrianpmdev
 */

import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { PokelikeRunStore } from '../pokelike-run-store';

@Component({
    changeDetection: ChangeDetectionStrategy.OnPush,
    selector: 'app-trade-offer',
    imports: [],
    templateUrl: './trade-offer.html',
    styleUrl: './trade-offer.scss'
})
export class TradeOffer {

    readonly store = inject(PokelikeRunStore);
}
