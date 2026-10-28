/**
 * ModalOverlay
 *
 * Behaviour shared by every modal backdrop: Escape or a click on the backdrop asks to close it,
 * focus moves into the dialog when it opens and stays inside while Tab is used, the dialog is
 * named after its title, and focus returns to the element that opened it when it closes.
 *
 * @author @adrianpmdev
 */

import { DOCUMENT, DestroyRef, Directive, ElementRef, afterNextRender, inject, output } from '@angular/core';

const FOCUSABLE = [
    'a[href]',
    'button:not([disabled])',
    'input:not([disabled]):not([type="hidden"])',
    'select:not([disabled])',
    'textarea:not([disabled])',
    '[tabindex]:not([tabindex="-1"])',
].join(', ');

const FORM_FIELD = 'input:not([disabled]):not([type="hidden"]), select:not([disabled]), textarea:not([disabled])';

const openOverlays: ModalOverlay[] = [];
let titleIds = 0;

@Directive({
    selector: '[appModalOverlay]',
    host: {
        '(click)': 'dismissOnBackdrop($event)',
        '(document:keydown)': 'handleKeydown($event)',
    },
})
export class ModalOverlay {
    readonly dismiss = output<void>();

    private readonly overlay: HTMLElement = inject(ElementRef).nativeElement;
    private readonly opener = inject(DOCUMENT).activeElement as HTMLElement | null;

    constructor() {
        openOverlays.push(this);
        afterNextRender(() => {
            this.nameDialogAfterTitle();
            this.focusInitialElement();
        });
        inject(DestroyRef).onDestroy(() => {
            openOverlays.splice(openOverlays.indexOf(this), 1);
            this.opener?.focus();
        });
    }

    protected dismissOnBackdrop(event: MouseEvent): void {
        if (event.target === this.overlay) {
            this.dismiss.emit();
        }
    }

    protected handleKeydown(event: KeyboardEvent): void {
        if (openOverlays[openOverlays.length - 1] !== this) {
            return;
        }

        if (event.key === 'Escape') {
            event.preventDefault();
            this.dismiss.emit();
        } else if (event.key === 'Tab') {
            this.keepFocusInside(event);
        }
    }

    private dialog(): HTMLElement {
        return this.overlay.querySelector<HTMLElement>('[role="dialog"]') ?? this.overlay;
    }

    private focusables(): HTMLElement[] {
        return [...this.dialog().querySelectorAll<HTMLElement>(FOCUSABLE)].filter(
            (element) => element.offsetParent !== null,
        );
    }

    private nameDialogAfterTitle(): void {
        const dialog = this.dialog();
        const title = dialog.querySelector<HTMLElement>('.modal-title');

        if (!title || dialog.hasAttribute('aria-labelledby') || dialog.hasAttribute('aria-label')) {
            return;
        }

        title.id ||= `modal-title-${++titleIds}`;
        dialog.setAttribute('aria-labelledby', title.id);
    }

    private focusInitialElement(): void {
        const dialog = this.dialog();
        const target = dialog.querySelector<HTMLElement>(FORM_FIELD) ?? this.focusables()[0];

        if (target) {
            target.focus();
        } else {
            dialog.tabIndex = -1;
            dialog.focus();
        }
    }

    private keepFocusInside(event: KeyboardEvent): void {
        const focusables = this.focusables();

        if (focusables.length === 0) {
            event.preventDefault();
            return;
        }

        const first = focusables[0];
        const last = focusables[focusables.length - 1];
        const active = this.overlay.ownerDocument.activeElement;

        if (!this.overlay.contains(active)) {
            event.preventDefault();
            first.focus();
        } else if (event.shiftKey && active === first) {
            event.preventDefault();
            last.focus();
        } else if (!event.shiftKey && active === last) {
            event.preventDefault();
            first.focus();
        }
    }
}
