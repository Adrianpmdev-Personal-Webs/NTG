package com.adrianpmdev.wowforever.features.characters.application.service;

import com.adrianpmdev.wowforever.features.characters.domain.ArmorySheet;
import com.adrianpmdev.wowforever.features.characters.infrastructure.armory.ArmoryClient;
import com.adrianpmdev.wowforever.features.gamedata.application.service.GameDataService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * What is kept from a read and what is asked again.
 *
 * @author adrianpmdev
 */
class ArmoryReaderTest {

    private static final ArmorySheet SHEET = new ArmorySheet("Alba Sol", 13, "Rogue", "rogue", "Gnome", "gnome",
            "ALLIANCE", "Male", null, 12, null, new ArmorySheet.Images(null, null, null), List.of(), List.of(),
            List.of(), List.of(), 0);

    private final ArmoryClient client = mock(ArmoryClient.class);
    private final ArmoryReader reader = new ArmoryReader(client, new GameDataService());

    @AfterEach
    void tearDown() {
        reader.stop();
    }

    @Test
    void aCharacterReadIsKeptForTheNextLookInAnyCase() throws IOException {
        when(client.character("Alba Sol")).thenReturn(Optional.of(SHEET));

        assertThat(reader.read("Alba Sol")).isEqualTo(new ArmoryReader.Result(ArmoryReader.Status.FOUND, SHEET));
        assertThat(reader.read("alba sol").status()).isEqualTo(ArmoryReader.Status.FOUND);
        verify(client, times(1)).character("Alba Sol");
    }

    @Test
    void aCharacterTheArmoryDoesNotShowIsNotFound() throws IOException {
        when(client.character("Nuevo Pj")).thenReturn(Optional.empty());

        assertThat(reader.read("Nuevo Pj")).isEqualTo(new ArmoryReader.Result(ArmoryReader.Status.NOT_FOUND, null));
    }

    @Test
    void aFailedReadIsNotKeptSoTheNextLookTriesAgain() throws IOException {
        when(client.character("Alba Sol")).thenThrow(new IOException("down")).thenReturn(Optional.of(SHEET));

        assertThat(reader.read("Alba Sol").status()).isEqualTo(ArmoryReader.Status.FAILED);
        assertThat(reader.read("Alba Sol").status()).isEqualTo(ArmoryReader.Status.FOUND);
    }

    @Test
    void severalCharactersAreReadAndAnsweredInOrder() throws IOException {
        when(client.character("Alba Sol")).thenReturn(Optional.of(SHEET));
        when(client.character("Nuevo Pj")).thenReturn(Optional.empty());
        when(client.character("Zul Tres")).thenThrow(new IOException("down"));

        Map<String, ArmoryReader.Result> results = reader.readAll(List.of("Alba Sol", "Nuevo Pj", "Zul Tres"));

        assertThat(results.keySet()).containsExactly("Alba Sol", "Nuevo Pj", "Zul Tres");
        assertThat(results.values()).extracting(ArmoryReader.Result::status).containsExactly(
                ArmoryReader.Status.FOUND, ArmoryReader.Status.NOT_FOUND, ArmoryReader.Status.FAILED);
    }
}
