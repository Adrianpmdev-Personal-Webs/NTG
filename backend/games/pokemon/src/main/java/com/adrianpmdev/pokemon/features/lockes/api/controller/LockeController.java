package com.adrianpmdev.pokemon.features.lockes.api.controller;

import com.adrianpmdev.pokemon.features.lockes.api.dto.request.LockeRequest;
import com.adrianpmdev.pokemon.features.lockes.api.dto.request.LockeRosterRequest;
import com.adrianpmdev.pokemon.features.lockes.api.dto.response.LockeDetailResponse;
import com.adrianpmdev.pokemon.features.lockes.api.dto.response.LockeSummaryResponse;
import com.adrianpmdev.pokemon.features.lockes.application.service.LockeService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Randomlockes.
 *
 * @author adrianpmdev
 */
@RestController
@RequestMapping("/api/v1/pokemon/lockes")
@RequiredArgsConstructor
public class LockeController {

    private final LockeService lockeService;

    /** Every randomlocke, or only the ones played in a game ({@code ?gameId=}). */
    @GetMapping
    public List<LockeSummaryResponse> list(@RequestParam(required = false) UUID gameId) {
        return lockeService.list(gameId);
    }

    @GetMapping("/{id}")
    public LockeDetailResponse get(@PathVariable UUID id) {
        return lockeService.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LockeDetailResponse create(@Valid @RequestBody LockeRequest request) {
        return lockeService.create(request);
    }

    @PutMapping("/{id}")
    public LockeDetailResponse update(@PathVariable UUID id, @Valid @RequestBody LockeRequest request) {
        return lockeService.update(id, request);
    }

    @PutMapping("/{id}/players")
    public LockeDetailResponse setRoster(@PathVariable UUID id, @Valid @RequestBody LockeRosterRequest request) {
        return lockeService.setRoster(id, request.playerIds());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        lockeService.delete(id);
    }
}
