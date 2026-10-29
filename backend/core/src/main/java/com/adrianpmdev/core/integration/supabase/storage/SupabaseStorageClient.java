package com.adrianpmdev.core.integration.supabase.storage;

import com.adrianpmdev.core.integration.supabase.config.SupabaseProperties;
import com.adrianpmdev.platform.port.ObjectStorage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Thin HTTP client for Supabase Storage, used with the service role key (which bypasses the bucket
 * policies). It is the only class that knows the Storage endpoints; game modules see it through the
 * {@link ObjectStorage} port.
 *
 * @author adrianpmdev
 */
@Slf4j
@Component
public class SupabaseStorageClient implements ObjectStorage {

    private static final String API_KEY_HEADER = "apikey";
    private static final String UPSERT_HEADER = "x-upsert";
    private static final String NEW_STYLE_KEY_PREFIX = "sb_";

    private final RestTemplate restTemplate;
    private final SupabaseProperties properties;

    /**
     * Builds the client.
     *
     * @param restTemplate shared HTTP client with timeouts
     * @param properties   Supabase settings
     */
    public SupabaseStorageClient(RestTemplate restTemplate, SupabaseProperties properties) {
        this.restTemplate = restTemplate;
        this.properties = properties;
    }

    /** Response item of the batch signing endpoint. */
    private record SignedObject(String path, String signedURL, String error) {
    }

    @Override
    public void upload(String bucket, String objectName, byte[] content, String contentType, Duration cacheMaxAge) {
        HttpHeaders headers = serviceHeaders();
        headers.setContentType(MediaType.parseMediaType(contentType));
        headers.set(UPSERT_HEADER, "true");

        if (cacheMaxAge != null) {
            headers.setCacheControl(CacheControl.maxAge(cacheMaxAge).cachePublic());
        }

        exchange(objectUrl(bucket, objectName), HttpMethod.POST, new HttpEntity<>(content, headers), Void.class);
    }

    @Override
    public Optional<byte[]> download(String bucket, String objectName) {
        try {
            byte[] body = restTemplate.exchange(objectUrl(bucket, objectName), HttpMethod.GET,
                    new HttpEntity<>(serviceHeaders()), byte[].class).getBody();

            return Optional.ofNullable(body);
        } catch (HttpClientErrorException exception) {
            // Storage answers 400 or 404 for a missing object or bucket.
            if (exception.getStatusCode().value() == HttpStatus.NOT_FOUND.value()
                    || exception.getStatusCode().value() == HttpStatus.BAD_REQUEST.value()) {
                return Optional.empty();
            }

            log.warn("Supabase Storage GET {}/{} failed: {}", bucket, objectName, exception.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "El almacenamiento no está disponible");
        } catch (RestClientException exception) {
            log.warn("Supabase Storage GET {}/{} failed: {}", bucket, objectName, exception.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "El almacenamiento no está disponible");
        }
    }

    @Override
    public String publicUrl(String bucket, String objectName) {
        return storageUrl() + "/object/public/" + bucket + "/" + objectName;
    }

    @Override
    public void delete(String bucket, Collection<String> objectNames) {
        HttpHeaders headers = serviceHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        exchange(storageUrl() + "/object/" + bucket, HttpMethod.DELETE,
                new HttpEntity<>(Map.of("prefixes", List.copyOf(objectNames)), headers), Void.class);
    }

    @Override
    public Map<String, String> signedUrls(String bucket, Collection<String> objectNames, Duration validity) {
        HttpHeaders headers = serviceHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> body = Map.of("expiresIn", validity.toSeconds(), "paths", List.copyOf(objectNames));

        SignedObject[] signed = exchange(storageUrl() + "/object/sign/" + bucket, HttpMethod.POST,
                new HttpEntity<>(body, headers), SignedObject[].class);
        Map<String, String> urls = new HashMap<>();

        if (signed != null) {
            for (SignedObject object : signed) {
                if (object.signedURL() != null && object.error() == null) {
                    // signedURL is relative to the Storage API: /object/sign/<bucket>/<path>?token=...
                    urls.put(object.path(), storageUrl() + object.signedURL());
                }
            }
        }

        return urls;
    }

    private <T> T exchange(String url, HttpMethod method, HttpEntity<?> entity, Class<T> responseType) {
        try {
            return restTemplate.exchange(url, method, entity, responseType).getBody();
        } catch (RestClientException exception) {
            log.warn("Supabase Storage {} {} failed: {}", method, url.replace(properties.url(), ""), exception.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "El almacenamiento de imágenes no está disponible");
        }
    }

    private String storageUrl() {
        String base = properties.url().endsWith("/") ? properties.url().substring(0, properties.url().length() - 1) : properties.url();

        return base + "/storage/v1";
    }

    private String objectUrl(String bucket, String objectName) {
        return storageUrl() + "/object/" + bucket + "/" + objectName;
    }

    private HttpHeaders serviceHeaders() {
        String key = properties.serviceRoleKey();
        HttpHeaders headers = new HttpHeaders();
        headers.set(API_KEY_HEADER, key);

        // Legacy service_role keys are JWTs and also go as bearer; new secret keys (sb_...) only as apikey.
        if (!key.startsWith(NEW_STYLE_KEY_PREFIX)) {
            headers.setBearerAuth(key);
        }

        return headers;
    }
}
