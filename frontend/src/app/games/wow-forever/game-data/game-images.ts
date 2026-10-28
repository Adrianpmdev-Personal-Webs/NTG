/**
 * gameImages
 *
 * Signal with the URLs of the WoW Forever pictures for a component. It starts empty and never errors: a screen without
 * its pictures still works, it just shows the plain background.
 *
 * @author @adrianpmdev
 */

import { Signal, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { catchError, of } from 'rxjs';
import { GameDataApi } from './game-data-api';

export const gameImages = (): Signal<Readonly<Record<string, string>>> =>
    toSignal(
        inject(GameDataApi)
            .getImages()
            .pipe(catchError(() => of({}))),
        { initialValue: {} },
    );

export const cssUrl = (url: string | undefined): string => (url ? `url('${url}')` : 'none');
