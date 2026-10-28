/**
 * AssetService
 *
 * HTTP service for the public pictures (admins only). Pictures are sent as they are: the backend validates and
 * compresses them, so this client has no image code.
 *
 * @author @adrianpmdev
 */

import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '@core/config/api';
import { Asset } from '@shared/models/asset';

/** Formats the backend accepts. */
export const ACCEPTED_IMAGE_TYPES = 'image/png,image/jpeg';

/** Largest original the backend accepts. */
export const MAX_IMAGE_BYTES = 4 * 1024 * 1024;

@Injectable({ providedIn: 'root' })
export class AssetService {

    private readonly http = inject(HttpClient);
    private readonly baseUrl = `${API_BASE_URL}/assets`;

    getAll(): Observable<Asset[]> {
        return this.http.get<Asset[]>(this.baseUrl);
    }

    upload(key: string, file: File): Observable<Asset> {
        const body = new FormData();
        body.append('file', file, file.name);

        return this.http.put<Asset>(this.baseUrl, body, { params: { key } });
    }

    delete(key: string): Observable<void> {
        return this.http.delete<void>(this.baseUrl, { params: { key } });
    }
}

/**
 * Checks a picture before sending it, so the user gets the message without waiting for the upload.
 *
 * @returns message for the user, or null when the picture can be sent
 */
export const checkImageFile = (file: File): string | null => {
    if (!ACCEPTED_IMAGE_TYPES.split(',').includes(file.type)) {
        return 'La imagen debe ser PNG o JPEG';
    }

    return file.size > MAX_IMAGE_BYTES ? 'La imagen no puede superar 4 MB' : null;
};
