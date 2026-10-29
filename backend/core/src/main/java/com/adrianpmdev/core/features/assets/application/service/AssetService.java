package com.adrianpmdev.core.features.assets.application.service;

import com.adrianpmdev.core.config.UploadConfig;
import com.adrianpmdev.core.features.assets.api.dto.response.AssetResponse;
import com.adrianpmdev.core.features.assets.infrastructure.persistence.entity.Asset;
import com.adrianpmdev.core.features.assets.infrastructure.persistence.repository.AssetRepository;
import com.adrianpmdev.platform.asset.ImageSlot;
import com.adrianpmdev.platform.asset.ImageSlotProvider;
import com.adrianpmdev.platform.asset.PublicAssets;
import com.adrianpmdev.platform.image.CompressedImage;
import com.adrianpmdev.platform.image.ImageCompressor;
import com.adrianpmdev.platform.image.ImageSpec;
import com.adrianpmdev.platform.port.ObjectStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Public pictures uploaded by admins (game covers, WoW talent trees...), kept in the public bucket {@value #BUCKET}.
 *
 * <ul>
 *     <li><b>Which pictures:</b> only the keys some module declares through an {@link ImageSlotProvider}, so an
 *     upload can never land in an unexpected place.</li>
 *     <li><b>Files:</b> every upload goes through the {@link ImageCompressor} (at most {@value #MAX_SIDE} px and
 *     1.5 MB once compressed, or the smaller limits its slot sets, as icons do). The object name is
 *     {@code <key>.<ext>}, so replacing a picture overwrites it.</li>
 *     <li><b>URLs:</b> built here without calling Storage, with {@code ?v=<upload time>} so a new picture gets a new
 *     URL and browsers can cache each one for a year. They are kept in memory and reloaded after every change.</li>
 * </ul>
 *
 * @author adrianpmdev
 */
@Service
public class AssetService implements PublicAssets {

    static final String BUCKET = "public-assets";

    private static final Logger AUDIT = LoggerFactory.getLogger("AUDIT");
    private static final Logger LOG = LoggerFactory.getLogger(AssetService.class);

    private static final int MAX_SIDE = 1920;
    private static final ImageSpec SPEC = new ImageSpec(MAX_SIDE, 1_500_000);
    private static final long MAX_SOURCE_BYTES = UploadConfig.MAX_FILE_BYTES;
    private static final Duration BROWSER_CACHE = Duration.ofDays(365);
    private static final String UNUSED_GROUP = "Sin uso";

    private final AssetRepository assetRepository;
    private final ImageCompressor imageCompressor;
    private final ObjectStorage storage;
    private final ObjectProvider<ImageSlotProvider> slotProviders;

    /** URL by key of every uploaded picture; null until first needed and after every change. */
    private volatile Map<String, String> urls;

    /**
     * Builds the service with its dependencies.
     *
     * @param assetRepository asset repository
     * @param imageCompressor picture compressor
     * @param storage         file storage
     * @param slotProviders   modules that declare pictures (read on use, so they may depend on this service)
     */
    public AssetService(AssetRepository assetRepository, ImageCompressor imageCompressor, ObjectStorage storage,
                        ObjectProvider<ImageSlotProvider> slotProviders) {
        this.assetRepository = assetRepository;
        this.imageCompressor = imageCompressor;
        this.storage = storage;
        this.slotProviders = slotProviders;
    }

    /**
     * Every declared picture, uploaded or not, followed by uploaded pictures no module expects any more.
     *
     * @return pictures for the admin screen
     */
    public List<AssetResponse> getAll() {
        Map<String, Asset> uploaded = assetRepository.findAll().stream()
                .collect(Collectors.toMap(Asset::getKey, asset -> asset));
        List<AssetResponse> responses = new ArrayList<>();

        for (ImageSlot slot : declaredSlots().values()) {
            responses.add(toResponse(slot.key(), slot.group(), slot.label(), true, uploaded.remove(slot.key())));
        }

        uploaded.values().stream()
                .sorted((a, b) -> a.getKey().compareTo(b.getKey()))
                .forEach(asset -> responses.add(toResponse(asset.getKey(), UNUSED_GROUP, asset.getKey(), false, asset)));

        return responses;
    }

    /**
     * Compresses and uploads (or replaces) the picture of a declared key.
     *
     * @param key     picture key
     * @param file    picture
     * @param actorId admin performing the action
     * @return the stored picture
     */
    public AssetResponse upload(String key, MultipartFile file, UUID actorId) {
        ImageSlot slot = declaredSlots().get(key);

        if (slot == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No hay ninguna imagen prevista con esa clave");
        }

        CompressedImage image = imageCompressor.compress(readLimited(file), slot.spec() == null ? SPEC : slot.spec());
        String objectName = key + "." + image.extension();
        storage.upload(BUCKET, objectName, image.bytes(), image.contentType(), BROWSER_CACHE);

        Asset asset = assetRepository.findById(key).orElseGet(() -> new Asset(key));
        String previousObject = asset.getObjectName();

        asset.setObjectName(objectName);
        asset.setContentType(image.contentType());
        asset.setSizeBytes(image.bytes().length);
        asset.setWidth(image.width());
        asset.setHeight(image.height());
        asset.setUpdatedAt(Instant.now());
        assetRepository.save(asset);
        urls = null;

        // A picture that changes format (JPEG <-> PNG) leaves its old object behind.
        if (previousObject != null && !previousObject.equals(objectName)) {
            deleteQuietly(previousObject);
        }

        AUDIT.info("actor={} action=ASSET_UPLOADED key={} bytes={}", actorId, key, image.bytes().length);

        return toResponse(slot.key(), slot.group(), slot.label(), true, asset);
    }

    /**
     * Deletes an uploaded picture.
     *
     * @param key     picture key
     * @param actorId admin performing the action
     */
    public void delete(String key, UUID actorId) {
        Asset asset = assetRepository.findById(key)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Image not found"));

        storage.delete(BUCKET, List.of(asset.getObjectName()));
        assetRepository.delete(asset);
        urls = null;
        AUDIT.info("actor={} action=ASSET_DELETED key={}", actorId, key);
    }

    /**
     * Deletes a picture if it was uploaded, logging failures (used when what it illustrates is deleted).
     *
     * @param key     picture key
     * @param actorId admin performing the action
     */
    public void deleteIfPresent(String key, UUID actorId) {
        if (assetRepository.existsById(key)) {
            try {
                delete(key, actorId);
            } catch (RuntimeException exception) {
                LOG.warn("Could not delete picture {}: {}", key, exception.getMessage());
            }
        }
    }

    @Override
    public Map<String, String> urls(String prefix) {
        return urlsByKey().entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(prefix))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    /* ─── Helpers ─── */

    private Map<String, String> urlsByKey() {
        Map<String, String> current = urls;

        if (current == null) {
            current = assetRepository.findAll().stream()
                    .collect(Collectors.toUnmodifiableMap(Asset::getKey, this::urlOf));
            urls = current;
        }

        return current;
    }

    private String urlOf(Asset asset) {
        return storage.publicUrl(BUCKET, asset.getObjectName()) + "?v=" + asset.getUpdatedAt().getEpochSecond();
    }

    /** Declared slots by key, in the order the modules give them. A key declared twice keeps its first declaration. */
    private Map<String, ImageSlot> declaredSlots() {
        Map<String, ImageSlot> slots = new LinkedHashMap<>();

        for (ImageSlotProvider provider : slotProviders.orderedStream().toList()) {
            for (ImageSlot slot : provider.imageSlots()) {
                slots.putIfAbsent(slot.key(), slot);
            }
        }

        return Collections.unmodifiableMap(slots);
    }

    private AssetResponse toResponse(String key, String group, String label, boolean declared, Asset asset) {
        if (asset == null) {
            return new AssetResponse(key, group, label, declared, null, null, null, null, null, null);
        }

        return new AssetResponse(key, group, label, declared, urlOf(asset), asset.getContentType(), asset.getSizeBytes(),
                asset.getWidth(), asset.getHeight(), asset.getUpdatedAt());
    }

    private void deleteQuietly(String objectName) {
        try {
            storage.delete(BUCKET, List.of(objectName));
        } catch (RuntimeException exception) {
            LOG.warn("Could not delete old picture object {}: {}", objectName, exception.getMessage());
        }
    }

    private static byte[] readLimited(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Falta la imagen");
        }

        if (file.getSize() > MAX_SOURCE_BYTES) {
            throw new ResponseStatusException(HttpStatus.CONTENT_TOO_LARGE, "La imagen no puede superar 4 MB");
        }

        try {
            return file.getBytes();
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se ha podido leer la imagen");
        }
    }
}
