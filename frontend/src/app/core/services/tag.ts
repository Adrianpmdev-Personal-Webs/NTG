/**
 * TagService
 *
 * Service that lists the tags an admin can assign to users.
 *
 * @author @adrianpmdev
 */

import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Tag } from '@shared/models/tag';
import { API_BASE_URL } from '@core/config/api';

@Injectable({ providedIn: 'root' })
export class TagService {

    private readonly http = inject(HttpClient);
    private readonly baseUrl = `${API_BASE_URL}/tags`;

    getAllTags(): Observable<Tag[]> {
        return this.http.get<Tag[]>(this.baseUrl);
    }
}
