package com.adrianpmdev.platform.asset;

import java.util.Map;

/**
 * Public URLs of the pictures uploaded by admins. Implemented by core; modules put these URLs in their responses so
 * web clients never build them. A URL changes every time its picture is replaced, so browsers may cache it for long.
 *
 * @author adrianpmdev
 */
public interface PublicAssets {

    /**
     * URLs of the uploaded pictures whose key starts with a prefix. Keys without a picture are left out.
     *
     * @param prefix key prefix, for example {@code wow-forever/}
     * @return URL by full key
     */
    Map<String, String> urls(String prefix);
}
