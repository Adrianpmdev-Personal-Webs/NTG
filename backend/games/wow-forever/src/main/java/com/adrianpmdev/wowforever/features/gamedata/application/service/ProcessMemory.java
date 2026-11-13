package com.adrianpmdev.wowforever.features.gamedata.application.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The most memory the process has used since it started, as the system counts it (the figure Render compares with its
 * limit). Only Linux tells it; elsewhere it is unknown.
 *
 * @author adrianpmdev
 */
final class ProcessMemory {

    private static final Path STATUS = Path.of("/proc/self/status");
    private static final Pattern PEAK = Pattern.compile("^VmHWM:\\s+(\\d+) kB", Pattern.MULTILINE);
    private static final int KILOBYTES_PER_MEGABYTE = 1024;

    private ProcessMemory() {
    }

    /**
     * The peak of the process.
     *
     * @return megabytes, or empty when the system does not tell
     */
    static Optional<Long> peakMegabytes() {
        try {
            Matcher matcher = PEAK.matcher(Files.readString(STATUS));

            return matcher.find() ? Optional.of(Long.parseLong(matcher.group(1)) / KILOBYTES_PER_MEGABYTE)
                    : Optional.empty();
        } catch (IOException | RuntimeException exception) {
            return Optional.empty();
        }
    }
}
