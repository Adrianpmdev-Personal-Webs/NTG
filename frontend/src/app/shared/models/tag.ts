/**
 * TagModels
 *
 * Tag model. A tag whose key matches a game id (for example "pokemon") grants access to that game.
 *
 * @author @adrianpmdev
 */

export interface Tag {
    key: string;
    name: string;
    description?: string;
    createdAt?: string;
}
