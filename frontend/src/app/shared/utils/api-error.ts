/**
 * ApiError
 *
 * Reads the message of an API error response (RFC 7807 `detail`, or `message`) and falls back to a default text.
 *
 * @author @adrianpmdev
 */

interface ApiErrorBody {
    detail?: string;
    message?: string;
}

/** Message to show for a failed request: the server's `detail` or `message` when it sent one, otherwise `fallback`. */
export const resolveApiError = (error: unknown, fallback: string): string => {
    const body = (error as { error?: ApiErrorBody | null } | null)?.error;

    if (typeof body !== 'object' || body === null) {
        return fallback;
    }

    return body.detail || body.message || fallback;
};
