package com.adrianpmdev.wowforever.features.characters.application.service;

import com.adrianpmdev.platform.module.UserDataCleaner;
import com.adrianpmdev.wowforever.features.characters.infrastructure.persistence.repository.GameCharacterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * When an account is deleted, its characters go with it.
 *
 * @author adrianpmdev
 */
@Component
@RequiredArgsConstructor
public class CharacterUserCleaner implements UserDataCleaner {

    private final GameCharacterRepository characterRepository;

    @Override
    @Transactional
    public void removeUser(UUID userId) {
        characterRepository.deleteByUserId(userId);
    }
}
