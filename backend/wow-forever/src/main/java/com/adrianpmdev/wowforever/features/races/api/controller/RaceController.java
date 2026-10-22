package com.adrianpmdev.wowforever.features.races.api.controller;

import com.adrianpmdev.wowforever.features.races.api.dto.request.RaceCreateRequest;
import com.adrianpmdev.wowforever.features.races.api.dto.request.RaceUpdateRequest;
import com.adrianpmdev.wowforever.features.races.api.dto.response.RaceResponse;
import com.adrianpmdev.wowforever.features.races.application.service.RaceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for managing WoW Forever races. Reading needs the module tag; writing needs ADMIN.
 *
 * @author adrianpmdev
 */
@RestController
@RequestMapping("/api/v1/wow-forever/races")
@RequiredArgsConstructor
public class RaceController {

    private final RaceService raceService;

    @GetMapping
    public ResponseEntity<List<RaceResponse>> getAll() {
        return ResponseEntity.ok(raceService.getAll());
    }

    @PostMapping
    public ResponseEntity<RaceResponse> create(@Valid @RequestBody RaceCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(raceService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<RaceResponse> update(@PathVariable UUID id, @Valid @RequestBody RaceUpdateRequest request) {
        return ResponseEntity.ok(raceService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        raceService.delete(id);

        return ResponseEntity.noContent().build();
    }
}
