package com.adrianpmdev.pokemon.features.games.api.controller;


import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.adrianpmdev.pokemon.features.games.api.dto.request.GameCreateRequest;
import com.adrianpmdev.pokemon.features.games.api.dto.request.GameUpdateRequest;
import com.adrianpmdev.pokemon.features.games.api.dto.response.GameResponse;
import com.adrianpmdev.pokemon.features.games.application.service.GameService;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for managing games.
 *
 * @author adrianpmdev
 */
@RestController
@RequestMapping("/api/v1/pokemon/games")
@RequiredArgsConstructor
public class GameController {

    private final GameService gameService;

    @PostMapping
    public ResponseEntity<GameResponse> createGame(@Valid @RequestBody GameCreateRequest request) {
        GameResponse createdGame = gameService.createGame(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdGame);
    }

    @GetMapping("/{id}")
    public ResponseEntity<GameResponse> getGameById(@PathVariable UUID id) {
        GameResponse game = gameService.getGameById(id);
        return ResponseEntity.ok(game);
    }

    @GetMapping
    public ResponseEntity<List<GameResponse>> getAllGames() {
        List<GameResponse> games = gameService.getAllGames();
        return ResponseEntity.ok(games);
    }

    @PutMapping("/{id}")
    public ResponseEntity<GameResponse> updateGame(
            @PathVariable UUID id,
            @RequestBody GameUpdateRequest request
    ) {
        GameResponse updatedGame = gameService.updateGame(id, request);
        return ResponseEntity.ok(updatedGame);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGame(@PathVariable UUID id) {
        gameService.deleteGame(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/search")
    public ResponseEntity<List<GameResponse>> searchGames(@RequestParam String name) {
        List<GameResponse> games = gameService.searchGamesByName(name);
        return ResponseEntity.ok(games);
    }
}