package com.adrianpmdev.core.features.music.application.service;

import com.adrianpmdev.core.config.UploadConfig;
import com.adrianpmdev.core.features.catalog.api.dto.response.GameResponse;
import com.adrianpmdev.core.features.catalog.application.service.GameCatalogService;
import com.adrianpmdev.core.features.music.api.dto.response.MusicAreaResponse;
import com.adrianpmdev.core.features.music.api.dto.response.MusicTrackResponse;
import com.adrianpmdev.core.features.music.infrastructure.persistence.entity.MusicTrack;
import com.adrianpmdev.core.features.music.infrastructure.persistence.repository.MusicTrackRepository;
import com.adrianpmdev.core.security.authorization.Authorities;
import com.adrianpmdev.platform.module.GameModule;
import com.adrianpmdev.platform.port.ObjectStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Background music of the web, one playlist per area: {@value #PLATFORM_AREA} for the general web and one per game
 * module (its id), so a new game gets its own playlist without any change here. Songs live in the public bucket
 * {@value #BUCKET}, one folder per area.
 *
 * <ul>
 *     <li><b>Who hears what:</b> everyone signed in gets the general playlist; players get the playlists of the games
 *     they have the tag for (upcoming games are already left out by the token filter); admins get every area, also
 *     the songs of areas no module declares any more, so they can delete them.</li>
 *     <li><b>Files:</b> the admin's browser compresses every song to AAC in an M4A file before sending it, so the
 *     server only checks it (an MP4 file of at most 4 MB) and never decodes audio. The object name is
 *     {@code <area>/<id>.m4a}: it never changes, so browsers keep each song for a year.</li>
 *     <li><b>Order:</b> each area plays its songs in the order the admin sets; a new song goes last.</li>
 * </ul>
 * The playlists are tiny and asked on every page load, so they are kept in memory and reloaded after every change.
 *
 * @author adrianpmdev
 */
@Service
public class MusicService {

    static final String BUCKET = "public-music";
    static final String PLATFORM_AREA = "platform";
    static final int MAX_TRACKS_PER_AREA = 50;
    static final int MAX_DURATION_SECONDS = 10 * 60;

    private static final Logger AUDIT = LoggerFactory.getLogger("AUDIT");
    private static final Logger LOG = LoggerFactory.getLogger(MusicService.class);

    private static final String PLATFORM_NAME = "Web general";
    private static final String UNUSED_NAME = "Sin uso";
    private static final String CONTENT_TYPE = "audio/mp4";
    private static final String EXTENSION = ".m4a";
    private static final int MAX_TITLE_LENGTH = 100;
    private static final long MAX_FILE_BYTES = UploadConfig.MAX_FILE_BYTES;
    private static final Duration BROWSER_CACHE = Duration.ofDays(365);

    private final MusicTrackRepository trackRepository;
    private final ObjectStorage storage;
    private final GameCatalogService gameCatalogService;
    private final List<GameModule> gameModules;

    /** Every song by area, in playing order; null until first needed and after every change. */
    private volatile Map<String, List<MusicTrackResponse>> tracks;

    /**
     * Builds the service with its dependencies.
     *
     * @param trackRepository    song repository
     * @param storage            file storage
     * @param gameCatalogService game catalog (names and order of the games)
     * @param gameModules        game modules on the classpath, one playlist each
     */
    public MusicService(MusicTrackRepository trackRepository, ObjectStorage storage,
                        GameCatalogService gameCatalogService, List<GameModule> gameModules) {
        this.trackRepository = trackRepository;
        this.storage = storage;
        this.gameCatalogService = gameCatalogService;
        this.gameModules = List.copyOf(gameModules);
    }

    /**
     * The playlists the caller can hear: the general one first, then the games in catalog order.
     *
     * @param admin       whether the caller is an admin
     * @param authorities authorities of the caller
     * @return playlists, empty ones included
     */
    public List<MusicAreaResponse> getAreas(boolean admin, Collection<String> authorities) {
        Map<String, List<MusicTrackResponse>> songs = tracksByArea();
        List<MusicAreaResponse> areas = new ArrayList<>();

        declaredAreas().forEach((area, name) -> {
            if (admin || area.equals(PLATFORM_AREA) || authorities.contains(Authorities.tag(area))) {
                areas.add(new MusicAreaResponse(area, name, true, songs.getOrDefault(area, List.of())));
            }
        });

        if (admin) {
            Set<String> declared = declaredAreas().keySet();
            songs.forEach((area, list) -> {
                if (!declared.contains(area)) {
                    areas.add(new MusicAreaResponse(area, UNUSED_NAME + " · " + area, false, list));
                }
            });
        }

        return areas;
    }

    /**
     * Adds a song at the end of the playlist of an area.
     *
     * @param area            area id
     * @param file            song, already compressed to AAC in an M4A file
     * @param title           name of the song
     * @param durationSeconds length of the song, as the browser measured it
     * @param actorId         admin performing the action
     * @return the stored song
     */
    public MusicTrackResponse upload(String area, MultipartFile file, String title, int durationSeconds,
                                     UUID actorId) {
        if (!declaredAreas().containsKey(area)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No hay ninguna música prevista para esa área");
        }

        String cleanTitle = cleanTitle(title);

        if (durationSeconds < 1 || durationSeconds > MAX_DURATION_SECONDS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La canción no puede durar más de 10 minutos");
        }

        List<MusicTrack> current = trackRepository.findAllByAreaOrderByPositionAscCreatedAtAsc(area);

        if (current.size() >= MAX_TRACKS_PER_AREA) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Un área no puede tener más de " + MAX_TRACKS_PER_AREA + " canciones");
        }

        byte[] bytes = readSong(file);
        String objectName = area + "/" + UUID.randomUUID() + EXTENSION;
        storage.upload(BUCKET, objectName, bytes, CONTENT_TYPE, BROWSER_CACHE);

        MusicTrack track = new MusicTrack();
        track.setArea(area);
        track.setTitle(cleanTitle);
        track.setObjectName(objectName);
        track.setSizeBytes(bytes.length);
        track.setDurationSeconds(durationSeconds);
        track.setPosition(current.isEmpty() ? 0 : current.getLast().getPosition() + 1);

        try {
            track = trackRepository.save(track);
        } catch (RuntimeException exception) {
            deleteQuietly(objectName);
            throw exception;
        }

        tracks = null;
        AUDIT.info("actor={} action=MUSIC_UPLOADED area={} track={} bytes={}", actorId, area, track.getId(),
                bytes.length);

        return toResponse(track);
    }

    /**
     * Renames a song.
     *
     * @param id      song id
     * @param title   new name
     * @param actorId admin performing the action
     * @return the song
     */
    public MusicTrackResponse rename(UUID id, String title, UUID actorId) {
        MusicTrack track = find(id);
        track.setTitle(cleanTitle(title));
        trackRepository.save(track);
        tracks = null;
        AUDIT.info("actor={} action=MUSIC_RENAMED track={}", actorId, id);

        return toResponse(track);
    }

    /**
     * Sets the playing order of an area.
     *
     * @param area    area id
     * @param ids     every song of the area, in the new order
     * @param actorId admin performing the action
     */
    @Transactional
    public void reorder(String area, List<UUID> ids, UUID actorId) {
        List<MusicTrack> current = trackRepository.findAllByAreaOrderByPositionAscCreatedAtAsc(area);
        Map<UUID, MusicTrack> byId = current.stream().collect(Collectors.toMap(MusicTrack::getId, track -> track));

        if (ids.size() != byId.size() || !byId.keySet().equals(new HashSet<>(ids))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El orden tiene que incluir todas las canciones del área, una vez cada una");
        }

        for (int position = 0; position < ids.size(); position++) {
            byId.get(ids.get(position)).setPosition(position);
        }

        trackRepository.saveAll(current);
        tracks = null;
        AUDIT.info("actor={} action=MUSIC_REORDERED area={}", actorId, area);
    }

    /**
     * Deletes a song and its file.
     *
     * @param id      song id
     * @param actorId admin performing the action
     */
    public void delete(UUID id, UUID actorId) {
        MusicTrack track = find(id);
        storage.delete(BUCKET, List.of(track.getObjectName()));
        trackRepository.delete(track);
        tracks = null;
        AUDIT.info("actor={} action=MUSIC_DELETED area={} track={}", actorId, track.getArea(), id);
    }

    /* ─── Helpers ─── */

    /** Areas with a playlist and their names: the general web, then the game modules in catalog order. */
    private Map<String, String> declaredAreas() {
        Set<String> modules = gameModules.stream().map(GameModule::tag).collect(Collectors.toSet());
        Map<String, String> areas = new LinkedHashMap<>();
        areas.put(PLATFORM_AREA, PLATFORM_NAME);

        for (GameResponse game : gameCatalogService.getVisible(true, Set.of())) {
            if (modules.contains(game.id())) {
                areas.putIfAbsent(game.id(), game.name());
            }
        }

        for (String module : modules.stream().sorted().toList()) {
            areas.putIfAbsent(module, module);
        }

        return areas;
    }

    private Map<String, List<MusicTrackResponse>> tracksByArea() {
        Map<String, List<MusicTrackResponse>> current = tracks;

        if (current == null) {
            Map<String, List<MusicTrackResponse>> byArea = new LinkedHashMap<>();

            for (MusicTrack track : trackRepository.findAllByOrderByAreaAscPositionAscCreatedAtAsc()) {
                byArea.computeIfAbsent(track.getArea(), ignored -> new ArrayList<>()).add(toResponse(track));
            }

            byArea.replaceAll((area, list) -> List.copyOf(list));
            current = Map.copyOf(byArea);
            tracks = current;
        }

        return current;
    }

    private MusicTrackResponse toResponse(MusicTrack track) {
        return new MusicTrackResponse(track.getId(), track.getTitle(), storage.publicUrl(BUCKET, track.getObjectName()),
                track.getDurationSeconds(), track.getSizeBytes());
    }

    private MusicTrack find(UUID id) {
        return trackRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Canción no encontrada"));
    }

    private static String cleanTitle(String title) {
        String clean = title == null ? "" : title.strip();

        if (clean.isEmpty() || clean.length() > MAX_TITLE_LENGTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El título tiene que tener entre 1 y " + MAX_TITLE_LENGTH + " caracteres");
        }

        return clean;
    }

    /** The bytes of the song, checked to be an MP4 file (an M4A starts with an {@code ftyp} box). */
    private static byte[] readSong(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Falta la canción");
        }

        if (file.getSize() > MAX_FILE_BYTES) {
            throw new ResponseStatusException(HttpStatus.CONTENT_TOO_LARGE, "La canción no puede superar 4 MB");
        }

        byte[] bytes;

        try {
            bytes = file.getBytes();
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se ha podido leer la canción");
        }

        if (bytes.length < 12 || !"ftyp".equals(new String(bytes, 4, 4, StandardCharsets.US_ASCII))) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "La canción tiene que ser un M4A");
        }

        return bytes;
    }

    private void deleteQuietly(String objectName) {
        try {
            storage.delete(BUCKET, List.of(objectName));
        } catch (RuntimeException exception) {
            LOG.warn("Could not delete song object {}: {}", objectName, exception.getMessage());
        }
    }
}
