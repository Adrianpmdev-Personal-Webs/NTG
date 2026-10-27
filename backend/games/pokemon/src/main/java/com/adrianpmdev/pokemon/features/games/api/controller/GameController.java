package com.adrianpmdev.pokemon.features.games.api.controller;

import com.adrianpmdev.pokemon.features.games.api.dto.request.GameRequest;
import com.adrianpmdev.pokemon.features.games.api.dto.response.GameResponse;
import com.adrianpmdev.pokemon.features.games.application.service.GameService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Pokémon games. Reading needs the pokemon tag; writing is admin only (core security rules).
 *
 * @author adrianpmdev
 */
@RestController
@RequestMapping("/api/v1/pokemon/games")
@RequiredArgsConstructor
public class GameController {

    private final GameService gameService;

    @GetMapping
    public List<GameResponse> list() {
        return gameService.list();
    }

    @GetMapping("/{id}")
    public GameResponse get(@PathVariable UUID id) {
        return gameService.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GameResponse create(@Valid @RequestBody GameRequest request) {
        return gameService.create(request);
    }

    @PutMapping("/{id}")
    public GameResponse update(@PathVariable UUID id, @Valid @RequestBody GameRequest request) {
        return gameService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        gameService.delete(id);
    }
}
