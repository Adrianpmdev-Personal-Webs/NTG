package com.adrianpmdev.core.features.music.api.controller;

import com.adrianpmdev.core.features.music.api.dto.request.ReorderTracksRequest;
import com.adrianpmdev.core.features.music.api.dto.request.UpdateTrackRequest;
import com.adrianpmdev.core.features.music.api.dto.response.MusicAreaResponse;
import com.adrianpmdev.core.features.music.api.dto.response.MusicTrackResponse;
import com.adrianpmdev.core.features.music.application.service.MusicService;
import com.adrianpmdev.core.security.authentication.AuthenticatedUser;
import com.adrianpmdev.core.security.authorization.Authorities;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST controller for the background music. Any signed-in user reads the playlists they can hear; changes are ADMIN
 * only (see {@code SecurityConfig}).
 *
 * @author adrianpmdev
 */
@RestController
@RequestMapping("/api/v1/music")
public class MusicController {

    private static final String ROLE_ADMIN = Authorities.ROLE_PREFIX + "ADMIN";

    private final MusicService musicService;

    /**
     * Builds the controller with its dependencies.
     *
     * @param musicService music service
     */
    public MusicController(MusicService musicService) {
        this.musicService = musicService;
    }

    /**
     * Lists the playlists the caller can hear.
     *
     * @param authentication current authentication
     * @return playlists by area
     */
    @GetMapping
    public List<MusicAreaResponse> getAreas(Authentication authentication) {
        Set<String> authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        return musicService.getAreas(authorities.contains(ROLE_ADMIN), authorities);
    }

    /**
     * Adds a song at the end of the playlist of an area.
     *
     * @param area            area id
     * @param file            song (M4A, up to 4 MB)
     * @param title           name of the song
     * @param durationSeconds length of the song
     * @param admin           authenticated admin
     * @return stored song
     */
    @PostMapping(path = "/areas/{area}/tracks", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MusicTrackResponse> upload(@PathVariable String area, @RequestPart("file") MultipartFile file,
                                                     @RequestParam String title, @RequestParam int durationSeconds,
                                                     @AuthenticationPrincipal AuthenticatedUser admin) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(musicService.upload(area, file, title, durationSeconds, admin.id()));
    }

    /**
     * Sets the playing order of an area.
     *
     * @param area    area id
     * @param request every song of the area, in the new order
     * @param admin   authenticated admin
     * @return empty response
     */
    @PutMapping("/areas/{area}/order")
    public ResponseEntity<Void> reorder(@PathVariable String area, @Valid @RequestBody ReorderTracksRequest request,
                                        @AuthenticationPrincipal AuthenticatedUser admin) {
        musicService.reorder(area, request.ids(), admin.id());

        return ResponseEntity.noContent().build();
    }

    /**
     * Renames a song.
     *
     * @param id      song id
     * @param request new name
     * @param admin   authenticated admin
     * @return the song
     */
    @PutMapping("/tracks/{id}")
    public MusicTrackResponse rename(@PathVariable UUID id, @Valid @RequestBody UpdateTrackRequest request,
                                     @AuthenticationPrincipal AuthenticatedUser admin) {
        return musicService.rename(id, request.title(), admin.id());
    }

    /**
     * Deletes a song.
     *
     * @param id    song id
     * @param admin authenticated admin
     * @return empty response
     */
    @DeleteMapping("/tracks/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id, @AuthenticationPrincipal AuthenticatedUser admin) {
        musicService.delete(id, admin.id());

        return ResponseEntity.noContent().build();
    }
}
