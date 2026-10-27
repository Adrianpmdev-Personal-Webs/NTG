package com.adrianpmdev.platform.module;

import org.springframework.http.HttpMethod;

import java.util.List;

/**
 * What a game module tells the platform about itself. Every game declares one as a bean and core builds the
 * access rules of its API from it, so core never names a game:
 * <ul>
 *     <li>reading under {@code apiPath} needs the game tag (or ADMIN);</li>
 *     <li>writing needs ADMIN, except the routes listed in {@code playerWrites}, which also accept the tag
 *     (the game then decides who may use them).</li>
 * </ul>
 *
 * @param tag          key of the tag that grants access to the game (also the game id)
 * @param apiPath      base path of the game API, for example {@code /api/v1/pokemon}
 * @param playerWrites write routes open to players with the tag, relative to {@code apiPath}
 *
 * @author adrianpmdev
 */
public record GameModule(String tag, String apiPath, List<PlayerWrite> playerWrites) {

    /**
     * Validates the declaration and copies the routes.
     */
    public GameModule {
        if (tag == null || tag.isBlank()) {
            throw new IllegalArgumentException("A game module needs a tag");
        }

        if (apiPath == null || !apiPath.startsWith("/") || apiPath.endsWith("/")) {
            throw new IllegalArgumentException("The API path of a game starts with '/' and does not end with it");
        }

        playerWrites = playerWrites == null ? List.of() : List.copyOf(playerWrites);
    }

    /**
     * Declares a game whose API is read-only for players.
     *
     * @param tag     key of the game tag
     * @param apiPath base path of the game API
     * @return the declaration
     */
    public static GameModule of(String tag, String apiPath) {
        return new GameModule(tag, apiPath, List.of());
    }

    /**
     * Pattern that matches every route of the game API.
     *
     * @return ant-style pattern
     */
    public String allRoutes() {
        return apiPath + "/**";
    }

    /**
     * A write route open to players with the game tag.
     *
     * @param method HTTP method
     * @param path   ant-style pattern relative to the game API path, for example {@code /items/*}
     */
    public record PlayerWrite(HttpMethod method, String path) {
    }
}
