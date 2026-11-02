package com.adrianpmdev.pokemon.features.players.application.service;

import com.adrianpmdev.pokemon.features.players.infrastructure.persistence.entity.Player;
import com.adrianpmdev.pokemon.features.players.infrastructure.persistence.repository.PlayerRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests that a deleted account leaves its player in the history, without the link.
 *
 * @author adrianpmdev
 */
class PlayerUserCleanerTest {

    private static final UUID USER = UUID.randomUUID();

    private final PlayerRepository repository = mock(PlayerRepository.class);
    private final PlayerUserCleaner cleaner = new PlayerUserCleaner(repository);

    @Test
    void keepsThePlayerWithoutTheLink() {
        Player player = new Player();
        player.setUserId(USER);
        when(repository.findByUserId(USER)).thenReturn(Optional.of(player));

        cleaner.removeUser(USER);

        assertThat(player.getUserId()).isNull();
        verify(repository, never()).delete(any());
    }

    @Test
    void anAccountWithoutPlayerChangesNothing() {
        when(repository.findByUserId(USER)).thenReturn(Optional.empty());

        cleaner.removeUser(USER);

        verify(repository, never()).delete(any());
    }
}
