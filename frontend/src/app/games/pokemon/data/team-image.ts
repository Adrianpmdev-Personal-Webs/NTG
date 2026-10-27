/**
 * team-image
 *
 * Compression of team pictures before they are uploaded. Supabase free plan has 1 GB of storage, so
 * every picture is resized to at most 1600 px on its longest side and re-encoded as WebP (JPEG when the
 * browser cannot write WebP). A screenshot of a team usually ends between 50 and 400 KB.
 *
 * @author @adrianpmdev
 */

/** Longest side of a stored picture. */
const MAX_SIDE = 1600;

/** Encoder quality (0 to 1). */
const QUALITY = 0.8;

/** Pictures larger than this are refused before compressing them. */
export const MAX_SOURCE_BYTES = 10 * 1024 * 1024;

/** The backend refuses anything larger. */
export const MAX_UPLOAD_BYTES = 1024 * 1024;

export const ACCEPTED_TYPES = 'image/png,image/jpeg,image/webp';

/**
 * Resizes and re-encodes a picture.
 *
 * @param file picture chosen by the user
 * @returns compressed picture
 * @throws Error with a message for the user when the picture cannot be used
 */
export async function compressTeamImage(file: File): Promise<Blob> {
    if (!ACCEPTED_TYPES.split(',').includes(file.type)) {
        throw new Error('La imagen debe ser PNG, JPEG o WebP');
    }

    if (file.size > MAX_SOURCE_BYTES) {
        throw new Error('La imagen no puede superar 10 MB');
    }

    const bitmap = await createImageBitmap(file);
    const scale = Math.min(1, MAX_SIDE / Math.max(bitmap.width, bitmap.height));
    const canvas = document.createElement('canvas');
    canvas.width = Math.round(bitmap.width * scale);
    canvas.height = Math.round(bitmap.height * scale);

    const context = canvas.getContext('2d');

    if (!context) {
        bitmap.close();
        throw new Error('El navegador no puede procesar la imagen');
    }

    context.drawImage(bitmap, 0, 0, canvas.width, canvas.height);
    bitmap.close();

    const blob = await encode(canvas, 'image/webp') ?? await encode(canvas, 'image/jpeg');

    if (!blob) {
        throw new Error('El navegador no puede procesar la imagen');
    }

    if (blob.size > MAX_UPLOAD_BYTES) {
        throw new Error('La imagen sigue ocupando más de 1 MB después de comprimirla');
    }

    return blob;
}

function encode(canvas: HTMLCanvasElement, type: string): Promise<Blob | null> {
    return new Promise(resolve => canvas.toBlob(blob => resolve(blob && blob.type === type ? blob : null), type, QUALITY));
}
