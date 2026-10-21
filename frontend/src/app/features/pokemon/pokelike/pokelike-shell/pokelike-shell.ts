/**
 * PokelikeShell
 *
 * Pokelike shell page.
 * Composes the full image-based Pokelike run interface.
 *
 * @author @adrianpmdev
 */

import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { BattleView } from '../battle-view/battle-view';
import { EndPanel } from '../end-panel/end-panel';
import { InventoryPanel } from '../inventory-panel/inventory-panel';
import { ItemReward } from '../item-reward/item-reward';
import { MainMenu } from '../main-menu/main-menu';
import { MapBoard } from '../map-board/map-board';
import { OverlayPanel } from '../overlay-panel/overlay-panel';
import { PokelikeTopBar } from '../pokelike-top-bar/pokelike-top-bar';
import { PokemonReward } from '../pokemon-reward/pokemon-reward';
import { RunStatus } from '../run-status/run-status';
import { StarterSelector } from '../starter-selector/starter-selector';
import { TeamPanel } from '../team-panel/team-panel';
import { TradeOffer } from '../trade-offer/trade-offer';
import { PokelikeRunStore } from '../pokelike-run-store';

@Component({
    changeDetection: ChangeDetectionStrategy.OnPush,
    selector: 'app-pokelike-shell',
    imports: [
        BattleView,
        EndPanel,
        InventoryPanel,
        ItemReward,
        MainMenu,
        MapBoard,
        OverlayPanel,
        PokelikeTopBar,
        PokemonReward,
        RunStatus,
        StarterSelector,
        TeamPanel,
        TradeOffer
    ],
    templateUrl: './pokelike-shell.html',
    styleUrl: './pokelike-shell.scss'
})
export class PokelikeShell {

    readonly store = inject(PokelikeRunStore);
}
