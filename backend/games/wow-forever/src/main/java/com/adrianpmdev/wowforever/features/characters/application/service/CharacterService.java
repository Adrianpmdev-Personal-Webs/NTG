package com.adrianpmdev.wowforever.features.characters.application.service;

import com.adrianpmdev.platform.error.ApiErrors;
import com.adrianpmdev.platform.port.CurrentUser;
import com.adrianpmdev.platform.port.UserDirectory;
import com.adrianpmdev.wowforever.WowForeverModule;
import com.adrianpmdev.wowforever.features.characters.api.dto.request.CharacterRequest;
import com.adrianpmdev.wowforever.features.characters.api.dto.response.CharacterSheetResponse;
import com.adrianpmdev.wowforever.features.characters.api.dto.response.CharacterSummary;
import com.adrianpmdev.wowforever.features.characters.api.dto.response.CharactersResponse;
import com.adrianpmdev.wowforever.features.characters.domain.ArmorySheet;
import com.adrianpmdev.wowforever.features.characters.infrastructure.armory.ArmoryClient;
import com.adrianpmdev.wowforever.features.characters.infrastructure.persistence.entity.GameCharacter;
import com.adrianpmdev.wowforever.features.characters.infrastructure.persistence.repository.GameCharacterRepository;
import com.adrianpmdev.wowforever.features.gamedata.application.service.GameDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * The characters of the group: everybody with the tag sees all of them, a player changes their own and an admin can
 * correct anybody's or give one to another account. Only the name and the owner are stored; everything else is read
 * from the official armory when somebody looks (ArmoryReader), never inside a transaction, so no read holds a database
 * connection. Every write answers with the updated list.
 *
 * @author adrianpmdev
 */
@Service
@RequiredArgsConstructor
public class CharacterService {

    private static final int MAX_CHARACTERS_PER_ACCOUNT = 20;

    private final GameCharacterRepository characterRepository;
    private final ArmoryReader armoryReader;
    private final GameDataService gameDataService;
    private final CurrentUser currentUser;
    private final UserDirectory userDirectory;

    /**
     * The characters of the group, without what the armory says.
     *
     * @return characters with what the caller may change, and the accounts an admin can use
     */
    @Transactional(readOnly = true)
    public CharactersResponse list() {
        List<GameCharacter> characters = characterRepository.findAll();
        Set<UUID> ownerIds = characters.stream().map(GameCharacter::getUserId).collect(Collectors.toSet());
        Map<UUID, String> usernames = userDirectory.usernames(ownerIds);

        List<CharactersResponse.Character> rows = characters.stream()
                .map(character -> new CharactersResponse.Character(character.getId(), ownerOf(character, usernames),
                        character.getName(), armoryUrl(character), canEdit(character)))
                .sorted(Comparator
                        .comparing((CharactersResponse.Character row) -> row.owner().username(),
                                Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                        .thenComparing(row -> row.owner().id())
                        .thenComparing(CharactersResponse.Character::name, String.CASE_INSENSITIVE_ORDER))
                .toList();

        return new CharactersResponse(currentUser.id().orElse(null), MAX_CHARACTERS_PER_ACCOUNT, assignableOwners(),
                rows);
    }

    /**
     * What the armory says of the characters of a player, in short.
     *
     * @param ownerId account
     * @return one summary per character
     */
    public List<CharacterSummary> summaries(UUID ownerId) {
        List<GameCharacter> characters = characterRepository.findByUserIdOrderByNameAsc(ownerId);
        Map<String, ArmoryReader.Result> results = armoryReader.readAll(
                characters.stream().map(GameCharacter::getName).toList());

        return characters.stream()
                .map(character -> summaryOf(character.getId(), results.get(character.getName())))
                .toList();
    }

    /**
     * The whole sheet of a character.
     *
     * @param id character id
     * @return the character with what the armory says
     */
    public CharacterSheetResponse sheet(UUID id) {
        GameCharacter character = find(id);
        ArmoryReader.Result result = armoryReader.read(character.getName());
        Map<UUID, String> usernames = userDirectory.usernames(List.of(character.getUserId()));

        return new CharacterSheetResponse(character.getId(), character.getName(), ownerOf(character, usernames),
                armoryUrl(character), result.status().name(), result.sheet());
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
        String name = request.name().trim();

        requireRoomFor(ownerId);

        if (characterRepository.existsByNameIgnoreCase(name)) {
            throw duplicatedName();
        }

        GameCharacter character = new GameCharacter();
        character.setUserId(ownerId);
        character.setName(name);
        characterRepository.saveAndFlush(character);

        return list();
    }

    /**
     * Renames a character or gives it to another account.
     *
     * @param id      character id
     * @param request the character
     * @return the characters of the group
     */
    @Transactional
    public CharactersResponse update(UUID id, CharacterRequest request) {
        GameCharacter character = find(id);
        String name = request.name().trim();

        requireCanEdit(character);

        UUID ownerId = chooseOwner(request.ownerId(), character.getUserId());

        if (!ownerId.equals(character.getUserId())) {
            requireRoomFor(ownerId);
        }

        if (!name.equalsIgnoreCase(character.getName()) && characterRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw duplicatedName();
        }

        character.setUserId(ownerId);
        character.setName(name);
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

    private static CharacterSummary summaryOf(UUID id, ArmoryReader.Result result) {
        ArmorySheet sheet = result.sheet();

        if (sheet == null) {
            return new CharacterSummary(id, result.status().name(), null, null, null, null, null, List.of());
        }

        return new CharacterSummary(id, result.status().name(), sheet.level(), sheet.className(), sheet.classSlug(),
                sheet.raceName(), sheet.raceSlug(), sheet.professions().stream()
                .map(known -> new CharacterSummary.Profession(known.slug(), known.name(), known.skill(),
                        known.maxSkill(), known.icon()))
                .toList());
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
        return ApiErrors.conflict("Ese personaje ya está en el grupo");
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

    private static CharactersResponse.Owner ownerOf(GameCharacter character, Map<UUID, String> usernames) {
        return new CharactersResponse.Owner(character.getUserId(), usernames.get(character.getUserId()));
    }

    private String armoryUrl(GameCharacter character) {
        return gameDataService.getConfig().armory().url() + "/character/" + ArmoryClient.slugOf(character.getName());
    }
}
