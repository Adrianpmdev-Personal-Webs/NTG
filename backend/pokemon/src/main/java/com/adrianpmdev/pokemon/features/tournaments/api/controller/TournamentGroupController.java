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

import com.adrianpmdev.pokemon.features.tournaments.api.dto.request.TournamentGroupCreateRequest;
import com.adrianpmdev.pokemon.features.tournaments.api.dto.request.TournamentGroupUpdateRequest;
import com.adrianpmdev.pokemon.features.tournaments.api.dto.response.TournamentGroupResponse;
import com.adrianpmdev.pokemon.features.tournaments.application.service.TournamentGroupService;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for managing tournament groups.
 *
 * @author adrianpmdev
 */
@RestController
@RequestMapping("/api/v1/pokemon/tournament-groups")
@RequiredArgsConstructor
public class TournamentGroupController {

    private final TournamentGroupService groupService;

    @PostMapping
    public ResponseEntity<TournamentGroupResponse> createGroup(
            @Valid @RequestBody TournamentGroupCreateRequest request
    ) {
        TournamentGroupResponse createdGroup = groupService.createGroup(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdGroup);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TournamentGroupResponse> getGroupById(@PathVariable UUID id) {
        TournamentGroupResponse group = groupService.getGroupById(id);
        return ResponseEntity.ok(group);
    }

    @GetMapping
    public ResponseEntity<List<TournamentGroupResponse>> getAllGroups() {
        List<TournamentGroupResponse> groups = groupService.getAllGroups();
        return ResponseEntity.ok(groups);
    }

    @GetMapping("/tournament/{tournamentId}")
    public ResponseEntity<List<TournamentGroupResponse>> getGroupsByTournamentId(@PathVariable UUID tournamentId) {
        List<TournamentGroupResponse> groups = groupService.getGroupsByTournamentId(tournamentId);
        return ResponseEntity.ok(groups);
    }

    @GetMapping("/tournament/{tournamentId}/letter/{groupLetter}")
    public ResponseEntity<TournamentGroupResponse> getGroupByTournamentAndLetter(
            @PathVariable UUID tournamentId,
            @PathVariable String groupLetter
    ) {
        TournamentGroupResponse group = groupService.getGroupByTournamentAndLetter(tournamentId, groupLetter);
        return ResponseEntity.ok(group);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TournamentGroupResponse> updateGroup(
            @PathVariable UUID id,
            @RequestBody TournamentGroupUpdateRequest request
    ) {
        TournamentGroupResponse updatedGroup = groupService.updateGroup(id, request);
        return ResponseEntity.ok(updatedGroup);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGroup(@PathVariable UUID id) {
        groupService.deleteGroup(id);
        return ResponseEntity.noContent().build();
    }
}