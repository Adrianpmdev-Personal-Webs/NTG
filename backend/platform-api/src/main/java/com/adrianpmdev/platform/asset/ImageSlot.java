package com.adrianpmdev.platform.asset;

import java.util.regex.Pattern;

/**
 * A public picture the platform expects, which an admin uploads from the web. The key says where it is used and is
 * also where the file goes in the public bucket: its first segment is the area ({@code platform}, a game id...).
 *
 * @param key   unique key, lowercase segments separated by '/', for example {@code wow-forever/talents/warrior-arms}
 * @param group heading the admin screen groups the pictures by, for example {@code WoW Forever · Talentos}
 * @param label name of the picture for the admin
 *
 * @author adrianpmdev
 */
public record ImageSlot(String key, String group, String label) {

    /** Lowercase letters, digits, '-' and '_' in each segment, separated by '/'. */
    public static final Pattern KEY_PATTERN = Pattern.compile("^[a-z0-9][a-z0-9_-]*(/[a-z0-9][a-z0-9_-]*)*$");

    /** Longest key accepted. */
    public static final int KEY_MAX_LENGTH = 150;

    /**
     * Validates the key.
     */
    public ImageSlot {
        if (key == null || key.length() > KEY_MAX_LENGTH || !KEY_PATTERN.matcher(key).matches()) {
            throw new IllegalArgumentException("Invalid image key: " + key);
        }
    }
}
