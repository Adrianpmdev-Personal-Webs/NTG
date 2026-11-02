package com.adrianpmdev.pokemon.features.players.application.service;

import com.adrianpmdev.platform.module.UserDataCleaner;
import com.adrianpmdev.pokemon.features.players.infrastructure.persistence.repository.PlayerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * When an account is deleted, its player stays without the link: series, standings and rewards are part of the history
 * of everybody else.
 *
 * @author adrianpmdev
 */
@Component
@RequiredArgsConstructor
public class PlayerUserCleaner implements UserDataCleaner {

    private final PlayerRepository playerRepository;

    @Override
    @Transactional
    public void removeUser(UUID userId) {
        playerRepository.findByUserId(userId).ifPresent(player -> player.setUserId(null));
    }
}
