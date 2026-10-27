package com.adrianpmdev.platform.web;

import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;

/**
 * Responses for JSON that only changes with a deploy (game data, type charts...). Each one carries an ETag, a
 * fingerprint of its content: when the browser asks again with the fingerprint it already has, Spring answers
 * {@code 304 Not Modified} with no body, so the data is not sent again through Vercel.
 *
 * @author adrianpmdev
 */
public final class JsonResponses {

    private JsonResponses() {
    }

    /**
     * A JSON body with its ETag and cache rules.
     *
     * @param json         serialized body
     * @param cacheControl how long the browser may keep it before asking again
     * @return response (Spring turns it into a 304 when the browser already has this version)
     */
    public static ResponseEntity<String> cacheable(String json, CacheControl cacheControl) {
        String etag = "\"" + DigestUtils.md5DigestAsHex(json.getBytes(StandardCharsets.UTF_8)) + "\"";

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .cacheControl(cacheControl)
                .eTag(etag)
                .body(json);
    }
}
