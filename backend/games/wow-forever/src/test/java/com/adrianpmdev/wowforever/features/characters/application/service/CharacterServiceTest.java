package com.adrianpmdev.wowforever.features.characters.application.service;

import com.adrianpmdev.platform.port.CurrentUser;
import com.adrianpmdev.platform.port.UserDirectory;
import com.adrianpmdev.wowforever.features.characters.api.dto.request.CharacterRequest;
import com.adrianpmdev.wowforever.features.characters.api.dto.response.CharactersResponse;
import com.adrianpmdev.wowforever.features.characters.infrastructure.persistence.entity.GameCharacter;
import com.adrianpmdev.wowforever.features.characters.infrastructure.persistence.repository.GameCharacterRepository;
import com.adrianpmdev.wowforever.features.gamedata.application.service.GameDataService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Who may change which character, the limit per account and what the list tells each caller.
 *
 * @author adrianpmdev
 */
class CharacterServiceTest {

    private static final UUID ANA = UUID.randomUUID();
    private static final UUID BRUNO = UUID.randomUUID();
    private static final UUID ADMIN = UUID.randomUUID();

    private final GameCharacterRepository repository = mock(GameCharacterRepository.class);
    private final CharacterRules rules = mock(CharacterRules.class);
    private final CurrentUser currentUser = mock(CurrentUser.class);
    private final UserDirectory directory = mock(UserDirectory.class);
    private final CharacterService service = new CharacterService(repository, rules, new GameDataService(),
            currentUser, directory);

    @BeforeEach
    void setUp() {
        Map<UUID, String> accounts = new LinkedHashMap<>();
        accounts.put(ANA, "Ana");
        accounts.put(BRUNO, "bruno");
        accounts.put(ADMIN, "Admin");

        when(directory.usernames(anyCollection())).thenAnswer(call -> {
            Map<UUID, String> found = new LinkedHashMap<>();

            for (Object id : call.<java.util.Collection<?>>getArgument(0)) {
                if (accounts.containsKey(id)) {
                    found.put((UUID) id, accounts.get(id));
                }
            }

            return found;
        });
        when(directory.usersWithTag("wow-forever")).thenReturn(Map.of(ANA, "Ana", BRUNO, "bruno"));
        when(repository.findAll()).thenReturn(List.of());
        signedInAs(ANA, false);
    }

    @Test
    void aPlayerCreatesTheirOwn() {
        service.create(request(null, " Thrall "));

        GameCharacter saved = saved();
        assertThat(saved.getUserId()).isEqualTo(ANA);
        assertThat(saved.getName()).isEqualTo("Thrall");
        assertThat(saved.getProfessions().path(0).path("slug").asText()).isEqualTo("alchemy");
        assertThat(saved.getProfessions().path(0).path("skill").asInt()).isEqualTo(300);
    }

    @Test
    void aPlayerCannotCreateForSomebodyElse() {
        assertForbidden(() -> service.create(request(BRUNO, "Thrall")));

        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void anAdminCreatesForAnotherAccount() {
        signedInAs(ADMIN, true);

        service.create(request(BRUNO, "Thrall"));

        assertThat(saved().getUserId()).isEqualTo(BRUNO);
    }

    @Test
    void anAdminCannotGiveACharacterToAnUnknownAccount() {
        signedInAs(ADMIN, true);

        assertThatThrownBy(() -> service.create(request(UUID.randomUUID(), "Thrall")))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void whatTheGameDoesNotAllowIsNotSaved() {
        doThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST)).when(rules).check(any());

        assertThatThrownBy(() -> service.create(request(null, "Thrall"))).isInstanceOf(ResponseStatusException.class);

        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void anAccountCannotHaveMoreThanTwentyCharacters() {
        when(repository.countByUserId(ANA)).thenReturn(20L);

        assertConflict(() -> service.create(request(null, "Thrall")));
    }

    @Test
    void anAccountCannotRepeatAName() {
        when(repository.existsByUserIdAndNameIgnoreCase(ANA, "Thrall")).thenReturn(true);

        assertConflict(() -> service.create(request(null, "Thrall")));
    }

    @Test
    void aPlayerUpdatesTheirOwnAndKeepsTheOwner() {
        GameCharacter character = stored(ANA, "Old");

        service.update(character.getId(), request(null, "New"));

        assertThat(saved().getName()).isEqualTo("New");
        assertThat(saved().getUserId()).isEqualTo(ANA);
    }

    @Test
    void aPlayerCannotUpdateSomebodyElses() {
        GameCharacter character = stored(BRUNO, "Old");

        assertForbidden(() -> service.update(character.getId(), request(null, "New")));
    }

    @Test
    void aPlayerCannotGiveAwayACharacter() {
        GameCharacter character = stored(ANA, "Old");

        assertForbidden(() -> service.update(character.getId(), request(BRUNO, "Old")));
    }

    @Test
    void anAdminCorrectsAnybodysAndKeepsTheOwner() {
        signedInAs(ADMIN, true);
        GameCharacter character = stored(BRUNO, "Old");

        service.update(character.getId(), request(null, "New"));

        assertThat(saved().getUserId()).isEqualTo(BRUNO);
    }

    @Test
    void movingACharacterRespectsTheLimitOfTheNewOwner() {
        signedInAs(ADMIN, true);
        GameCharacter character = stored(ANA, "Old");
        when(repository.countByUserId(BRUNO)).thenReturn(20L);

        assertConflict(() -> service.update(character.getId(), request(BRUNO, "Old")));
    }

    @Test
    void aPlayerDeletesTheirOwnButNotSomebodyElses() {
        GameCharacter own = stored(ANA, "Own");
        GameCharacter other = stored(BRUNO, "Other");

        service.delete(own.getId());
        assertForbidden(() -> service.delete(other.getId()));

        verify(repository).delete(own);
        verify(repository, never()).delete(other);
    }

    @Test
    void anAdminDeletesAnybodys() {
        signedInAs(ADMIN, true);
        GameCharacter character = stored(BRUNO, "Other");

        service.delete(character.getId());

        verify(repository).delete(character);
    }

    @Test
    void anUnknownCharacterIsNotFound() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(id)).isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void everybodySeesEveryCharacterSortedWithTheirOwnEditable() {
        UUID deleted = UUID.randomUUID();
        when(repository.findAll()).thenReturn(List.of(
                character(BRUNO, "Zul"), character(deleted, "Lost"), character(ANA, "Zeta"), character(ANA, "alba")));

        CharactersResponse response = service.list();

        assertThat(response.limit()).isEqualTo(20);
        assertThat(response.owners()).isEmpty();
        assertThat(response.characters()).extracting(CharactersResponse.Character::name)
                .containsExactly("alba", "Zeta", "Zul", "Lost");
        assertThat(response.characters()).extracting(CharactersResponse.Character::canEdit)
                .containsExactly(true, true, false, false);
        assertThat(response.characters().get(3).owner().username()).isNull();
    }

    @Test
    void everyCharacterLinksToTheArmoryWithItsNameInTheAddress() {
        when(repository.findAll()).thenReturn(List.of(character(ANA, "Shyleena Timewalker"), character(ANA, "Ña'ka")));

        assertThat(service.list().characters()).extracting(CharactersResponse.Character::armoryUrl).containsExactly(
                "https://worldofwarcraft.blizzard.com/es-es/forever/eu/armory?q=Shyleena+Timewalker",
                "https://worldofwarcraft.blizzard.com/es-es/forever/eu/armory?q=%C3%91a%27ka");
    }

    @Test
    void anAdminGetsTheAccountsWithTheTagAndTheirOwn() {
        signedInAs(ADMIN, true);

        assertThat(service.list().owners()).extracting(CharactersResponse.Owner::username)
                .containsExactly("Admin", "Ana", "bruno");
    }

    private void signedInAs(UUID account, boolean admin) {
        when(currentUser.id()).thenReturn(Optional.of(account));
        when(currentUser.isAdmin()).thenReturn(admin);
    }

    private GameCharacter saved() {
        ArgumentCaptor<GameCharacter> saved = ArgumentCaptor.forClass(GameCharacter.class);
        verify(repository).saveAndFlush(saved.capture());
        return saved.getValue();
    }

    private GameCharacter stored(UUID owner, String name) {
        GameCharacter character = character(owner, name);
        when(repository.findById(character.getId())).thenReturn(Optional.of(character));
        return character;
    }

    private static GameCharacter character(UUID owner, String name) {
        GameCharacter character = new GameCharacter();
        character.setId(UUID.randomUUID());
        character.setUserId(owner);
        character.setName(name);
        character.setClassSlug("warrior");
        character.setRaceSlug("orc");
        character.setLevel((short) 60);
        character.setProfessions(new ObjectMapper().createArrayNode());
        return character;
    }

    private static CharacterRequest request(UUID owner, String name) {
        return new CharacterRequest(owner, name, "warrior", "orc", 60,
                List.of(new CharacterRequest.Profession("alchemy", 300)), null);
    }

    private static void assertForbidden(org.assertj.core.api.ThrowableAssert.ThrowingCallable call) {
        assertThatThrownBy(call).isInstanceOfSatisfying(ResponseStatusException.class,
                exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    private static void assertConflict(org.assertj.core.api.ThrowableAssert.ThrowingCallable call) {
        assertThatThrownBy(call).isInstanceOfSatisfying(ResponseStatusException.class,
                exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
    }
}
