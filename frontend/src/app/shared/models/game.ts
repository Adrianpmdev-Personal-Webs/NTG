/**
 * Game catalog models
 *
 * A game of the catalog as the backend gives it to the current user, and the data an admin sends to change it.
 *
 * @author @adrianpmdev
 */

export interface CatalogGame {
    id: string;
    name: string;
    coverUrl: string | null;
    upcoming: boolean;
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
