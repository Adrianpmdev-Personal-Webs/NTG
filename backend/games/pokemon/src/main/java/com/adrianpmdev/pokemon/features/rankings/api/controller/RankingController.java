package com.adrianpmdev.pokemon.features.rankings.api.controller;

import com.adrianpmdev.pokemon.features.rankings.api.dto.response.PlayerStandingResponse;
import com.adrianpmdev.pokemon.features.rankings.application.service.RankingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Global all-time ranking of every randomlocke. The standings of a single randomlocke come with it.
 *
 * @author adrianpmdev
 */
@RestController
@RequestMapping("/api/v1/pokemon/ranking")
@RequiredArgsConstructor
public class RankingController {

    private final RankingService rankingService;

    @GetMapping
    public List<PlayerStandingResponse> globalRanking() {
        return rankingService.globalRanking();
    }
}
