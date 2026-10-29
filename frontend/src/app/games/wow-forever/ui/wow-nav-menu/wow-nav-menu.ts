/**
 * WowNavMenu
 *
 * Menu button of the WoW Forever pages that opens a panel with their navigation links, for screens too small to show
 * them as buttons. The panel closes with Escape, a click outside or when a link is followed, and focus goes back to the
 * menu button when it closes from the keyboard.
 *
 * @author @adrianpmdev
 */

import {
    ChangeDetectionStrategy,
    Component,
    ElementRef,
    Injector,
    afterNextRender,
    inject,
    input,
    signal,
    viewChild,
} from '@angular/core';
import { WowButton } from '../wow-button/wow-button';

export interface WowNavLink {
    label: string;
    link: string;
}

let nextId = 0;

@Component({
    selector: 'app-wow-nav-menu',
    imports: [WowButton],
    templateUrl: './wow-nav-menu.html',
    styleUrl: './wow-nav-menu.scss',
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: {
        '(document:keydown.escape)': 'closeFromKeyboard()',
        '(document:click)': 'closeOnOutsideClick($event)',
    },
})
export class WowNavMenu {
    readonly links = input.required<readonly WowNavLink[]>();

    private readonly host = inject(ElementRef<HTMLElement>);
    private readonly injector = inject(Injector);
    private readonly toggleButton = viewChild.required<ElementRef<HTMLButtonElement>>('toggleButton');
    private readonly panel = viewChild<ElementRef<HTMLElement>>('panel');

    protected readonly panelId = `wow-nav-menu-${nextId++}`;
    protected readonly open = signal(false);

    protected toggle(): void {
        if (this.open()) {
            this.closeFromKeyboard();
            return;
        }

        this.open.set(true);
        afterNextRender(() => this.panel()?.nativeElement.querySelector('a')?.focus(), { injector: this.injector });
    }

    protected close(): void {
        this.open.set(false);
    }

    protected closeFromKeyboard(): void {
        if (!this.open()) {
            return;
        }

        this.open.set(false);
        this.toggleButton().nativeElement.focus();
    }

    protected closeOnOutsideClick(event: MouseEvent): void {
        if (this.open() && !this.host.nativeElement.contains(event.target as Node)) {
            this.open.set(false);
        }
    }
}
