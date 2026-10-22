/**
 * WowForeverLayout
 *
 * Shell of the WoW Forever area: faction background, header with the feature navigation, content and legal footer.
 *
 * @author @adrianpmdev
 */

import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { WOW_FEATURES } from '../wow-forever-config';
import { WowForeverTheme } from '../wow-forever-theme';

@Component({
    selector: 'app-wow-forever-layout',
    imports: [RouterOutlet, RouterLink, RouterLinkActive],
    templateUrl: './wow-forever-layout.html',
    styleUrl: './wow-forever-layout.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class WowForeverLayout {

    readonly features = WOW_FEATURES;
    readonly faction = inject(WowForeverTheme).faction;
}
