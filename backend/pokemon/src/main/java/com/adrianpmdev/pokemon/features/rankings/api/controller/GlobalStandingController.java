package com.adrianpmdev.pokemon.features.rankings.api.controller;


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

import com.adrianpmdev.pokemon.features.rankings.api.dto.request.GlobalStandingCreateRequest;
import com.adrianpmdev.pokemon.features.rankings.api.dto.request.GlobalStandingUpdateRequest;
import com.adrianpmdev.pokemon.features.rankings.api.dto.response.GlobalStandingResponse;
import com.adrianpmdev.pokemon.features.rankings.application.service.GlobalStandingService;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for managing global standings.
 *
 * @author adrianpmdev
 */
@RestController
@RequestMapping("/api/v1/pokemon/global-standings")
@RequiredArgsConstructor
public class GlobalStandingController {

    private final GlobalStandingService standingService;

    @PostMapping
    public ResponseEntity<GlobalStandingResponse> createStanding(
            @Valid @RequestBody GlobalStandingCreateRequest request
    ) {
        GlobalStandingResponse createdStanding = standingService.createStanding(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdStanding);
    }

    @GetMapping("/{id}")
    public ResponseEntity<GlobalStandingResponse> getStandingById(@PathVariable UUID id) {
        GlobalStandingResponse standing = standingService.getStandingById(id);
        return ResponseEntity.ok(standing);
    }

    @GetMapping
    public ResponseEntity<List<GlobalStandingResponse>> getAllStandings() {
        List<GlobalStandingResponse> standings = standingService.getAllStandings();
        return ResponseEntity.ok(standings);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<GlobalStandingResponse> getStandingByUserId(@PathVariable UUID userId) {
        GlobalStandingResponse standing = standingService.getStandingByUserId(userId);
        return ResponseEntity.ok(standing);
    }

    @GetMapping("/ranking")
    public ResponseEntity<List<GlobalStandingResponse>> getRankingByPosition() {
        List<GlobalStandingResponse> standings = standingService.getRankingByPosition();
        return ResponseEntity.ok(standings);
    }

    @PutMapping("/{id}")
    public ResponseEntity<GlobalStandingResponse> updateStanding(
            @PathVariable UUID id,
            @RequestBody GlobalStandingUpdateRequest request
    ) {
        GlobalStandingResponse updatedStanding = standingService.updateStanding(id, request);
        return ResponseEntity.ok(updatedStanding);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStanding(@PathVariable UUID id) {
        standingService.deleteStanding(id);
        return ResponseEntity.noContent().build();
    }
}