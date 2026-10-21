/**
 * UserService
 *
 * Service for managing user CRUD operations in the panel. Users are addressed by id.
 *
 * @author @adrianpmdev
 */

import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { CreatedUserResponse, CreateUserRequest, UpdateUserRequest, User } from '@shared/models/user';
import { API_BASE_URL } from '@core/config/api';

@Injectable({ providedIn: 'root' })
export class UserService {

    private readonly http = inject(HttpClient);
    private readonly baseUrl = `${API_BASE_URL}/users`;

    getAllUsers(): Observable<User[]> {
        return this.http.get<User[]>(this.baseUrl);
    }

    getUserById(id: string): Observable<User> {
        return this.http.get<User>(`${this.baseUrl}/${id}`);
    }

    /** Creates the user and makes the backend email them a link to choose their own password. */
    createUser(request: CreateUserRequest): Observable<CreatedUserResponse> {
        return this.http.post<CreatedUserResponse>(this.baseUrl, request);
    }

    updateUser(id: string, request: UpdateUserRequest): Observable<User> {
        return this.http.put<User>(`${this.baseUrl}/${id}`, request);
    }

    deleteUser(id: string): Observable<void> {
        return this.http.delete<void>(`${this.baseUrl}/${id}`);
    }

    /** Sends the "choose your password" email again (expired link, lost email or forgotten password). */
    resendInvite(id: string): Observable<void> {
        return this.http.post<void>(`${this.baseUrl}/${id}/invite`, {});
    }

    /** Replaces the whole tag set of a user. */
    setUserTags(id: string, tags: string[]): Observable<User> {
        return this.http.put<User>(`${this.baseUrl}/${id}/tags`, { tags });
    }
}
