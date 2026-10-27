package com.adrianpmdev.platform.port;

import java.time.Duration;
import java.util.Collection;
import java.util.Map;

/**
 * Private file storage. Implemented by core on top of Supabase Storage, which keeps the keys and the
 * HTTP details out of the game module.
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
    void upload(String bucket, String objectName, byte[] content, String contentType);

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
