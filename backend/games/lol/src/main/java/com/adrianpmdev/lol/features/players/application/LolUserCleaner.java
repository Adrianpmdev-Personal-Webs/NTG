package com.adrianpmdev.lol.features.players.application;

import com.adrianpmdev.platform.module.UserDataCleaner;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * When an account is deleted, its Riot link goes with it, along with its games and final ranks, which the database
 * removes with the link.
 *
 * @author adrianpmdev
 */
@Component
@RequiredArgsConstructor
public class LolUserCleaner implements UserDataCleaner {

    private final LolPlayerService playerService;

    @Override
    public void removeUser(UUID userId) {
        playerService.unlink(userId);
    }
}
