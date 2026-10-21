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

import com.adrianpmdev.pokemon.features.rewards.api.dto.request.RewardCreateRequest;
import com.adrianpmdev.pokemon.features.rewards.api.dto.request.RewardUpdateRequest;
import com.adrianpmdev.pokemon.features.rewards.api.dto.response.RewardResponse;
import com.adrianpmdev.pokemon.features.rewards.application.service.RewardService;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for managing rewards.
 *
 * @author adrianpmdev
 */
@RestController
@RequestMapping("/api/v1/pokemon/rewards")
@RequiredArgsConstructor
public class RewardController {

    private final RewardService rewardService;

    @PostMapping
    public ResponseEntity<RewardResponse> createReward(@Valid @RequestBody RewardCreateRequest request) {
        RewardResponse createdReward = rewardService.createReward(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdReward);
    }

    @GetMapping("/{id}")
    public ResponseEntity<RewardResponse> getRewardById(@PathVariable UUID id) {
        RewardResponse reward = rewardService.getRewardById(id);
        return ResponseEntity.ok(reward);
    }

    @GetMapping
    public ResponseEntity<List<RewardResponse>> getAllRewards() {
        List<RewardResponse> rewards = rewardService.getAllRewards();
        return ResponseEntity.ok(rewards);
    }

    @GetMapping("/tournament/{tournamentId}")
    public ResponseEntity<List<RewardResponse>> getRewardsByTournamentId(@PathVariable UUID tournamentId) {
        List<RewardResponse> rewards = rewardService.getRewardsByTournamentId(tournamentId);
        return ResponseEntity.ok(rewards);
    }

    @PutMapping("/{id}")
    public ResponseEntity<RewardResponse> updateReward(
            @PathVariable UUID id,
            @RequestBody RewardUpdateRequest request
    ) {
        RewardResponse updatedReward = rewardService.updateReward(id, request);
        return ResponseEntity.ok(updatedReward);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReward(@PathVariable UUID id) {
        rewardService.deleteReward(id);
        return ResponseEntity.noContent().build();
    }
}