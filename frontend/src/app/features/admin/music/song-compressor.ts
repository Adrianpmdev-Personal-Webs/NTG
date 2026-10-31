/**
 * compressSong
 *
 * Turns any song the browser can read (MP3, WAV, FLAC, OGG, M4A...) into AAC in an M4A file small enough for the
 * backend (4 MB), with the encoder the browser already has (WebCodecs, through Mediabunny). The quality depends on
 * the length so every allowed song fits: up to 5 minutes 96 kbps, up to 8 minutes 64 kbps, up to 10 minutes 48 kbps.
 * At most two channels are kept and the tags (cover pictures included) are dropped. Mediabunny is loaded only when a
 * song is compressed, so it never reaches the screens of players.
 *
 * @author @adrianpmdev
 */

export const MAX_SOURCE_BYTES = 200 * 1024 * 1024;
export const MAX_SONG_BYTES = 4 * 1024 * 1024;
export const MAX_SONG_SECONDS = 10 * 60;

const BITRATES: readonly { upToSeconds: number; bitrate: number }[] = [
    { upToSeconds: 5 * 60, bitrate: 96_000 },
    { upToSeconds: 8 * 60, bitrate: 64_000 },
    { upToSeconds: MAX_SONG_SECONDS, bitrate: 48_000 },
];

export interface CompressedSong {
    blob: Blob;
    durationSeconds: number;
}

export class SongError extends Error {}

export async function compressSong(file: File, onProgress: (progress: number) => void): Promise<CompressedSong> {
    if (file.size > MAX_SOURCE_BYTES) {
        throw new SongError('El archivo original no puede superar 200 MB');
    }

    const { ALL_FORMATS, BlobSource, BufferTarget, Conversion, Input, Mp4OutputFormat, Output, canEncodeAudio } =
        await import('mediabunny');

    const input = new Input({ formats: ALL_FORMATS, source: new BlobSource(file) });

    try {
        if (!(await input.getPrimaryAudioTrack())) {
            throw new SongError('El archivo no tiene audio que el navegador sepa leer');
        }

        const seconds = await input.computeDuration();
        const bitrate = BITRATES.find((entry) => seconds <= entry.upToSeconds)?.bitrate;

        if (!bitrate) {
            throw new SongError('La canción no puede durar más de 10 minutos');
        }

        if (!(await canEncodeAudio('aac', { bitrate }))) {
            throw new SongError('Este navegador no puede comprimir a AAC: usa Chrome, Edge, Brave o Safari');
        }

        const output = new Output({
            format: new Mp4OutputFormat({ fastStart: 'in-memory' }),
            target: new BufferTarget(),
        });
        const conversion = await Conversion.init({
            input,
            output,
            video: { discard: true },
            audio: (track) => ({
                codec: 'aac',
                bitrate,
                numberOfChannels: Math.min(track.numberOfChannels, 2),
                forceTranscode: true,
            }),
            tags: {},
            showWarnings: false,
        });

        if (!conversion.isValid) {
            throw new SongError('El navegador no ha podido convertir el audio de este archivo');
        }

        conversion.onProgress = (progress) => onProgress(progress);
        await conversion.execute();

        const buffer = output.target.buffer;

        if (!buffer) {
            throw new SongError('No se ha podido comprimir la canción');
        }

        if (buffer.byteLength > MAX_SONG_BYTES) {
            throw new SongError('Comprimida sigue superando 4 MB');
        }

        return { blob: new Blob([buffer], { type: 'audio/mp4' }), durationSeconds: Math.max(1, Math.round(seconds)) };
    } catch (error) {
        if (error instanceof SongError) {
            throw error;
        }

        throw new SongError('El navegador no sabe leer este archivo de audio');
    } finally {
        input.dispose();
    }
}
