package com.adrianpmdev.lol.features.players.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Riot accounts of the players.
 *
 * @author adrianpmdev
 */
public interface LolAccountRepository extends JpaRepository<LolAccount, UUID> {

    /**
     * The account linked with a Riot player.
     *
     * @param puuid player id of Riot
     * @return the account, if somebody linked it
     */
    Optional<LolAccount> findByPuuid(String puuid);
}
