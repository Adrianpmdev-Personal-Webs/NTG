/**
 * Asset models
 *
 * A public picture expected by the platform or a game, as the admin screen of pictures lists it.
 *
 * @author @adrianpmdev
 */

export interface Asset {
    key: string;
    group: string;
    label: string;
    declared: boolean;
    url: string | null;
    contentType: string | null;
    sizeBytes: number | null;
    width: number | null;
    height: number | null;
    updatedAt: string | null;
}
