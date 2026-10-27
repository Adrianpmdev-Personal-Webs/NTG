package com.adrianpmdev.pokemon.features.players.api.controller;

import com.adrianpmdev.pokemon.features.players.api.dto.request.PlayerRequest;
import com.adrianpmdev.pokemon.features.players.api.dto.response.PlayerResponse;
import com.adrianpmdev.pokemon.features.players.application.service.PlayerService;
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
 * Players of the randomlockes.
 *
 * @author adrianpmdev
 */
@RestController
@RequestMapping("/api/v1/pokemon/players")
@RequiredArgsConstructor
public class PlayerController {

    private final PlayerService playerService;

    @GetMapping
    public List<PlayerResponse> list() {
        return playerService.list();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PlayerResponse create(@Valid @RequestBody PlayerRequest request) {
        return playerService.create(request);
    }

    @PutMapping("/{id}")
    public PlayerResponse update(@PathVariable UUID id, @Valid @RequestBody PlayerRequest request) {
        return playerService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        playerService.delete(id);
    }
}
