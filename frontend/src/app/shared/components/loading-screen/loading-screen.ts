/**
 * LoadingScreen
 *
 * The message with three pulsing dots shown while the web waits for something. Its styles are global (_loading-screen)
 * because index.html repeats it with the text of the start, to show it before the application exists.
 *
 * @author @adrianpmdev
 */

import { ChangeDetectionStrategy, Component, booleanAttribute, input } from '@angular/core';

@Component({
    changeDetection: ChangeDetectionStrategy.OnPush,
    selector: 'app-loading-screen',
    templateUrl: './loading-screen.html',
})
export class LoadingScreen {
    readonly text = input.required<string>();
    readonly fullScreen = input(false, { transform: booleanAttribute });
}
