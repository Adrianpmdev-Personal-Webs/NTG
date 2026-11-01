package com.adrianpmdev.core.features.users.application.service;

import com.adrianpmdev.core.config.UploadConfig;
import com.adrianpmdev.core.features.users.api.dto.response.UserResponse;
import com.adrianpmdev.core.features.users.api.mapper.UserMapper;
import com.adrianpmdev.core.features.users.infrastructure.persistence.entity.User;
import com.adrianpmdev.core.features.users.infrastructure.persistence.repository.UserRepository;
import com.adrianpmdev.platform.image.CompressedImage;
import com.adrianpmdev.platform.image.ImageCompressor;
import com.adrianpmdev.platform.image.ImageSpec;
import com.adrianpmdev.platform.port.ObjectStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

/**
 * Avatars uploaded by their owner. The server compresses them to a small picture and keeps them in the public bucket
 * as {@code avatars/<user id>-<random>.<ext>}, so a new avatar gets a new URL and browsers can cache each one for a
 * year. Only the owner's previous upload is ever deleted: an avatar typed as a URL points somewhere else.
 *
 * @author adrianpmdev
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserAvatarService {

    private static final Logger AUDIT = LoggerFactory.getLogger("AUDIT");

    private static final String BUCKET = "public-assets";
    private static final String FOLDER = "avatars/";
    private static final ImageSpec SPEC = new ImageSpec(256, 150_000);
    private static final Duration BROWSER_CACHE = Duration.ofDays(365);

    private final UserRepository userRepository;
    private final ImageCompressor imageCompressor;
    private final ObjectStorage storage;

    /**
     * Replaces the avatar of a user with an uploaded picture.
     *
     * @param id   user id
     * @param file the picture
     * @return updated user
     */
    @Transactional
    public UserResponse upload(UUID id, MultipartFile file) {
        User user = find(id);
        CompressedImage image = imageCompressor.compress(readLimited(file), SPEC);
        String objectName = FOLDER + id + "-" + UUID.randomUUID() + "." + image.extension();

        storage.upload(BUCKET, objectName, image.bytes(), image.contentType(), BROWSER_CACHE);

        String previous = user.getAvatarUrl();

        user.setAvatarUrl(storage.publicUrl(BUCKET, objectName));
        deleteOwnUpload(id, previous);
        AUDIT.info("actor={} action=AVATAR_UPLOADED bytes={}", id, image.bytes().length);

        return UserMapper.toResponse(user);
    }

    /**
     * Removes the avatar of a user.
     *
     * @param id user id
     * @return updated user
     */
    @Transactional
    public UserResponse delete(UUID id) {
        User user = find(id);
        String previous = user.getAvatarUrl();

        user.setAvatarUrl(null);
        deleteOwnUpload(id, previous);
        AUDIT.info("actor={} action=AVATAR_DELETED", id);

        return UserMapper.toResponse(user);
    }

    private User find(UUID id) {
        return userRepository.findWithTagsById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private void deleteOwnUpload(UUID id, String url) {
        String ownPrefix = FOLDER + id + "-";
        String ownUrl = storage.publicUrl(BUCKET, ownPrefix);

        if (url == null || !url.startsWith(ownUrl)) {
            return;
        }

        try {
            storage.delete(BUCKET, List.of(ownPrefix + url.substring(ownUrl.length())));
        } catch (RuntimeException exception) {
            log.warn("The previous avatar of {} could not be deleted: {}", id, exception.getMessage());
        }
    }

    private static byte[] readLimited(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Falta la imagen");
        }

        if (file.getSize() > UploadConfig.MAX_FILE_BYTES) {
            throw new ResponseStatusException(HttpStatus.CONTENT_TOO_LARGE, "La imagen no puede superar 4 MB");
        }

        try {
            return file.getBytes();
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se ha podido leer la imagen");
        }
    }
}
