package com.adrianpmdev.core.features.catalog.api.controller;

import com.adrianpmdev.core.features.catalog.api.dto.request.CreateGameRequest;
import com.adrianpmdev.core.features.catalog.api.dto.request.UpdateGameRequest;
import com.adrianpmdev.core.features.catalog.api.dto.response.GameResponse;
import com.adrianpmdev.core.features.catalog.application.service.GameCatalogService;
import com.adrianpmdev.core.security.authentication.AuthenticatedUser;
import com.adrianpmdev.core.security.authorization.Authorities;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * REST controller for the game catalog. Any signed-in user reads the games they can see; changes are ADMIN only
 * (see {@code SecurityConfig}).
 *
 * @author adrianpmdev
 */
@RestController
@RequestMapping("/api/v1/games")
public class GameCatalogController {

    private static final String ROLE_ADMIN = Authorities.ROLE_PREFIX + "ADMIN";

    private final GameCatalogService gameCatalogService;

    /**
     * Builds the controller with its dependencies.
     *
     * @param gameCatalogService catalog service
     */
    public GameCatalogController(GameCatalogService gameCatalogService) {
        this.gameCatalogService = gameCatalogService;
    }

    /**
     * Lists the games the caller can see, with their cover and whether they can open them.
     *
     * @param authentication current authentication
     * @return visible games
     */
    @GetMapping
    public List<GameResponse> getVisible(Authentication authentication) {
        Set<String> authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        return gameCatalogService.getVisible(authorities.contains(ROLE_ADMIN), authorities);
    }

    /**
     * Adds a game to the catalog.
     *
     * @param request game data
     * @param admin   authenticated admin
     * @return created game
     */
    @PostMapping
    public ResponseEntity<GameResponse> create(
            @Valid @RequestBody CreateGameRequest request, @AuthenticationPrincipal AuthenticatedUser admin) {
        return ResponseEntity.status(HttpStatus.CREATED).body(gameCatalogService.create(request, admin.id()));
    }

    /**
     * Updates a game of the catalog.
     *
     * @param key     game key
     * @param request new data
     * @param admin   authenticated admin
     * @return updated game
     */
    @PutMapping("/{key}")
    public GameResponse update(
            @PathVariable String key, @Valid @RequestBody UpdateGameRequest request,
            @AuthenticationPrincipal AuthenticatedUser admin) {
        return gameCatalogService.update(key, request, admin.id());
    }

    /**
     * Removes a game from the catalog.
     *
     * @param key   game key
     * @param admin authenticated admin
     * @return empty response
     */
    @DeleteMapping("/{key}")
    public ResponseEntity<Void> delete(@PathVariable String key, @AuthenticationPrincipal AuthenticatedUser admin) {
        gameCatalogService.delete(key, admin.id());

        return ResponseEntity.noContent().build();
    }
}
