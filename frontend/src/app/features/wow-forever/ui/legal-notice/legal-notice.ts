/**
 * LegalNotice
 *
 * Legal notice shown at the bottom of the launcher screen.
 *
 * @author @adrianpmdev
 */

import { ChangeDetectionStrategy, Component } from '@angular/core';

@Component({
    selector: 'app-legal-notice',
    templateUrl: './legal-notice.html',
    styleUrl: './legal-notice.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class LegalNotice {}
