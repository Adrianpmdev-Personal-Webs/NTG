package com.adrianpmdev.wowforever.features.classes.api.controller;

import com.adrianpmdev.wowforever.features.classes.api.dto.request.WowClassCreateRequest;
import com.adrianpmdev.wowforever.features.classes.api.dto.request.WowClassUpdateRequest;
import com.adrianpmdev.wowforever.features.classes.api.dto.response.WowClassResponse;
import com.adrianpmdev.wowforever.features.classes.application.service.WowClassService;
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
 * REST controller for managing WoW Forever classes. Reading needs the module tag; writing needs ADMIN.
 *
 * @author adrianpmdev
 */
@RestController
@RequestMapping("/api/v1/wow-forever/classes")
@RequiredArgsConstructor
public class WowClassController {

    private final WowClassService wowClassService;

    @GetMapping
    public ResponseEntity<List<WowClassResponse>> getAll() {
        return ResponseEntity.ok(wowClassService.getAll());
    }

    @PostMapping
    public ResponseEntity<WowClassResponse> create(@Valid @RequestBody WowClassCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(wowClassService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<WowClassResponse> update(@PathVariable UUID id, @Valid @RequestBody WowClassUpdateRequest request) {
        return ResponseEntity.ok(wowClassService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        wowClassService.delete(id);

        return ResponseEntity.noContent().build();
    }
}
