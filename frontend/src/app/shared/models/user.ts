/**
 * UserModels
 *
 * User model and the request bodies used to create and update users.
 *
 * @author @adrianpmdev
 */

export interface User {
    id: string;
    email: string;
    username: string;
    avatarUrl?: string;
    role: string;
    tags: string[];
    createdAt?: string;
}

export interface CreateUserRequest {
    email: string;
    username: string;
    tags: string[];
}

export interface CreatedUserResponse {
    user: User;
    inviteEmailSent: boolean;
}

export interface UpdateUserRequest {
    username: string;
    avatarUrl?: string;
}
