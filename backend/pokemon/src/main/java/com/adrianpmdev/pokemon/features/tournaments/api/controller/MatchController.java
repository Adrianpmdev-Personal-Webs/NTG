package com.adrianpmdev.pokemon.features.tournaments.api.controller;

import com.adrianpmdev.pokemon.features.tournaments.api.dto.request.MatchGamesRequest;
import com.adrianpmdev.pokemon.features.tournaments.api.dto.response.TournamentDetailResponse;
import com.adrianpmdev.pokemon.features.tournaments.application.service.MatchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Series of a tournament (admin only).
 *
 * @author adrianpmdev
 */
@RestController
@RequestMapping("/api/v1/pokemon/matches")
@RequiredArgsConstructor
public class MatchController {

    private final MatchService matchService;

    /** Replaces the games of a series. */
    @PutMapping("/{id}/games")
    public TournamentDetailResponse recordGames(@PathVariable UUID id, @Valid @RequestBody MatchGamesRequest request) {
        return matchService.recordGames(id, request);
    }

    @DeleteMapping("/{id}")
    public TournamentDetailResponse delete(@PathVariable UUID id) {
        return matchService.delete(id);
    }
}
