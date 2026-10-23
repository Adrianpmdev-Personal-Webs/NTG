/**
 * LauncherHeader
 *
 * Metal header of the launcher screen: decorative top menu, logo and a link back to NTG.
 *
 * @author @adrianpmdev
 */

import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
    selector: 'app-launcher-header',
    imports: [RouterLink],
    templateUrl: './launcher-header.html',
    styleUrl: './launcher-header.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class LauncherHeader {}
