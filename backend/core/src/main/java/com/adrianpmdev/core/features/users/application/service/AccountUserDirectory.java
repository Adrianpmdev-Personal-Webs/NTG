package com.adrianpmdev.core.features.users.application.service;

import com.adrianpmdev.core.features.users.infrastructure.persistence.entity.User;
import com.adrianpmdev.core.features.users.infrastructure.persistence.repository.UserRepository;
import com.adrianpmdev.platform.port.UserDirectory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Tells game modules the names of the platform accounts.
 *
 * @author adrianpmdev
 */
@Service
@RequiredArgsConstructor
public class AccountUserDirectory implements UserDirectory {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, String> usernames(Collection<UUID> ids) {
        Map<UUID, String> usernames = new LinkedHashMap<>();

        userRepository.findAllById(ids).forEach(user -> usernames.put(user.getId(), user.getUsername()));

        return usernames;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, String> usersWithTag(String tag) {
        Map<UUID, String> usernames = new LinkedHashMap<>();

        for (User user : userRepository.findAllByOrderByUsernameAsc()) {
            if (user.getTags().stream().anyMatch(candidate -> candidate.getKey().equals(tag))) {
                usernames.put(user.getId(), user.getUsername());
            }
        }

        return usernames;
    }
}
