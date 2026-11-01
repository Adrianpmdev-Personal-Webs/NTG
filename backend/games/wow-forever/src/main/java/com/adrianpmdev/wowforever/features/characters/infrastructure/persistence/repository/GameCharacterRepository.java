package com.adrianpmdev.wowforever.features.characters.infrastructure.persistence.repository;

import com.adrianpmdev.wowforever.features.characters.infrastructure.persistence.entity.GameCharacter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Characters of the players.
 *
 * @author adrianpmdev
 */
public interface GameCharacterRepository extends JpaRepository<GameCharacter, UUID> {

    /**
     * How many characters an account has.
     *
     * @param userId account id
     * @return number of characters
     */
    long countByUserId(UUID userId);

    /**
     * Whether an account already has a character with this name.
     *
     * @param userId account id
     * @param name   character name, in any case
     * @return true when it has
     */
    boolean existsByUserIdAndNameIgnoreCase(UUID userId, String name);

    /**
     * Whether an account has another character with this name.
     *
     * @param userId account id
     * @param name   character name, in any case
     * @param id     character left out
     * @return true when it has
     */
    boolean existsByUserIdAndNameIgnoreCaseAndIdNot(UUID userId, String name, UUID id);

    /**
     * The characters of an account by name.
     *
     * @param userId account id
     * @return characters
     */
    List<GameCharacter> findByUserIdOrderByNameAsc(UUID userId);
}
