package com.adrianpmdev.pokemon.features.tournaments.api.controller;


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

import com.adrianpmdev.pokemon.features.tournaments.api.dto.request.TournamentCreateRequest;
import com.adrianpmdev.pokemon.features.tournaments.api.dto.request.TournamentUpdateRequest;
import com.adrianpmdev.pokemon.features.tournaments.api.dto.response.TournamentResponse;
import com.adrianpmdev.pokemon.features.tournaments.application.service.TournamentService;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for managing tournaments.
 *
 * @author adrianpmdev
 */
@RestController
@RequestMapping("/api/v1/pokemon/tournaments")
@RequiredArgsConstructor
public class TournamentController {

    private final TournamentService tournamentService;

    @PostMapping
    public ResponseEntity<TournamentResponse> createTournament(
            @Valid @RequestBody TournamentCreateRequest request
    ) {
        TournamentResponse createdTournament = tournamentService.createTournament(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdTournament);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TournamentResponse> getTournamentById(@PathVariable UUID id) {
        TournamentResponse tournament = tournamentService.getTournamentById(id);
        return ResponseEntity.ok(tournament);
    }

    @GetMapping
    public ResponseEntity<List<TournamentResponse>> getAllTournaments() {
        List<TournamentResponse> tournaments = tournamentService.getAllTournaments();
        return ResponseEntity.ok(tournaments);
    }

    @GetMapping("/locke/{lockeId}")
    public ResponseEntity<List<TournamentResponse>> getTournamentsByLockeId(@PathVariable UUID lockeId) {
        List<TournamentResponse> tournaments = tournamentService.getTournamentsByLockeId(lockeId);
        return ResponseEntity.ok(tournaments);
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<TournamentResponse>> getTournamentsByStatus(@PathVariable String status) {
        List<TournamentResponse> tournaments = tournamentService.getTournamentsByStatus(status);
        return ResponseEntity.ok(tournaments);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TournamentResponse> updateTournament(
            @PathVariable UUID id,
            @RequestBody TournamentUpdateRequest request
    ) {
        TournamentResponse updatedTournament = tournamentService.updateTournament(id, request);
        return ResponseEntity.ok(updatedTournament);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTournament(@PathVariable UUID id) {
        tournamentService.deleteTournament(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/search")
    public ResponseEntity<List<TournamentResponse>> searchTournaments(@RequestParam String name) {
        List<TournamentResponse> tournaments = tournamentService.searchTournamentsByName(name);
        return ResponseEntity.ok(tournaments);
    }
}