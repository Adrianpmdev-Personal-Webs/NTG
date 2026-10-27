package com.adrianpmdev.core.features.assets.api.controller;

import com.adrianpmdev.core.features.assets.api.dto.response.AssetResponse;
import com.adrianpmdev.core.features.assets.application.service.AssetService;
import com.adrianpmdev.core.security.authentication.AuthenticatedUser;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * REST controller for the public pictures (ADMIN only, see {@code SecurityConfig}). The key goes as a query parameter
 * because it contains slashes.
 *
 * @author adrianpmdev
 */
@RestController
@RequestMapping("/api/v1/assets")
public class AssetController {

    private final AssetService assetService;

    /**
     * Builds the controller with its dependencies.
     *
     * @param assetService asset service
     */
    public AssetController(AssetService assetService) {
        this.assetService = assetService;
    }

    /**
     * Lists every expected picture, uploaded or not.
     *
     * @return pictures
     */
    @GetMapping
    public List<AssetResponse> getAll() {
        return assetService.getAll();
    }

    /**
     * Uploads or replaces a picture. The server compresses it.
     *
     * @param key   picture key
     * @param file  picture (JPEG or PNG, up to 4 MB)
     * @param admin authenticated admin
     * @return stored picture
     */
    @PutMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AssetResponse upload(@RequestParam String key, @RequestPart("file") MultipartFile file,
                                @AuthenticationPrincipal AuthenticatedUser admin) {
        return assetService.upload(key, file, admin.id());
    }

    /**
     * Deletes a picture.
     *
     * @param key   picture key
     * @param admin authenticated admin
     * @return empty response
     */
    @DeleteMapping
    public ResponseEntity<Void> delete(@RequestParam String key, @AuthenticationPrincipal AuthenticatedUser admin) {
        assetService.delete(key, admin.id());

        return ResponseEntity.noContent().build();
    }
}
