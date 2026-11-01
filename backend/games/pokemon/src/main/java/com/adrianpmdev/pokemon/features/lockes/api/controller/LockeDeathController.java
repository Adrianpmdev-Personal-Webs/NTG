package com.adrianpmdev.pokemon.features.lockes.api.controller;

import com.adrianpmdev.pokemon.features.lockes.api.dto.request.LockeDeathRequest;
import com.adrianpmdev.pokemon.features.lockes.api.dto.response.LockeDeathsResponse;
import com.adrianpmdev.pokemon.features.lockes.application.service.LockeDeathService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * The Pokémon lost in a randomlocke.
 *
 * @author adrianpmdev
 */
@RestController
@RequestMapping("/api/v1/pokemon/lockes/{lockeId}/deaths")
@RequiredArgsConstructor
public class LockeDeathController {

    private final LockeDeathService lockeDeathService;

    @GetMapping
    public LockeDeathsResponse list(@PathVariable UUID lockeId) {
        return lockeDeathService.list(lockeId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LockeDeathsResponse add(@PathVariable UUID lockeId, @Valid @RequestBody LockeDeathRequest request) {
        return lockeDeathService.add(lockeId, request);
    }

    @DeleteMapping("/{deathId}")
    public LockeDeathsResponse delete(@PathVariable UUID lockeId, @PathVariable UUID deathId) {
        return lockeDeathService.delete(lockeId, deathId);
    }
}
