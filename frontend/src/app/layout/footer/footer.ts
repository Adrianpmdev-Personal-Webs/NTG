/**
 * Footer
 *
 * Bottom bar of the terminal layout.
 *
 * @author @adrianpmdev
 */

import { ChangeDetectionStrategy, Component } from '@angular/core';

@Component({
    changeDetection: ChangeDetectionStrategy.OnPush,
    selector: 'app-footer',
    templateUrl: './footer.html',
    styleUrl: './footer.scss'
})
export class Footer {}
