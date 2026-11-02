package com.adrianpmdev.wowforever.features.progress.application.service;

import com.adrianpmdev.platform.module.UserDataCleaner;
import com.adrianpmdev.wowforever.features.progress.infrastructure.persistence.PlayerProgressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * When an account is deleted, its talent builds and its Legacy challenges go with it.
 *
 * @author adrianpmdev
 */
@Component
@RequiredArgsConstructor
public class PlayerProgressUserCleaner implements UserDataCleaner {

    private final PlayerProgressRepository repository;

    @Override
    @Transactional
    public void removeUser(UUID userId) {
        repository.deleteAll(userId);
    }
}
