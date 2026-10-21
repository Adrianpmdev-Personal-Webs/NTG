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

import com.adrianpmdev.pokemon.features.tournaments.api.dto.request.TournamentStandingCreateRequest;
import com.adrianpmdev.pokemon.features.tournaments.api.dto.request.TournamentStandingUpdateRequest;
import com.adrianpmdev.pokemon.features.tournaments.api.dto.response.TournamentStandingResponse;
import com.adrianpmdev.pokemon.features.tournaments.application.service.TournamentStandingService;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for managing tournament standings.
 *
 * @author adrianpmdev
 */
@RestController
@RequestMapping("/api/v1/pokemon/tournament-standings")
@RequiredArgsConstructor
public class TournamentStandingController {

    private final TournamentStandingService standingService;

    @PostMapping
    public ResponseEntity<TournamentStandingResponse> createStanding(
            @Valid @RequestBody TournamentStandingCreateRequest request
    ) {
        TournamentStandingResponse createdStanding = standingService.createStanding(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdStanding);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TournamentStandingResponse> getStandingById(@PathVariable UUID id) {
        TournamentStandingResponse standing = standingService.getStandingById(id);
        return ResponseEntity.ok(standing);
    }

    @GetMapping
    public ResponseEntity<List<TournamentStandingResponse>> getAllStandings() {
        List<TournamentStandingResponse> standings = standingService.getAllStandings();
        return ResponseEntity.ok(standings);
    }

    @GetMapping("/tournament/{tournamentId}")
    public ResponseEntity<List<TournamentStandingResponse>> getStandingsByTournamentId(
            @PathVariable UUID tournamentId
    ) {
        List<TournamentStandingResponse> standings = standingService.getStandingsByTournamentId(tournamentId);
        return ResponseEntity.ok(standings);
    }

    @GetMapping("/tournament/{tournamentId}/participant/{participantId}")
    public ResponseEntity<TournamentStandingResponse> getStandingByTournamentAndParticipant(
            @PathVariable UUID tournamentId,
            @PathVariable UUID participantId
    ) {
        TournamentStandingResponse standing = standingService.getStandingByTournamentAndParticipant(
                tournamentId,
                participantId
        );
        return ResponseEntity.ok(standing);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TournamentStandingResponse> updateStanding(
            @PathVariable UUID id,
            @RequestBody TournamentStandingUpdateRequest request
    ) {
        TournamentStandingResponse updatedStanding = standingService.updateStanding(id, request);
        return ResponseEntity.ok(updatedStanding);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStanding(@PathVariable UUID id) {
        standingService.deleteStanding(id);
        return ResponseEntity.noContent().build();
    }
}