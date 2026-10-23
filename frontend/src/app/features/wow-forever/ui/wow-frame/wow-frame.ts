/**
 * WowFrame
 *
 * Metal frame that wraps the launcher screen.
 *
 * @author @adrianpmdev
 */

import { ChangeDetectionStrategy, Component } from '@angular/core';

@Component({
    selector: 'app-wow-frame',
    templateUrl: './wow-frame.html',
    styleUrl: './wow-frame.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class WowFrame {}
