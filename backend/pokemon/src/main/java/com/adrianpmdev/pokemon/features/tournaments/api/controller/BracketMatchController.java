package com.adrianpmdev.pokemon.features.tournaments.api.controller;


import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.adrianpmdev.pokemon.features.tournaments.api.dto.request.BracketMatchCreateRequest;
import com.adrianpmdev.pokemon.features.tournaments.api.dto.request.BracketMatchUpdateRequest;
import com.adrianpmdev.pokemon.features.tournaments.api.dto.response.BracketMatchResponse;
import com.adrianpmdev.pokemon.features.tournaments.application.service.BracketMatchService;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for managing bracket matches.
 *
 * @author adrianpmdev
 */
@RestController
@RequestMapping("/api/v1/pokemon/bracket-matches")
@RequiredArgsConstructor
public class BracketMatchController {

    private final BracketMatchService matchService;

    @PostMapping
    public ResponseEntity<BracketMatchResponse> createMatch(
            @Valid @RequestBody BracketMatchCreateRequest request
    ) {
        BracketMatchResponse createdMatch = matchService.createMatch(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdMatch);
    }

    @GetMapping("/{id}")
    public ResponseEntity<BracketMatchResponse> getMatchById(@PathVariable UUID id) {
        BracketMatchResponse match = matchService.getMatchById(id);
        return ResponseEntity.ok(match);
    }

    @GetMapping
    public ResponseEntity<List<BracketMatchResponse>> getAllMatches() {
        List<BracketMatchResponse> matches = matchService.getAllMatches();
        return ResponseEntity.ok(matches);
    }

    @GetMapping("/stage/{stageId}")
    public ResponseEntity<List<BracketMatchResponse>> getMatchesByBracketStageId(@PathVariable UUID stageId) {
        List<BracketMatchResponse> matches = matchService.getMatchesByBracketStageId(stageId);
        return ResponseEntity.ok(matches);
    }

    @GetMapping("/tournament/{tournamentId}")
    public ResponseEntity<List<BracketMatchResponse>> getMatchesByTournamentId(@PathVariable UUID tournamentId) {
        List<BracketMatchResponse> matches = matchService.getMatchesByTournamentId(tournamentId);
        return ResponseEntity.ok(matches);
    }

    @PutMapping("/{id}")
    public ResponseEntity<BracketMatchResponse> updateMatch(
            @PathVariable UUID id,
            @RequestBody BracketMatchUpdateRequest request
    ) {
        BracketMatchResponse updatedMatch = matchService.updateMatch(id, request);
        return ResponseEntity.ok(updatedMatch);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMatch(@PathVariable UUID id) {
        matchService.deleteMatch(id);
        return ResponseEntity.noContent().build();
    }
}