/**
 * PokemonLayout
 *
 * Shell of the Pokemon game with its own look, independent from the terminal theme.
 * Provides the game navigation and the way back to the main system.
 *
 * @author @adrianpmdev
 */

import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '@core/services/auth';

@Component({
    changeDetection: ChangeDetectionStrategy.OnPush,
    selector: 'app-pokemon-layout',
    imports: [RouterOutlet, RouterLink, RouterLinkActive],
    templateUrl: './pokemon-layout.html',
    styleUrl: './pokemon-layout.scss'
})
export class PokemonLayout {

    readonly authService = inject(AuthService);

    readonly menuItems = [
        { label: 'Lockes', route: '/pokemon/lockes' },
        { label: 'Calculadora', route: '/pokemon/type-calculator' },
        { label: 'Pokelike', route: '/pokemon/pokelike' }
    ] as const;
}
