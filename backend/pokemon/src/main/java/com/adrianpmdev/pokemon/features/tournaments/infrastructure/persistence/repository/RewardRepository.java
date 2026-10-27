package com.adrianpmdev.pokemon.features.tournaments.infrastructure.persistence.repository;

import com.adrianpmdev.pokemon.features.tournaments.infrastructure.persistence.entity.Reward;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

/**
 * Rewards of tournaments.
 *
 * @author adrianpmdev
 */
public interface RewardRepository extends JpaRepository<Reward, UUID> {

    /**
     * Rewards of a tournament in display order.
     *
     * @param tournamentId tournament id
     * @return rewards
     */
    List<Reward> findByTournamentIdOrderBySortOrderAsc(UUID tournamentId);

    /**
     * Last display position used in a tournament.
     *
     * @param tournamentId tournament id
     * @return highest sort order, 0 when it has no rewards
     */
    @Query("select coalesce(max(r.sortOrder), 0) from Reward r where r.tournament.id = :tournamentId")
    int findLastSortOrder(@Param("tournamentId") UUID tournamentId);
}
