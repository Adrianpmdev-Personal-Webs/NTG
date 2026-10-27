package com.adrianpmdev.pokemon.features.games.application.service;

import com.adrianpmdev.pokemon.features.games.api.dto.request.GameRequest;
import com.adrianpmdev.pokemon.features.games.api.dto.response.GameResponse;
import com.adrianpmdev.pokemon.features.games.infrastructure.persistence.entity.Game;
import com.adrianpmdev.pokemon.features.games.infrastructure.persistence.repository.GameRepository;
import com.adrianpmdev.pokemon.features.tournaments.infrastructure.persistence.repository.TournamentRepository;
import com.adrianpmdev.pokemon.shared.error.ApiErrors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Games: list, read, create, replace and delete. A game that has tournaments cannot be deleted (the
 * database refuses it and the API answers 409).
 *
 * @author adrianpmdev
 */
@Service
@RequiredArgsConstructor
public class GameService {

    private final GameRepository gameRepository;
    private final TournamentRepository tournamentRepository;

    /**
     * Every game with the number of randomlockes played in it.
     *
     * @return games
     */
    @Transactional(readOnly = true)
    public List<GameResponse> list() {
        Map<UUID, Integer> lockes = lockeCounts();

        return gameRepository.findAllByOrderByCreatedAtAsc().stream()
                .map(game -> toResponse(game, lockes.getOrDefault(game.getId(), 0)))
                .toList();
    }

    /**
     * One game.
     *
     * @param id game id
     * @return the game
     */
    @Transactional(readOnly = true)
    public GameResponse get(UUID id) {
        return toResponse(find(id), lockeCounts().getOrDefault(id, 0));
    }

    /**
     * Creates a game.
     *
     * @param request game data
     * @return the new game
     */
    @Transactional
    public GameResponse create(GameRequest request) {
        Game game = new Game();
        apply(game, request);

        return toResponse(gameRepository.save(game), 0);
    }

    /**
     * Replaces the data of a game.
     *
     * @param id      game id
     * @param request game data
     * @return the updated game
     */
    @Transactional
    public GameResponse update(UUID id, GameRequest request) {
        Game game = find(id);
        apply(game, request);

        return get(gameRepository.save(game).getId());
    }

    /**
     * Deletes a game without tournaments.
     *
     * @param id game id
     */
    @Transactional
    public void delete(UUID id) {
        gameRepository.delete(find(id));
        // Flush now so a game in use fails here, as a 409, and not at commit time.
        gameRepository.flush();
    }

    private Map<UUID, Integer> lockeCounts() {
        return tournamentRepository.countLockesByGame().stream()
                .collect(Collectors.toMap(TournamentRepository.GameLockeCount::getGameId, row -> (int) row.getLockes()));
    }

    private Game find(UUID id) {
        return gameRepository.findById(id).orElseThrow(() -> ApiErrors.notFound("Game"));
    }

    private static void apply(Game game, GameRequest request) {
        game.setName(request.name().trim());
        game.setDescription(blankToNull(request.description()));
        game.setImageUrl(request.imageUrl().trim());
    }

    private static GameResponse toResponse(Game game, int lockeCount) {
        return new GameResponse(game.getId(), game.getName(), game.getDescription(), game.getImageUrl(), lockeCount);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
