package com.adrianpmdev.core.features.catalog.application.service;

import com.adrianpmdev.core.features.assets.application.service.AssetService;
import com.adrianpmdev.core.features.catalog.api.dto.request.CreateGameRequest;
import com.adrianpmdev.core.features.catalog.api.dto.request.UpdateGameRequest;
import com.adrianpmdev.core.features.catalog.api.dto.response.GameResponse;
import com.adrianpmdev.core.features.catalog.infrastructure.persistence.entity.CatalogGame;
import com.adrianpmdev.core.features.catalog.infrastructure.persistence.repository.CatalogGameRepository;
import com.adrianpmdev.core.features.tags.infrastructure.persistence.entity.Tag;
import com.adrianpmdev.core.features.tags.infrastructure.persistence.repository.TagRepository;
import com.adrianpmdev.core.security.authorization.Authorities;
import com.adrianpmdev.platform.asset.ImageSlot;
import com.adrianpmdev.platform.asset.ImageSlotProvider;
import com.adrianpmdev.platform.image.ImageSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Catalog of the games shown by the web clients.
 * <ul>
 *     <li><b>Who sees what:</b> admins see every game; players see the released games they have the tag for, and the
 *     upcoming ones as "coming soon" (listed but not accessible).</li>
 *     <li><b>Upcoming games:</b> the access token filter asks {@link #upcomingKeys()} and ignores the tag of an
 *     upcoming game for non-admins, so its API stays closed to players even if they have the tag.</li>
 *     <li><b>Pictures:</b> each game declares two public pictures, uploaded by an admin: its cover
 *     {@code platform/games/<key>} and its icon {@code platform/game-icons/<key>} (shown small in the menus, so it is
 *     stored at {@value #ICON_SIDE} px at most).</li>
 *     <li><b>Tags:</b> adding a game also creates the tag with its key (if it does not exist yet), so it can be given
 *     to users right away. Removing a game keeps its tag.</li>
 * </ul>
 * The catalog is tiny and read on every request by the token filter, so it is kept in memory and reloaded after every
 * change.
 *
 * @author adrianpmdev
 */
@Service
public class GameCatalogService implements ImageSlotProvider {

    private static final Logger AUDIT = LoggerFactory.getLogger("AUDIT");

    private static final String COVER_PREFIX = "platform/games/";
    private static final String COVER_GROUP = "Plataforma · Portadas de juegos";
    private static final String ICON_PREFIX = "platform/game-icons/";
    private static final String ICON_GROUP = "Plataforma · Iconos de juegos";
    private static final int ICON_SIDE = 128;
    private static final ImageSpec ICON_SPEC = new ImageSpec(ICON_SIDE, 100_000);

    private final CatalogGameRepository gameRepository;
    private final TagRepository tagRepository;
    private final AssetService assetService;

    /** Catalog in display order; null until first needed and after every change. */
    private volatile List<Entry> entries;

    /** Immutable copy of a game, safe to share between requests. */
    private record Entry(String key, String name, boolean upcoming, int sortOrder) {
    }

    /**
     * Builds the service with its dependencies.
     *
     * @param gameRepository catalog repository
     * @param tagRepository  tag repository (the tag of each new game)
     * @param assetService   public pictures (covers and icons)
     */
    public GameCatalogService(CatalogGameRepository gameRepository, TagRepository tagRepository,
                              AssetService assetService) {
        this.gameRepository = gameRepository;
        this.tagRepository = tagRepository;
        this.assetService = assetService;
    }

    /**
     * The catalog as the caller sees it.
     *
     * @param admin       whether the caller is an admin
     * @param authorities authorities of the caller (tags of upcoming games are already left out)
     * @return visible games in display order
     */
    public List<GameResponse> getVisible(boolean admin, Collection<String> authorities) {
        Map<String, String> covers = assetService.urls(COVER_PREFIX);
        Map<String, String> icons = assetService.urls(ICON_PREFIX);

        return entries().stream()
                .map(entry -> {
                    boolean accessible = admin || (!entry.upcoming() && authorities.contains(Authorities.tag(entry.key())));

                    return new GameResponse(entry.key(), entry.name(), covers.get(COVER_PREFIX + entry.key()),
                            icons.get(ICON_PREFIX + entry.key()), entry.upcoming(), accessible, entry.sortOrder());
                })
                .filter(game -> admin || game.accessible() || game.upcoming())
                .toList();
    }

    /**
     * Keys of the games that are still coming soon.
     *
     * @return upcoming game keys
     */
    public Set<String> upcomingKeys() {
        return entries().stream().filter(Entry::upcoming).map(Entry::key).collect(Collectors.toUnmodifiableSet());
    }

    /**
     * Adds a game to the catalog and creates its tag when it does not exist yet, both or neither.
     *
     * @param request game data
     * @param actorId admin performing the action
     * @return the game as an admin sees it
     */
    @Transactional
    public GameResponse create(CreateGameRequest request, UUID actorId) {
        if (gameRepository.existsById(request.key())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Game '" + request.key() + "' already exists");
        }

        String name = request.name().trim();
        CatalogGame game = gameRepository.save(
                new CatalogGame(request.key(), name, request.upcoming(), (short) request.sortOrder()));

        if (!tagRepository.existsById(game.getKey())) {
            tagRepository.save(new Tag(game.getKey(), name, "Acceso a " + name));
            AUDIT.info("actor={} action=TAG_CREATED tag={} reason=GAME_CREATED", actorId, game.getKey());
        }

        entries = null;
        AUDIT.info("actor={} action=GAME_CREATED game={}", actorId, game.getKey());

        return asAdmin(game);
    }

    /**
     * Updates the name, state and order of a game.
     *
     * @param key     game key
     * @param request new data
     * @param actorId admin performing the action
     * @return the game as an admin sees it
     */
    public GameResponse update(String key, UpdateGameRequest request, UUID actorId) {
        CatalogGame game = find(key);
        game.setName(request.name().trim());
        game.setUpcoming(request.upcoming());
        game.setSortOrder((short) request.sortOrder());
        gameRepository.save(game);
        entries = null;
        AUDIT.info("actor={} action=GAME_UPDATED game={} upcoming={}", actorId, key, request.upcoming());

        return asAdmin(game);
    }

    /**
     * Removes a game from the catalog, with its cover and its icon. Its tag and its data are kept.
     *
     * @param key     game key
     * @param actorId admin performing the action
     */
    public void delete(String key, UUID actorId) {
        CatalogGame game = find(key);
        gameRepository.delete(game);
        entries = null;
        assetService.deleteIfPresent(COVER_PREFIX + key, actorId);
        assetService.deleteIfPresent(ICON_PREFIX + key, actorId);
        AUDIT.info("actor={} action=GAME_DELETED game={}", actorId, key);
    }

    @Override
    public List<ImageSlot> imageSlots() {
        List<ImageSlot> slots = new ArrayList<>();

        for (Entry entry : entries()) {
            slots.add(new ImageSlot(COVER_PREFIX + entry.key(), COVER_GROUP, entry.name()));
        }

        for (Entry entry : entries()) {
            slots.add(new ImageSlot(ICON_PREFIX + entry.key(), ICON_GROUP, entry.name(), ICON_SPEC));
        }

        return List.copyOf(slots);
    }

    /* ─── Helpers ─── */

    private List<Entry> entries() {
        List<Entry> current = entries;

        if (current == null) {
            current = gameRepository.findAllByOrderBySortOrderAscKeyAsc().stream()
                    .map(game -> new Entry(game.getKey(), game.getName(), game.isUpcoming(), game.getSortOrder()))
                    .toList();
            entries = current;
        }

        return current;
    }

    private GameResponse asAdmin(CatalogGame game) {
        String cover = assetService.urls(COVER_PREFIX + game.getKey()).get(COVER_PREFIX + game.getKey());
        String icon = assetService.urls(ICON_PREFIX + game.getKey()).get(ICON_PREFIX + game.getKey());

        return new GameResponse(game.getKey(), game.getName(), cover, icon, game.isUpcoming(), true,
                game.getSortOrder());
    }

    private CatalogGame find(String key) {
        return gameRepository.findById(key)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Game not found"));
    }
}
