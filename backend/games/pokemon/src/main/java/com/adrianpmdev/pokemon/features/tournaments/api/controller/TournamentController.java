package com.adrianpmdev.pokemon.features.tournaments.api.controller;

import com.adrianpmdev.pokemon.features.tournaments.api.dto.request.MatchCreateRequest;
import com.adrianpmdev.pokemon.features.tournaments.api.dto.request.RewardRequest;
import com.adrianpmdev.pokemon.features.tournaments.api.dto.request.ScheduleRequest;
import com.adrianpmdev.pokemon.features.tournaments.api.dto.request.TournamentCreateRequest;
import com.adrianpmdev.pokemon.features.tournaments.api.dto.request.TournamentPlayerAddRequest;
import com.adrianpmdev.pokemon.features.tournaments.api.dto.request.TournamentUpdateRequest;
import com.adrianpmdev.pokemon.features.tournaments.api.dto.response.TournamentDetailResponse;
import com.adrianpmdev.pokemon.features.tournaments.application.service.MatchService;
import com.adrianpmdev.pokemon.features.tournaments.application.service.RewardService;
import com.adrianpmdev.pokemon.features.tournaments.application.service.ScheduleService;
import com.adrianpmdev.pokemon.features.tournaments.application.service.TournamentPlayerService;
import com.adrianpmdev.pokemon.features.tournaments.application.service.TournamentQueryService;
import com.adrianpmdev.pokemon.features.tournaments.application.service.TournamentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Tournaments. GET returns the whole tournament page; every admin action answers with the updated
 * tournament, so the client never has to put several responses together.
 *
 * @author adrianpmdev
 */
@RestController
@RequestMapping("/api/v1/pokemon/tournaments")
@RequiredArgsConstructor
public class TournamentController {

    private final TournamentQueryService queryService;
    private final TournamentService tournamentService;
    private final TournamentPlayerService tournamentPlayerService;
    private final ScheduleService scheduleService;
    private final MatchService matchService;
    private final RewardService rewardService;

    @GetMapping("/{id}")
    public TournamentDetailResponse get(@PathVariable UUID id) {
        return queryService.detail(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TournamentDetailResponse create(@Valid @RequestBody TournamentCreateRequest request) {
        return tournamentService.create(request);
    }

    @PutMapping("/{id}")
    public TournamentDetailResponse update(@PathVariable UUID id, @Valid @RequestBody TournamentUpdateRequest request) {
        return tournamentService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        tournamentService.delete(id);
    }

    /** Adds a player of the platform list to the tournament. */
    @PostMapping("/{id}/players")
    public TournamentDetailResponse addPlayer(@PathVariable UUID id, @Valid @RequestBody TournamentPlayerAddRequest request) {
        return tournamentPlayerService.add(id, request.playerId());
    }

    /** Generates the league, or the groups and their series, replacing the previous calendar. */
    @PostMapping("/{id}/schedule")
    public TournamentDetailResponse generateCalendar(@PathVariable UUID id, @RequestBody ScheduleRequest request) {
        scheduleService.generateCalendar(id, request.randomGroups());
        return queryService.detail(id);
    }

    /** Generates the double-elimination bracket from the final group tables. */
    @PostMapping("/{id}/bracket")
    public TournamentDetailResponse generateBracket(@PathVariable UUID id) {
        scheduleService.generateBracket(id);
        return queryService.detail(id);
    }

    /** Adds a series by hand. */
    @PostMapping("/{id}/matches")
    public TournamentDetailResponse addMatch(@PathVariable UUID id, @Valid @RequestBody MatchCreateRequest request) {
        return matchService.create(id, request);
    }

    /** Adds a reward. */
    @PostMapping("/{id}/rewards")
    public TournamentDetailResponse addReward(@PathVariable UUID id, @Valid @RequestBody RewardRequest request) {
        return rewardService.create(id, request);
    }
}
