package com.adrianpmdev.wowforever.features.progress.application.service;

import com.adrianpmdev.platform.error.ApiErrors;
import com.adrianpmdev.platform.port.CurrentUser;
import com.adrianpmdev.wowforever.features.gamedata.application.service.GameDataService;
import com.adrianpmdev.wowforever.features.progress.api.dto.request.LegacyChallengesRequest;
import com.adrianpmdev.wowforever.features.progress.api.dto.request.SavedBuildRequest;
import com.adrianpmdev.wowforever.features.progress.api.dto.response.SavedBuildResponse;
import com.adrianpmdev.wowforever.features.progress.infrastructure.persistence.PlayerProgressRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * What each player keeps in WoW Forever, always on their own account: five talent builds per class and the Legacy
 * challenges they ticked. A build is not checked against the talents, because a game patch can change them and the slot
 * must stay; the screen marks one that no longer fits.
 *
 * @author adrianpmdev
 */
@Service
@RequiredArgsConstructor
public class PlayerProgressService {

    /** Slots per class. */
    static final int SLOTS = 5;
    /** More than the game has, only to keep an account from filling the table. */
    static final int MAX_CHALLENGES = 500;

    private static final ObjectMapper JSON = new ObjectMapper();

    private final PlayerProgressRepository repository;
    private final GameDataService gameDataService;
    private final CurrentUser currentUser;

    /**
     * The builds of the caller in a class.
     *
     * @param classSlug class
     * @return one entry per slot, null when it is empty
     */
    public List<SavedBuildResponse> builds(String classSlug) {
        Map<Integer, SavedBuildResponse> builds = repository.builds(caller(), classSlug);
        List<SavedBuildResponse> slots = new ArrayList<>();

        for (int slot = 0; slot < SLOTS; slot++) {
            slots.add(builds.get(slot));
        }

        return slots;
    }

    /**
     * Keeps a build of the caller.
     *
     * @param classSlug class, which must exist in the game data
     * @param slot      slot
     * @param request   the build
     * @return the builds of that class
     */
    public List<SavedBuildResponse> saveBuild(String classSlug, int slot, SavedBuildRequest request) {
        requireSlot(slot);
        requireClass(classSlug);
        repository.saveBuild(caller(), classSlug, slot, request.name().trim(), request.level(), request.build());

        return builds(classSlug);
    }

    /**
     * Empties a slot of the caller. The class is not checked, so a slot of a class the game removed can still be emptied.
     *
     * @param classSlug class
     * @param slot      slot
     * @return the builds of that class
     */
    public List<SavedBuildResponse> deleteBuild(String classSlug, int slot) {
        requireSlot(slot);
        repository.deleteBuild(caller(), classSlug, slot);

        return builds(classSlug);
    }

    /**
     * The challenges the caller ticked.
     *
     * @return names of the challenges
     */
    public List<String> challenges() {
        return repository.challenges(caller());
    }

    /**
     * Ticks or unticks challenges of the caller.
     *
     * @param request the challenges
     * @return every challenge the caller has ticked
     */
    @Transactional
    public List<String> setChallenges(LegacyChallengesRequest request) {
        UUID caller = caller();
        List<String> names = request.names().stream().map(String::trim).distinct().toList();

        if (!request.completed()) {
            repository.removeChallenges(caller, names);
        } else if (repository.challenges(caller).size() + names.size() > MAX_CHALLENGES) {
            throw ApiErrors.conflict("Hay demasiados retos marcados");
        } else {
            repository.addChallenges(caller, names);
        }

        return repository.challenges(caller);
    }

    private void requireSlot(int slot) {
        if (slot < 0 || slot >= SLOTS) {
            throw ApiErrors.badRequest("Ese hueco no existe");
        }
    }

    private void requireClass(String classSlug) {
        for (JsonNode gameClass : read(gameDataService.getIndex()).path("classes")) {
            if (gameClass.path("slug").asText().equals(classSlug)) {
                return;
            }
        }

        throw ApiErrors.badRequest("Esa clase no existe");
    }

    private UUID caller() {
        return currentUser.id().orElseThrow(ApiErrors::forbidden);
    }

    private static JsonNode read(String json) {
        try {
            return JSON.readTree(json);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("The game data could not be read", exception);
        }
    }
}
