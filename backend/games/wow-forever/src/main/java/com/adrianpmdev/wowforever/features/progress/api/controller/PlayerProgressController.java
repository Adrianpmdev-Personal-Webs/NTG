package com.adrianpmdev.wowforever.features.progress.api.controller;

import com.adrianpmdev.wowforever.features.progress.api.dto.request.LegacyChallengesRequest;
import com.adrianpmdev.wowforever.features.progress.api.dto.request.SavedBuildRequest;
import com.adrianpmdev.wowforever.features.progress.api.dto.response.SavedBuildResponse;
import com.adrianpmdev.wowforever.features.progress.application.service.PlayerProgressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * What the caller keeps in WoW Forever: their talent builds and their Legacy challenges. Every route acts on the caller,
 * so nobody can reach what somebody else keeps.
 *
 * @author adrianpmdev
 */
@RestController
@RequestMapping("/api/v1/wow-forever/me")
@RequiredArgsConstructor
public class PlayerProgressController {

    private final PlayerProgressService service;

    @GetMapping("/builds/{classSlug}")
    public List<SavedBuildResponse> builds(@PathVariable String classSlug) {
        return service.builds(classSlug);
    }

    @PutMapping("/builds/{classSlug}/{slot}")
    public List<SavedBuildResponse> saveBuild(@PathVariable String classSlug, @PathVariable int slot,
                                              @Valid @RequestBody SavedBuildRequest request) {
        return service.saveBuild(classSlug, slot, request);
    }

    @DeleteMapping("/builds/{classSlug}/{slot}")
    public List<SavedBuildResponse> deleteBuild(@PathVariable String classSlug, @PathVariable int slot) {
        return service.deleteBuild(classSlug, slot);
    }

    @GetMapping("/legacy-challenges")
    public List<String> challenges() {
        return service.challenges();
    }

    @PutMapping("/legacy-challenges")
    public List<String> setChallenges(@Valid @RequestBody LegacyChallengesRequest request) {
        return service.setChallenges(request);
    }
}
