package com.adrianpmdev.wowforever.features.gamedata.application.service;

import com.adrianpmdev.platform.module.GameHighlightProvider;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Tells on the home page which game build the data comes from. It comes from memory, so it costs no call.
 *
 * @author adrianpmdev
 */
@Component
public class GameBuildHighlight implements GameHighlightProvider {

    private final GameDataService gameDataService;

    /**
     * Builds the provider.
     *
     * @param gameDataService the data being served
     */
    public GameBuildHighlight(GameDataService gameDataService) {
        this.gameDataService = gameDataService;
    }

    @Override
    public String gameKey() {
        return "wow-forever";
    }

    @Override
    public Optional<String> highlight() {
        return gameDataService.currentBuild().map(build -> "Build " + build);
    }
}
