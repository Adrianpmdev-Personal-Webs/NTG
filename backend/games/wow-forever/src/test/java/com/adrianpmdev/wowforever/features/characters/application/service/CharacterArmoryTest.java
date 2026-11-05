package com.adrianpmdev.wowforever.features.characters.application.service;

import com.adrianpmdev.wowforever.features.characters.infrastructure.armory.ArmoryCharacter;
import com.adrianpmdev.wowforever.features.characters.infrastructure.armory.ArmoryClient;
import com.adrianpmdev.wowforever.features.characters.infrastructure.armory.ArmoryMatch;
import com.adrianpmdev.wowforever.features.characters.infrastructure.persistence.entity.GameCharacter;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * When the page of a character is read and what is kept from it.
 *
 * @author adrianpmdev
 */
class CharacterArmoryTest {

    private static final Instant THEN = Instant.ofEpochSecond(1000);
    private static final Instant NOW = Instant.ofEpochSecond(2000);

    private final ArmoryClient client = mock(ArmoryClient.class);
    private final CharacterArmory armory = new CharacterArmory(client);

    @Test
    void aCharacterTheArmoryDoesNotHaveHasNoChanges() throws IOException {
        when(client.search("Alba Sol")).thenReturn(Optional.empty());

        assertThat(armory.changesOf(character())).isEmpty();
        verify(client, never()).read(any());
    }

    @Test
    void aCharacterThatDidNotChangeIsNotReadAgain() throws IOException {
        GameCharacter character = character();
        character.setArmorySlug("alba-sol");
        character.setArmoryUpdatedAt(NOW);
        when(client.search("Alba Sol")).thenReturn(Optional.of(new ArmoryMatch("alba-sol", NOW)));

        assertThat(armory.changesOf(character)).isEmpty();
        verify(client, never()).read(any());
    }

    @Test
    void aCharacterThatChangedIsRead() throws IOException {
        GameCharacter character = character();
        character.setArmorySlug("alba-sol");
        character.setArmoryUpdatedAt(THEN);
        ArmoryCharacter read = read();
        when(client.search("Alba Sol")).thenReturn(Optional.of(new ArmoryMatch("alba-sol", NOW)));
        when(client.read("alba-sol")).thenReturn(read);

        assertThat(armory.changesOf(character)).contains(read);
    }

    @Test
    void aCharacterSeenForTheFirstTimeIsReadEvenIfTheArmoryGivesNoDate() throws IOException {
        ArmoryCharacter read = read();
        when(client.search("Alba Sol")).thenReturn(Optional.of(new ArmoryMatch("alba-sol", null)));
        when(client.read("alba-sol")).thenReturn(read);

        assertThat(armory.changesOf(character())).contains(read);
    }

    @Test
    void theDataIsKeptWithTheSlugsOfTheGameData() {
        GameCharacter character = character();

        armory.apply(character, read());

        assertThat(character.getName()).isEqualTo("Alba Sol");
        assertThat(character.getClassSlug()).isEqualTo("death-knight");
        assertThat(character.getRaceSlug()).isEqualTo("night-elf");
        assertThat(character.getLevel()).isEqualTo((short) 13);
        assertThat(character.getArmorySlug()).isEqualTo("alba-sol");
        assertThat(character.getArmoryUpdatedAt()).isEqualTo(NOW);
        assertThat(character.getProfessions().path(0).path("slug").asText()).isEqualTo("first-aid");
        assertThat(character.getProfessions().path(0).path("skill").asInt()).isEqualTo(79);
        assertThat(character.getProfessions().path(0).path("maxSkill").asInt()).isEqualTo(150);
    }

    @Test
    void clearingLeavesOnlyTheName() {
        GameCharacter character = character();
        armory.apply(character, read());

        armory.clear(character);

        assertThat(character.getLevel()).isNull();
        assertThat(character.getClassSlug()).isNull();
        assertThat(character.getRaceSlug()).isNull();
        assertThat(character.getArmorySlug()).isNull();
        assertThat(character.getArmoryUpdatedAt()).isNull();
        assertThat(character.getProfessions()).isEmpty();
    }

    private static GameCharacter character() {
        GameCharacter character = new GameCharacter();
        character.setName("Alba Sol");
        return character;
    }

    private static ArmoryCharacter read() {
        return new ArmoryCharacter("alba-sol", "Alba Sol", "Death Knight", "Night Elf", 13,
                List.of(new ArmoryCharacter.Profession("First Aid", 79, 150)), NOW);
    }
}
