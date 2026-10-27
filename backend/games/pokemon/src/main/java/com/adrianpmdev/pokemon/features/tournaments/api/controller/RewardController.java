package com.adrianpmdev.pokemon.features.tournaments.api.controller;

import com.adrianpmdev.pokemon.features.tournaments.api.dto.request.RewardRequest;
import com.adrianpmdev.pokemon.features.tournaments.api.dto.response.TournamentDetailResponse;
import com.adrianpmdev.pokemon.features.tournaments.application.service.RewardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Rewards of a tournament (admin only).
 *
 * @author adrianpmdev
 */
@RestController
@RequestMapping("/api/v1/pokemon/rewards")
@RequiredArgsConstructor
public class RewardController {

    private final RewardService rewardService;

    @PutMapping("/{id}")
    public TournamentDetailResponse update(@PathVariable UUID id, @Valid @RequestBody RewardRequest request) {
        return rewardService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public TournamentDetailResponse delete(@PathVariable UUID id) {
        return rewardService.delete(id);
    }
}
