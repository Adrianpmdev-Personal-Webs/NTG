package com.adrianpmdev.core.config;

import jakarta.servlet.MultipartConfigElement;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Limits of uploaded files. Pictures are sent as they are and compressed by the server, so the original may weigh up
 * to {@value #MAX_FILE_BYTES} bytes (4 MB), which stays below the 4.5 MB a request may carry through Vercel.
 * Bigger requests are refused before they are read.
 *
 * @author adrianpmdev
 */
@Configuration
public class UploadConfig {

    /** Largest uploaded file. */
    public static final long MAX_FILE_BYTES = 4L * 1024 * 1024;

    private static final long MAX_REQUEST_BYTES = MAX_FILE_BYTES + 64 * 1024;

    /** Files are kept in memory: they are small and nothing is written to the server disk. */
    private static final int FILE_SIZE_THRESHOLD = (int) MAX_REQUEST_BYTES;

    /**
     * Multipart limits used by the servlet container.
     *
     * @return multipart configuration
     */
    @Bean
    MultipartConfigElement multipartConfigElement() {
        return new MultipartConfigElement("", MAX_FILE_BYTES, MAX_REQUEST_BYTES, FILE_SIZE_THRESHOLD);
    }
}
