package com.adrianpmdev.wowforever.features.characters.api.controller;

import com.adrianpmdev.wowforever.features.characters.api.dto.request.CharacterRequest;
import com.adrianpmdev.wowforever.features.characters.api.dto.response.CharacterSheetResponse;
import com.adrianpmdev.wowforever.features.characters.api.dto.response.CharacterSummary;
import com.adrianpmdev.wowforever.features.characters.api.dto.response.CharactersResponse;
import com.adrianpmdev.wowforever.features.characters.application.service.CharacterService;
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
 * The characters of the group, and what the official armory says of them.
 *
 * @author adrianpmdev
 */
@RestController
@RequestMapping("/api/v1/wow-forever/characters")
@RequiredArgsConstructor
public class CharacterController {

    private final CharacterService characterService;

    @GetMapping
    public CharactersResponse list() {
        return characterService.list();
    }

    @GetMapping("/summaries")
    public List<CharacterSummary> summaries(@RequestParam UUID owner) {
        return characterService.summaries(owner);
    }

    @GetMapping("/{id}/armory")
    public CharacterSheetResponse sheet(@PathVariable UUID id) {
        return characterService.sheet(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CharactersResponse create(@Valid @RequestBody CharacterRequest request) {
        return characterService.create(request);
    }

    @PutMapping("/{id}")
    public CharactersResponse update(@PathVariable UUID id, @Valid @RequestBody CharacterRequest request) {
        return characterService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public CharactersResponse delete(@PathVariable UUID id) {
        return characterService.delete(id);
    }
}
