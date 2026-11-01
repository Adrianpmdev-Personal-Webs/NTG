package com.adrianpmdev.wowforever.features.characters.application.service;

import com.adrianpmdev.platform.error.ApiErrors;
import com.adrianpmdev.platform.port.CurrentUser;
import com.adrianpmdev.platform.port.UserDirectory;
import com.adrianpmdev.wowforever.WowForeverModule;
import com.adrianpmdev.wowforever.features.characters.api.dto.request.CharacterRequest;
import com.adrianpmdev.wowforever.features.characters.api.dto.response.CharactersResponse;
import com.adrianpmdev.wowforever.features.characters.infrastructure.persistence.entity.GameCharacter;
import com.adrianpmdev.wowforever.features.characters.infrastructure.persistence.repository.GameCharacterRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * The characters of the group: everybody with the tag sees all of them, a player changes their own and an admin can
 * correct anybody's or give one to another account. Every write answers with the updated list.
 *
 * @author adrianpmdev
 */
@Service
@RequiredArgsConstructor
public class CharacterService {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final int MAX_CHARACTERS_PER_ACCOUNT = 20;

    private final GameCharacterRepository characterRepository;
    private final CharacterRules characterRules;
    private final CurrentUser currentUser;
    private final UserDirectory userDirectory;

    /**
     * The characters of the group.
     *
     * @return characters with what the caller may change, and the accounts an admin can use
     */
    @Transactional(readOnly = true)
    public CharactersResponse list() {
        List<GameCharacter> characters = characterRepository.findAll();
        Set<UUID> ownerIds = characters.stream().map(GameCharacter::getUserId).collect(Collectors.toSet());
        Map<UUID, String> usernames = userDirectory.usernames(ownerIds);

        List<CharactersResponse.Character> rows = characters.stream()
                .map(character -> toResponse(character, usernames))
                .sorted(Comparator
                        .comparing((CharactersResponse.Character row) -> row.owner().username(),
                                Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                        .thenComparing(row -> row.owner().id())
                        .thenComparing(CharactersResponse.Character::name, String.CASE_INSENSITIVE_ORDER))
                .toList();

        return new CharactersResponse(MAX_CHARACTERS_PER_ACCOUNT, assignableOwners(), rows);
    }

    /**
     * Creates a character.
     *
     * @param request the character
     * @return the characters of the group
     */
    @Transactional
    public CharactersResponse create(CharacterRequest request) {
        UUID ownerId = chooseOwner(request.ownerId(), caller());

        characterRules.check(request);
        requireRoomFor(ownerId);

        if (characterRepository.existsByUserIdAndNameIgnoreCase(ownerId, request.name().trim())) {
            throw duplicatedName();
        }

        GameCharacter character = new GameCharacter();
        apply(character, ownerId, request);
        characterRepository.saveAndFlush(character);

        return list();
    }

    /**
     * Replaces a character.
     *
     * @param id      character id
     * @param request the character
     * @return the characters of the group
     */
    @Transactional
    public CharactersResponse update(UUID id, CharacterRequest request) {
        GameCharacter character = find(id);

        requireCanEdit(character);

        UUID ownerId = chooseOwner(request.ownerId(), character.getUserId());

        characterRules.check(request);

        if (!ownerId.equals(character.getUserId())) {
            requireRoomFor(ownerId);
        }

        if (characterRepository.existsByUserIdAndNameIgnoreCaseAndIdNot(ownerId, request.name().trim(), id)) {
            throw duplicatedName();
        }

        apply(character, ownerId, request);
        characterRepository.saveAndFlush(character);

        return list();
    }

    /**
     * Deletes a character.
     *
     * @param id character id
     * @return the characters of the group
     */
    @Transactional
    public CharactersResponse delete(UUID id) {
        GameCharacter character = find(id);

        requireCanEdit(character);
        characterRepository.delete(character);
        characterRepository.flush();

        return list();
    }

    private GameCharacter find(UUID id) {
        return characterRepository.findById(id).orElseThrow(() -> ApiErrors.notFound("Character"));
    }

    private UUID caller() {
        return currentUser.id().orElseThrow(ApiErrors::forbidden);
    }

    private boolean canEdit(GameCharacter character) {
        return currentUser.isAdmin() || currentUser.id().map(character.getUserId()::equals).orElse(false);
    }

    private void requireCanEdit(GameCharacter character) {
        if (!canEdit(character)) {
            throw ApiErrors.forbidden();
        }
    }

    /** The owner asked for, which only an admin can make another account, or the given one when none is asked. */
    private UUID chooseOwner(UUID asked, UUID fallback) {
        if (asked == null || asked.equals(fallback)) {
            return fallback;
        }

        if (!currentUser.isAdmin()) {
            throw ApiErrors.forbidden();
        }

        if (!userDirectory.usernames(List.of(asked)).containsKey(asked)) {
            throw ApiErrors.notFound("Account");
        }

        return asked;
    }

    private void requireRoomFor(UUID ownerId) {
        if (characterRepository.countByUserId(ownerId) >= MAX_CHARACTERS_PER_ACCOUNT) {
            throw ApiErrors.conflict("Solo se pueden tener " + MAX_CHARACTERS_PER_ACCOUNT + " personajes");
        }
    }

    private static RuntimeException duplicatedName() {
        return ApiErrors.conflict("Ya hay un personaje con ese nombre en esa cuenta");
    }

    private List<CharactersResponse.Owner> assignableOwners() {
        if (!currentUser.isAdmin()) {
            return List.of();
        }

        Map<UUID, String> owners = new LinkedHashMap<>(userDirectory.usersWithTag(WowForeverModule.TAG));

        currentUser.id()
                .filter(id -> !owners.containsKey(id))
                .ifPresent(id -> owners.putAll(userDirectory.usernames(List.of(id))));

        return owners.entrySet().stream()
                .map(owner -> new CharactersResponse.Owner(owner.getKey(), owner.getValue()))
                .sorted(Comparator.comparing(CharactersResponse.Owner::username, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private static void apply(GameCharacter character, UUID ownerId, CharacterRequest request) {
        character.setUserId(ownerId);
        character.setName(request.name().trim());
        character.setClassSlug(request.classSlug());
        character.setRaceSlug(request.raceSlug());
        character.setLevel(request.level().shortValue());
        character.setProfessions(JSON.valueToTree(request.professions()));
        character.setNotes(request.notes() == null || request.notes().isBlank() ? null : request.notes().trim());
    }

    private CharactersResponse.Character toResponse(GameCharacter character, Map<UUID, String> usernames) {
        List<CharactersResponse.Profession> professions = new ArrayList<>();

        for (JsonNode profession : character.getProfessions()) {
            JsonNode skill = profession.path("skill");

            professions.add(new CharactersResponse.Profession(profession.path("slug").asText(),
                    skill.isNumber() ? skill.asInt() : null));
        }

        return new CharactersResponse.Character(character.getId(),
                new CharactersResponse.Owner(character.getUserId(), usernames.get(character.getUserId())),
                character.getName(), character.getClassSlug(), character.getRaceSlug(), character.getLevel(),
                professions, character.getNotes(), canEdit(character));
    }
}
