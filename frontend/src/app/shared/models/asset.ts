/**
 * Asset models
 *
 * A public picture expected by the platform or a game, as the admin screen of pictures lists it.
 *
 * @author @adrianpmdev
 */

export interface Asset {
    /** Where the picture is used, for example platform/games/pokemon or wow-forever/talents/warrior-arms. */
    key: string;
    group: string;
    label: string;
    /** False for an uploaded picture that no module expects any more. */
    declared: boolean;
    url: string | null;
    contentType: string | null;
    sizeBytes: number | null;
    width: number | null;
    height: number | null;
    updatedAt: string | null;
}
