package com.adrianpmdev.pokemon.features.rewards.api.controller;


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

import com.adrianpmdev.pokemon.features.rewards.api.dto.request.UserRewardCreateRequest;
import com.adrianpmdev.pokemon.features.rewards.api.dto.request.UserRewardUpdateRequest;
import com.adrianpmdev.pokemon.features.rewards.api.dto.response.UserRewardResponse;
import com.adrianpmdev.pokemon.features.rewards.application.service.UserRewardService;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for managing user rewards.
 *
 * @author adrianpmdev
 */
@RestController
@RequestMapping("/api/v1/pokemon/user-rewards")
@RequiredArgsConstructor
public class UserRewardController {

    private final UserRewardService userRewardService;

    @PostMapping
    public ResponseEntity<UserRewardResponse> claimReward(
            @Valid @RequestBody UserRewardCreateRequest request
    ) {
        UserRewardResponse claimedReward = userRewardService.claimReward(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(claimedReward);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserRewardResponse> getRewardById(@PathVariable UUID id) {
        UserRewardResponse userReward = userRewardService.getRewardById(id);
        return ResponseEntity.ok(userReward);
    }

    @GetMapping
    public ResponseEntity<List<UserRewardResponse>> getAllRewards() {
        List<UserRewardResponse> rewards = userRewardService.getAllRewards();
        return ResponseEntity.ok(rewards);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<UserRewardResponse>> getRewardsByUserId(@PathVariable UUID userId) {
        List<UserRewardResponse> rewards = userRewardService.getRewardsByUserId(userId);
        return ResponseEntity.ok(rewards);
    }

    @GetMapping("/tournament/{tournamentId}")
    public ResponseEntity<List<UserRewardResponse>> getRewardsByTournamentId(
            @PathVariable UUID tournamentId
    ) {
        List<UserRewardResponse> rewards = userRewardService.getRewardsByTournamentId(tournamentId);
        return ResponseEntity.ok(rewards);
    }

    @GetMapping("/user/{userId}/tournament/{tournamentId}")
    public ResponseEntity<List<UserRewardResponse>> getRewardsByUserAndTournament(
            @PathVariable UUID userId,
            @PathVariable UUID tournamentId
    ) {
        List<UserRewardResponse> rewards = userRewardService.getRewardsByUserAndTournament(
                userId,
                tournamentId
        );
        return ResponseEntity.ok(rewards);
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserRewardResponse> updateReward(
            @PathVariable UUID id,
            @RequestBody UserRewardUpdateRequest request
    ) {
        UserRewardResponse updatedReward = userRewardService.updateReward(id, request);
        return ResponseEntity.ok(updatedReward);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReward(@PathVariable UUID id) {
        userRewardService.deleteReward(id);
        return ResponseEntity.noContent().build();
    }
}