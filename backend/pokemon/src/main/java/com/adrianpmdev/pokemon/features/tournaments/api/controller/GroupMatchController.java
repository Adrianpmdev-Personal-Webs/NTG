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

import com.adrianpmdev.pokemon.features.tournaments.api.dto.request.GroupMatchCreateRequest;
import com.adrianpmdev.pokemon.features.tournaments.api.dto.request.GroupMatchUpdateRequest;
import com.adrianpmdev.pokemon.features.tournaments.api.dto.response.GroupMatchResponse;
import com.adrianpmdev.pokemon.features.tournaments.application.service.GroupMatchService;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for managing group matches.
 *
 * @author adrianpmdev
 */
@RestController
@RequestMapping("/api/v1/pokemon/group-matches")
@RequiredArgsConstructor
public class GroupMatchController {

    private final GroupMatchService matchService;

    @PostMapping
    public ResponseEntity<GroupMatchResponse> createMatch(
            @Valid @RequestBody GroupMatchCreateRequest request
    ) {
        GroupMatchResponse createdMatch = matchService.createMatch(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdMatch);
    }

    @GetMapping("/{id}")
    public ResponseEntity<GroupMatchResponse> getMatchById(@PathVariable UUID id) {
        GroupMatchResponse match = matchService.getMatchById(id);
        return ResponseEntity.ok(match);
    }

    @GetMapping
    public ResponseEntity<List<GroupMatchResponse>> getAllMatches() {
        List<GroupMatchResponse> matches = matchService.getAllMatches();
        return ResponseEntity.ok(matches);
    }

    @GetMapping("/group/{groupId}")
    public ResponseEntity<List<GroupMatchResponse>> getMatchesByGroupId(@PathVariable UUID groupId) {
        List<GroupMatchResponse> matches = matchService.getMatchesByGroupId(groupId);
        return ResponseEntity.ok(matches);
    }

    @GetMapping("/group/{groupId}/status/{status}")
    public ResponseEntity<List<GroupMatchResponse>> getMatchesByGroupIdAndStatus(
            @PathVariable UUID groupId,
            @PathVariable String status
    ) {
        List<GroupMatchResponse> matches = matchService.getMatchesByGroupIdAndStatus(groupId, status);
        return ResponseEntity.ok(matches);
    }

    @PutMapping("/{id}")
    public ResponseEntity<GroupMatchResponse> updateMatch(
            @PathVariable UUID id,
            @RequestBody GroupMatchUpdateRequest request
    ) {
        GroupMatchResponse updatedMatch = matchService.updateMatch(id, request);
        return ResponseEntity.ok(updatedMatch);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMatch(@PathVariable UUID id) {
        matchService.deleteMatch(id);
        return ResponseEntity.noContent().build();
    }
}