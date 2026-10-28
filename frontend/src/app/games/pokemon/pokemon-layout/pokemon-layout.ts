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
import { AuthSession } from '@core/auth/auth-session';

@Component({
    changeDetection: ChangeDetectionStrategy.OnPush,
    selector: 'app-pokemon-layout',
    imports: [RouterOutlet, RouterLink, RouterLinkActive],
    templateUrl: './pokemon-layout.html',
    styleUrl: './pokemon-layout.scss',
})
export class PokemonLayout {
    protected readonly authSession = inject(AuthSession);

    protected readonly menuItems = [
        { label: 'Juegos', route: '/pokemon/games' },
        { label: 'Ranking', route: '/pokemon/ranking' },
        { label: 'Calculadora', route: '/pokemon/type-calculator' },
    ] as const;
}
