package com.adrianpmdev.core.security.authorization;

import java.util.Locale;

/**
 * Helpers to build Spring Security authority names.
 *
 * @author adrianpmdev
 */
public final class Authorities {

    public static final String ROLE_PREFIX = "ROLE_";
    public static final String TAG_PREFIX = "TAG_";

    private Authorities() {
    }

    /**
     * Builds the authority granted by a tag, e.g. {@code pokemon} becomes {@code TAG_POKEMON}.
     *
     * @param tagKey tag key
     * @return authority name
     */
    public static String tag(String tagKey) {
        return TAG_PREFIX + tagKey.toUpperCase(Locale.ROOT);
    }
}