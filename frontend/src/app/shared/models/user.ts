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
    /** Tag keys granted to the user. A tag with the id of a game gives access to that game. */
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
    /** False when the user was created but the email with the password link could not be sent. */
    inviteEmailSent: boolean;
}

export interface UpdateUserRequest {
    username: string;
    avatarUrl?: string;
}
