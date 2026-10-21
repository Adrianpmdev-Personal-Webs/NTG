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

import com.adrianpmdev.pokemon.features.tournaments.api.dto.request.TournamentParticipantCreateRequest;
import com.adrianpmdev.pokemon.features.tournaments.api.dto.request.TournamentParticipantUpdateRequest;
import com.adrianpmdev.pokemon.features.tournaments.api.dto.response.TournamentParticipantResponse;
import com.adrianpmdev.pokemon.features.tournaments.application.service.TournamentParticipantService;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for managing tournament participants.
 *
 * @author adrianpmdev
 */
@RestController
@RequestMapping("/api/v1/pokemon/tournament-participants")
@RequiredArgsConstructor
public class TournamentParticipantController {

    private final TournamentParticipantService participantService;

    @PostMapping
    public ResponseEntity<TournamentParticipantResponse> createParticipant(
            @Valid @RequestBody TournamentParticipantCreateRequest request
    ) {
        TournamentParticipantResponse createdParticipant = participantService.createParticipant(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdParticipant);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TournamentParticipantResponse> getParticipantById(@PathVariable UUID id) {
        TournamentParticipantResponse participant = participantService.getParticipantById(id);
        return ResponseEntity.ok(participant);
    }

    @GetMapping
    public ResponseEntity<List<TournamentParticipantResponse>> getAllParticipants() {
        List<TournamentParticipantResponse> participants = participantService.getAllParticipants();
        return ResponseEntity.ok(participants);
    }

    @GetMapping("/tournament/{tournamentId}")
    public ResponseEntity<List<TournamentParticipantResponse>> getParticipantsByTournamentId(
            @PathVariable UUID tournamentId
    ) {
        List<TournamentParticipantResponse> participants = participantService.getParticipantsByTournamentId(
                tournamentId
        );
        return ResponseEntity.ok(participants);
    }

    @GetMapping("/tournament/{tournamentId}/user/{userId}")
    public ResponseEntity<TournamentParticipantResponse> getParticipantByTournamentAndUser(
            @PathVariable UUID tournamentId,
            @PathVariable UUID userId
    ) {
        TournamentParticipantResponse participant = participantService.getParticipantByTournamentAndUser(
                tournamentId,
                userId
        );
        return ResponseEntity.ok(participant);
    }

    @GetMapping("/tournament/{tournamentId}/status/{status}")
    public ResponseEntity<List<TournamentParticipantResponse>> getParticipantsByTournamentIdAndStatus(
            @PathVariable UUID tournamentId,
            @PathVariable String status
    ) {
        List<TournamentParticipantResponse> participants = participantService.getParticipantsByTournamentIdAndStatus(
                tournamentId,
                status
        );
        return ResponseEntity.ok(participants);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<TournamentParticipantResponse>> getParticipantsByUserId(@PathVariable UUID userId) {
        List<TournamentParticipantResponse> participants = participantService.getParticipantsByUserId(userId);
        return ResponseEntity.ok(participants);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TournamentParticipantResponse> updateParticipant(
            @PathVariable UUID id,
            @RequestBody TournamentParticipantUpdateRequest request
    ) {
        TournamentParticipantResponse updatedParticipant = participantService.updateParticipant(id, request);
        return ResponseEntity.ok(updatedParticipant);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteParticipant(@PathVariable UUID id) {
        participantService.deleteParticipant(id);
        return ResponseEntity.noContent().build();
    }
}