/**
 * UserApi
 *
 * Service for managing user CRUD operations in the panel. Users are addressed by id.
 *
 * @author @adrianpmdev
 */

import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { CreatedUserResponse, CreateUserRequest, UpdateUserRequest, User } from '@shared/models/user';
import { API_BASE_URL } from '@core/api/api';

@Injectable({ providedIn: 'root' })
export class UserApi {
    private readonly http = inject(HttpClient);
    private readonly baseUrl = `${API_BASE_URL}/users`;

    getAllUsers(): Observable<User[]> {
        return this.http.get<User[]>(this.baseUrl);
    }

    createUser(request: CreateUserRequest): Observable<CreatedUserResponse> {
        return this.http.post<CreatedUserResponse>(this.baseUrl, request);
    }

    updateUser(id: string, request: UpdateUserRequest): Observable<User> {
        return this.http.put<User>(`${this.baseUrl}/${id}`, request);
    }

    deleteUser(id: string): Observable<void> {
        return this.http.delete<void>(`${this.baseUrl}/${id}`);
    }

    resendInvite(id: string): Observable<void> {
        return this.http.post<void>(`${this.baseUrl}/${id}/invite`, {});
    }

    setUserTags(id: string, tags: string[]): Observable<User> {
        return this.http.put<User>(`${this.baseUrl}/${id}/tags`, { tags });
    }

    updateMe(request: UpdateUserRequest): Observable<User> {
        return this.http.put<User>(`${this.baseUrl}/me`, request);
    }

    uploadMyAvatar(file: File): Observable<User> {
        const body = new FormData();
        body.append('file', file, file.name);

        return this.http.put<User>(`${this.baseUrl}/me/avatar`, body);
    }

    deleteMyAvatar(): Observable<User> {
        return this.http.delete<User>(`${this.baseUrl}/me/avatar`);
    }

    sendMyPasswordEmail(): Observable<void> {
        return this.http.post<void>(`${this.baseUrl}/me/password-email`, {});
    }
}
