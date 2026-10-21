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

import com.adrianpmdev.pokemon.features.tournaments.api.dto.request.BracketStageCreateRequest;
import com.adrianpmdev.pokemon.features.tournaments.api.dto.request.BracketStageUpdateRequest;
import com.adrianpmdev.pokemon.features.tournaments.api.dto.response.BracketStageResponse;
import com.adrianpmdev.pokemon.features.tournaments.application.service.BracketStageService;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for managing bracket stages.
 *
 * @author adrianpmdev
 */
@RestController
@RequestMapping("/api/v1/pokemon/bracket-stages")
@RequiredArgsConstructor
public class BracketStageController {

    private final BracketStageService stageService;

    @PostMapping
    public ResponseEntity<BracketStageResponse> createStage(
            @Valid @RequestBody BracketStageCreateRequest request
    ) {
        BracketStageResponse createdStage = stageService.createStage(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdStage);
    }

    @GetMapping("/{id}")
    public ResponseEntity<BracketStageResponse> getStageById(@PathVariable UUID id) {
        BracketStageResponse stage = stageService.getStageById(id);
        return ResponseEntity.ok(stage);
    }

    @GetMapping
    public ResponseEntity<List<BracketStageResponse>> getAllStages() {
        List<BracketStageResponse> stages = stageService.getAllStages();
        return ResponseEntity.ok(stages);
    }

    @GetMapping("/tournament/{tournamentId}")
    public ResponseEntity<List<BracketStageResponse>> getStagesByTournamentId(@PathVariable UUID tournamentId) {
        List<BracketStageResponse> stages = stageService.getStagesByTournamentId(tournamentId);
        return ResponseEntity.ok(stages);
    }

    @PutMapping("/{id}")
    public ResponseEntity<BracketStageResponse> updateStage(
            @PathVariable UUID id,
            @RequestBody BracketStageUpdateRequest request
    ) {
        BracketStageResponse updatedStage = stageService.updateStage(id, request);
        return ResponseEntity.ok(updatedStage);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStage(@PathVariable UUID id) {
        stageService.deleteStage(id);
        return ResponseEntity.noContent().build();
    }
}