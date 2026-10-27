package com.adrianpmdev.pokemon.features.tournaments.application.service;

import com.adrianpmdev.pokemon.features.tournaments.api.dto.response.TeamImageResponse;
import com.adrianpmdev.pokemon.features.tournaments.domain.TournamentStatus;
import com.adrianpmdev.pokemon.features.tournaments.infrastructure.persistence.entity.Tournament;
import com.adrianpmdev.pokemon.features.tournaments.infrastructure.persistence.entity.TournamentPlayer;
import com.adrianpmdev.pokemon.features.tournaments.infrastructure.persistence.repository.TournamentPlayerRepository;
import com.adrianpmdev.pokemon.shared.error.ApiErrors;
import com.adrianpmdev.pokemon.shared.port.CurrentUser;
import com.adrianpmdev.pokemon.shared.port.ObjectStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Team pictures of tournament players, kept in the private Storage bucket {@value #BUCKET}.
 *
 * <ul>
 *     <li><b>Who may change a picture:</b> an admin, always; the player linked to that tournament entry,
 *     while the tournament is not finished and the teams are not revealed yet.</li>
 *     <li><b>Who may see it:</b> an admin and its owner, always; everybody else once the reveal moment
 *     has passed (or when the tournament has none).</li>
 *     <li><b>Files:</b> WebP, JPEG or PNG (checked by their first bytes, not by the name), up to 1 MB. The
 *     browser compresses pictures before sending them. Object names are generated here, never taken from
 *     the request.</li>
 *     <li><b>URLs:</b> the database keeps only the object name; clients get signed URLs that expire,
 *     cached here and reused until shortly before they expire.</li>
 * </ul>
 *
 * @author adrianpmdev
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TeamImageService {

    static final String BUCKET = "pokemon-teams";

    private static final int MAX_BYTES = 1024 * 1024;
    private static final Duration URL_VALIDITY = Duration.ofHours(1);
    private static final Duration URL_REUSE_MARGIN = Duration.ofMinutes(10);
    private static final int RANDOM_SUFFIX_BYTES = 4;

    private final TournamentPlayerRepository tournamentPlayerRepository;
    private final ObjectStorage storage;
    private final CurrentUser currentUser;

    private final Map<String, SignedUrl> urlCache = new ConcurrentHashMap<>();

    private record SignedUrl(String url, Instant expiresAt) {
    }

    /** Supported picture formats, recognised by their first bytes. */
    private enum ImageType {
        WEBP("image/webp", "webp"),
        JPEG("image/jpeg", "jpg"),
        PNG("image/png", "png");

        private final String contentType;
        private final String extension;

        ImageType(String contentType, String extension) {
            this.contentType = contentType;
            this.extension = extension;
        }

        static ImageType detect(byte[] bytes) {
            if (bytes.length >= 12 && ascii(bytes, 0, "RIFF") && ascii(bytes, 8, "WEBP")) {
                return WEBP;
            }

            if (bytes.length >= 3 && (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8 && (bytes[2] & 0xFF) == 0xFF) {
                return JPEG;
            }

            if (bytes.length >= 8 && (bytes[0] & 0xFF) == 0x89 && ascii(bytes, 1, "PNG")
                    && bytes[4] == 0x0D && bytes[5] == 0x0A && bytes[6] == 0x1A && bytes[7] == 0x0A) {
                return PNG;
            }

            return null;
        }

        private static boolean ascii(byte[] bytes, int offset, String text) {
            for (int i = 0; i < text.length(); i++) {
                if (bytes[offset + i] != text.charAt(i)) {
                    return false;
                }
            }

            return true;
        }
    }

    /**
     * Uploads or replaces the team picture of a tournament player.
     *
     * @param tournamentPlayerId tournament player id
     * @param file               picture
     * @return temporary URL of the new picture
     */
    @Transactional
    public TeamImageResponse upload(UUID tournamentPlayerId, MultipartFile file) {
        TournamentPlayer entry = findEntry(tournamentPlayerId);
        requireCanEdit(entry);

        byte[] bytes = readLimited(file);
        ImageType type = ImageType.detect(bytes);

        if (type == null) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "La imagen debe ser WebP, JPEG o PNG");
        }

        String objectName = "tournaments/%s/%s-%s.%s".formatted(entry.getTournament().getId(), entry.getId(),
                randomSuffix(), type.extension);
        storage.upload(BUCKET, objectName, bytes, type.contentType);

        String previous = entry.getTeamImage();
        entry.setTeamImage(objectName);
        tournamentPlayerRepository.save(entry);
        deleteQuietly(previous);

        return new TeamImageResponse(urlsFor(List.of(objectName)).get(objectName));
    }

    /**
     * Removes the team picture of a tournament player.
     *
     * @param tournamentPlayerId tournament player id
     */
    @Transactional
    public void remove(UUID tournamentPlayerId) {
        TournamentPlayer entry = findEntry(tournamentPlayerId);
        requireCanEdit(entry);

        String previous = entry.getTeamImage();
        entry.setTeamImage(null);
        tournamentPlayerRepository.save(entry);
        deleteQuietly(previous);
    }

    /**
     * Whether the caller may change the team picture of a tournament player now.
     *
     * @param entry tournament player (with player and tournament loaded)
     * @return true when they may upload or remove it
     */
    public boolean canEdit(TournamentPlayer entry) {
        if (currentUser.isAdmin()) {
            return true;
        }

        Tournament tournament = entry.getTournament();
        Instant revealAt = tournament.getTeamsRevealAt();

        return isOwner(entry)
                && tournament.getStatus() != TournamentStatus.FINISHED
                && (revealAt == null || Instant.now().isBefore(revealAt));
    }

    /**
     * Whether the caller may see the team picture of a tournament player.
     *
     * @param entry tournament player (with player and tournament loaded)
     * @return true when the picture can be shown to them
     */
    public boolean canSee(TournamentPlayer entry) {
        return currentUser.isAdmin() || isOwner(entry) || entry.getTournament().teamsRevealed(Instant.now());
    }

    /**
     * Whether the caller is the player of a tournament entry.
     *
     * @param entry tournament player (with player loaded)
     * @return true for their own entry
     */
    public boolean isOwner(TournamentPlayer entry) {
        UUID userId = entry.getPlayer().getUserId();

        return userId != null && currentUser.id().map(userId::equals).orElse(false);
    }

    /**
     * Temporary URLs of pictures. A Storage failure is logged and leaves the pictures out, so a page can
     * still load without them.
     *
     * @param objectNames object names
     * @return URL by object name
     */
    public Map<String, String> urlsFor(Collection<String> objectNames) {
        Instant reuseUntil = Instant.now().plus(URL_REUSE_MARGIN);
        Map<String, String> urls = new HashMap<>();
        List<String> missing = objectNames.stream()
                .filter(Objects::nonNull)
                .distinct()
                .filter(name -> {
                    SignedUrl cached = urlCache.get(name);

                    if (cached != null && cached.expiresAt().isAfter(reuseUntil)) {
                        urls.put(name, cached.url());
                        return false;
                    }

                    return true;
                })
                .toList();

        if (!missing.isEmpty()) {
            try {
                Instant expiresAt = Instant.now().plus(URL_VALIDITY);
                storage.signedUrls(BUCKET, missing, URL_VALIDITY).forEach((name, url) -> {
                    urlCache.put(name, new SignedUrl(url, expiresAt));
                    urls.put(name, url);
                });
            } catch (RuntimeException exception) {
                log.warn("Could not sign {} team picture URLs: {}", missing.size(), exception.getMessage());
            }
        }

        return urls;
    }

    /**
     * Deletes pictures from Storage, logging failures (used when tournaments are deleted).
     *
     * @param objectNames object names
     */
    public void deleteQuietly(Collection<String> objectNames) {
        List<String> names = objectNames.stream().filter(Objects::nonNull).toList();

        if (names.isEmpty()) {
            return;
        }

        try {
            storage.delete(BUCKET, names);
            names.forEach(urlCache::remove);
        } catch (RuntimeException exception) {
            log.warn("Could not delete {} team pictures: {}", names.size(), exception.getMessage());
        }
    }

    private void deleteQuietly(String objectName) {
        if (objectName != null) {
            deleteQuietly(List.of(objectName));
        }
    }

    private TournamentPlayer findEntry(UUID tournamentPlayerId) {
        return tournamentPlayerRepository.findDetailedById(tournamentPlayerId)
                .orElseThrow(() -> ApiErrors.notFound("Tournament player"));
    }

    private void requireCanEdit(TournamentPlayer entry) {
        if (!canEdit(entry)) {
            throw ApiErrors.forbidden();
        }
    }

    private static byte[] readLimited(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw ApiErrors.badRequest("Falta la imagen");
        }

        if (file.getSize() > MAX_BYTES) {
            throw new ResponseStatusException(HttpStatus.CONTENT_TOO_LARGE, "La imagen no puede superar 1 MB");
        }

        try {
            return file.getBytes();
        } catch (IOException exception) {
            throw ApiErrors.badRequest("No se ha podido leer la imagen");
        }
    }

    private static String randomSuffix() {
        byte[] bytes = new byte[RANDOM_SUFFIX_BYTES];
        ThreadLocalRandom.current().nextBytes(bytes);

        return HexFormat.of().formatHex(bytes);
    }
}
