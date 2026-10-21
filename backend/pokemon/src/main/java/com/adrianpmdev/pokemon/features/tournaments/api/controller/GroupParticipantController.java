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

import com.adrianpmdev.pokemon.features.tournaments.api.dto.request.GroupParticipantCreateRequest;
import com.adrianpmdev.pokemon.features.tournaments.api.dto.request.GroupParticipantUpdateRequest;
import com.adrianpmdev.pokemon.features.tournaments.api.dto.response.GroupParticipantResponse;
import com.adrianpmdev.pokemon.features.tournaments.application.service.GroupParticipantService;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for managing group participants.
 *
 * @author adrianpmdev
 */
@RestController
@RequestMapping("/api/v1/pokemon/group-participants")
@RequiredArgsConstructor
public class GroupParticipantController {

    private final GroupParticipantService participantService;

    @PostMapping
    public ResponseEntity<GroupParticipantResponse> createParticipant(
            @Valid @RequestBody GroupParticipantCreateRequest request
    ) {
        GroupParticipantResponse createdParticipant = participantService.createParticipant(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdParticipant);
    }

    @GetMapping("/{id}")
    public ResponseEntity<GroupParticipantResponse> getParticipantById(@PathVariable UUID id) {
        GroupParticipantResponse participant = participantService.getParticipantById(id);
        return ResponseEntity.ok(participant);
    }

    @GetMapping
    public ResponseEntity<List<GroupParticipantResponse>> getAllParticipants() {
        List<GroupParticipantResponse> participants = participantService.getAllParticipants();
        return ResponseEntity.ok(participants);
    }

    @GetMapping("/group/{groupId}")
    public ResponseEntity<List<GroupParticipantResponse>> getParticipantsByGroupId(@PathVariable UUID groupId) {
        List<GroupParticipantResponse> participants = participantService.getParticipantsByGroupId(groupId);
        return ResponseEntity.ok(participants);
    }

    @PutMapping("/{id}")
    public ResponseEntity<GroupParticipantResponse> updateParticipant(
            @PathVariable UUID id,
            @RequestBody GroupParticipantUpdateRequest request
    ) {
        GroupParticipantResponse updatedParticipant = participantService.updateParticipant(id, request);
        return ResponseEntity.ok(updatedParticipant);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteParticipant(@PathVariable UUID id) {
        participantService.deleteParticipant(id);
        return ResponseEntity.noContent().build();
    }
}