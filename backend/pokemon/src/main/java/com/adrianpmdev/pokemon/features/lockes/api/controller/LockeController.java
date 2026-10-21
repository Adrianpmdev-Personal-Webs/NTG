package com.adrianpmdev.pokemon.features.lockes.api.controller;


import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.adrianpmdev.pokemon.features.lockes.api.dto.request.LockeCreateRequest;
import com.adrianpmdev.pokemon.features.lockes.api.dto.request.LockeUpdateRequest;
import com.adrianpmdev.pokemon.features.lockes.api.dto.response.LockeResponse;
import com.adrianpmdev.pokemon.features.lockes.application.service.LockeService;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for managing lockes.
 *
 * @author adrianpmdev
 */
@RestController
@RequestMapping("/api/v1/pokemon/lockes")
@RequiredArgsConstructor
public class LockeController {

    private final LockeService lockeService;

    @PostMapping
    public ResponseEntity<LockeResponse> createLocke(@Valid @RequestBody LockeCreateRequest request) {
        LockeResponse createdLocke = lockeService.createLocke(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdLocke);
    }

    @GetMapping("/{id}")
    public ResponseEntity<LockeResponse> getLockeById(@PathVariable UUID id) {
        LockeResponse locke = lockeService.getLockeById(id);
        return ResponseEntity.ok(locke);
    }

    @GetMapping
    public ResponseEntity<List<LockeResponse>> getAllLockes() {
        List<LockeResponse> lockes = lockeService.getAllLockes();
        return ResponseEntity.ok(lockes);
    }

    @GetMapping("/game/{gameId}")
    public ResponseEntity<List<LockeResponse>> getLockesByGameId(@PathVariable UUID gameId) {
        List<LockeResponse> lockes = lockeService.getLockesByGameId(gameId);
        return ResponseEntity.ok(lockes);
    }

    @PutMapping("/{id}")
    public ResponseEntity<LockeResponse> updateLocke(
            @PathVariable UUID id,
            @RequestBody LockeUpdateRequest request
    ) {
        LockeResponse updatedLocke = lockeService.updateLocke(id, request);
        return ResponseEntity.ok(updatedLocke);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLocke(@PathVariable UUID id) {
        lockeService.deleteLocke(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/search")
    public ResponseEntity<List<LockeResponse>> searchLockes(@RequestParam String name) {
        List<LockeResponse> lockes = lockeService.searchLockesByName(name);
        return ResponseEntity.ok(lockes);
    }
}