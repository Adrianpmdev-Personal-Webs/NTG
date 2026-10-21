package com.adrianpmdev.core.features.users.infrastructure.persistence.repository;

import com.adrianpmdev.core.features.users.infrastructure.persistence.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JPA repository for the {@link User} entity.
 *
 * @author adrianpmdev
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Finds a user together with its tags in a single query.
     *
     * @param id user id
     * @return user with tags loaded
     */
    @EntityGraph(attributePaths = "tags")
    Optional<User> findWithTagsById(UUID id);

    /**
     * Lists all users with their tags, ordered by username.
     *
     * @return users with tags loaded
     */
    @EntityGraph(attributePaths = "tags")
    List<User> findAllByOrderByUsernameAsc();

    /**
     * Checks whether a username is already used (case-insensitive).
     *
     * @param username username to check
     * @return true if it exists
     */
    boolean existsByUsernameIgnoreCase(String username);

    /**
     * Checks whether a username is used by a different user (case-insensitive).
     *
     * @param username username to check
     * @param id       user to exclude
     * @return true if another user has it
     */
    boolean existsByUsernameIgnoreCaseAndIdNot(String username, UUID id);

    /**
     * Checks whether an email is already registered locally (case-insensitive).
     *
     * @param email email to check
     * @return true if it exists
     */
    boolean existsByEmailIgnoreCase(String email);
}