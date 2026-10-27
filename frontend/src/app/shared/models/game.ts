/**
 * Game catalog models
 *
 * A game of the catalog as the backend gives it to the current user, and the data an admin sends to change it.
 *
 * @author @adrianpmdev
 */

export interface CatalogGame {
    /** Game id: key of the tag that unlocks it and its URL segment. */
    id: string;
    name: string;
    /** Public URL of the cover, or null when no cover has been uploaded. */
    coverUrl: string | null;
    /** Coming soon: listed for everyone, but only admins can open it. */
    upcoming: boolean;
    /** Whether the current user can open it. */
    accessible: boolean;
    sortOrder: number;
}

export interface CatalogGameRequest {
    name: string;
    upcoming: boolean;
    sortOrder: number;
}

export interface CreateCatalogGameRequest extends CatalogGameRequest {
    key: string;
}
