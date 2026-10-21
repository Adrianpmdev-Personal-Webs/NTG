package com.adrianpmdev.pokemon.features.games.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.adrianpmdev.pokemon.features.games.api.dto.request.GameCreateRequest;
import com.adrianpmdev.pokemon.features.games.api.dto.request.GameUpdateRequest;
import com.adrianpmdev.pokemon.features.games.api.dto.response.GameResponse;
import com.adrianpmdev.pokemon.features.games.infrastructure.persistence.entity.Game;
import com.adrianpmdev.pokemon.features.games.api.mapper.GameMapper;
import com.adrianpmdev.pokemon.features.games.infrastructure.persistence.repository.GameRepository;

import java.util.List;
import java.util.UUID;

/**
 * Service for managing games.
 *
 * @author adrianpmdev
 */
@Service
@RequiredArgsConstructor
public class GameService {

    private final GameRepository gameRepository;

    public GameResponse createGame(GameCreateRequest request) {
        Game game = GameMapper.toEntity(request);
        return GameMapper.toResponse(gameRepository.save(game));
    }

    public GameResponse getGameById(UUID id) {
        return GameMapper.toResponse(getGameEntityById(id));
    }

    public List<GameResponse> getAllGames() {
        return gameRepository.findAll()
                .stream()
                .map(GameMapper::toResponse)
                .toList();
    }

    public GameResponse updateGame(UUID id, GameUpdateRequest request) {
        Game game = getGameEntityById(id);

        GameMapper.updateEntity(game, request);

        return GameMapper.toResponse(gameRepository.save(game));
    }

    public void deleteGame(UUID id) {
        Game game = getGameEntityById(id);
        gameRepository.delete(game);
    }

    public List<GameResponse> searchGamesByName(String name) {
        return gameRepository.findByNameContainingIgnoreCase(name)
                .stream()
                .map(GameMapper::toResponse)
                .toList();
    }

    private Game getGameEntityById(UUID id) {
        return gameRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Game not found"));
    }
}