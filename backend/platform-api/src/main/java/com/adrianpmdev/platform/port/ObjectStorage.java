package com.adrianpmdev.platform.port;

import java.time.Duration;
import java.util.Collection;
import java.util.Map;

/**
 * File storage (private buckets with signed URLs, public buckets with plain URLs). Implemented by core on top of
 * Supabase Storage, which keeps the keys and the HTTP details out of the modules.
 *
 * @author adrianpmdev
 */
public interface ObjectStorage {

    /**
     * Uploads (or replaces) an object.
     *
     * @param bucket      bucket name
     * @param objectName  object name inside the bucket
     * @param content     file bytes
     * @param contentType MIME type
     */
    default void upload(String bucket, String objectName, byte[] content, String contentType) {
        upload(bucket, objectName, content, contentType, null);
    }

    /**
     * Uploads (or replaces) an object telling browsers and the CDN how long they may cache it.
     *
     * @param bucket      bucket name
     * @param objectName  object name inside the bucket
     * @param content     file bytes
     * @param contentType MIME type
     * @param cacheMaxAge how long the object may be cached, or null for the storage default
     */
    void upload(String bucket, String objectName, byte[] content, String contentType, Duration cacheMaxAge);

    /**
     * Address of an object in a public bucket. It needs no request to the storage.
     *
     * @param bucket     public bucket name
     * @param objectName object name inside the bucket
     * @return absolute URL
     */
    String publicUrl(String bucket, String objectName);

    /**
     * Deletes objects. Missing objects are ignored.
     *
     * @param bucket      bucket name
     * @param objectNames object names inside the bucket
     */
    void delete(String bucket, Collection<String> objectNames);

    /**
     * Creates temporary download URLs.
     *
     * @param bucket      bucket name
     * @param objectNames object names inside the bucket
     * @param validity    how long the URLs work
     * @return signed URL per object name (objects that do not exist are left out)
     */
    Map<String, String> signedUrls(String bucket, Collection<String> objectNames, Duration validity);
}
