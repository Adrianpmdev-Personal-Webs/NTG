package com.adrianpmdev.pokemon.features.tournaments.application.service;

import com.adrianpmdev.pokemon.features.tournaments.api.dto.request.RewardRequest;
import com.adrianpmdev.pokemon.features.tournaments.api.dto.response.TournamentDetailResponse;
import com.adrianpmdev.pokemon.features.tournaments.infrastructure.persistence.entity.Reward;
import com.adrianpmdev.pokemon.features.tournaments.infrastructure.persistence.entity.Tournament;
import com.adrianpmdev.pokemon.features.tournaments.infrastructure.persistence.entity.TournamentPlayer;
import com.adrianpmdev.pokemon.features.tournaments.infrastructure.persistence.repository.RewardRepository;
import com.adrianpmdev.pokemon.features.tournaments.infrastructure.persistence.repository.TournamentPlayerRepository;
import com.adrianpmdev.pokemon.features.tournaments.infrastructure.persistence.repository.TournamentRepository;
import com.adrianpmdev.pokemon.shared.error.ApiErrors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Rewards of a tournament. Players pick them in the order of the final standings; an admin records
 * each pick. A player picks at most one reward per tournament.
 *
 * @author adrianpmdev
 */
@Service
@RequiredArgsConstructor
public class RewardService {

    private final TournamentRepository tournamentRepository;
    private final TournamentPlayerRepository tournamentPlayerRepository;
    private final RewardRepository rewardRepository;
    private final TournamentQueryService queryService;

    /**
     * Adds a reward at the end of the list.
     *
     * @param tournamentId tournament id
     * @param request      reward data
     * @return the updated tournament
     */
    @Transactional
    public TournamentDetailResponse create(UUID tournamentId, RewardRequest request) {
        Tournament tournament = tournamentRepository.findById(tournamentId).orElseThrow(() -> ApiErrors.notFound("Tournament"));

        Reward reward = new Reward();
        reward.setTournament(tournament);
        reward.setSortOrder((short) (rewardRepository.findLastSortOrder(tournamentId) + 1));
        apply(reward, request, tournamentId);
        rewardRepository.saveAndFlush(reward);

        return queryService.detail(tournamentId);
    }

    /**
     * Replaces the name of a reward and who picked it.
     *
     * @param id      reward id
     * @param request reward data
     * @return the updated tournament
     */
    @Transactional
    public TournamentDetailResponse update(UUID id, RewardRequest request) {
        Reward reward = rewardRepository.findById(id).orElseThrow(() -> ApiErrors.notFound("Reward"));
        UUID tournamentId = reward.getTournament().getId();
        apply(reward, request, tournamentId);
        rewardRepository.saveAndFlush(reward);

        return queryService.detail(tournamentId);
    }

    /**
     * Deletes a reward.
     *
     * @param id reward id
     * @return the updated tournament
     */
    @Transactional
    public TournamentDetailResponse delete(UUID id) {
        Reward reward = rewardRepository.findById(id).orElseThrow(() -> ApiErrors.notFound("Reward"));
        UUID tournamentId = reward.getTournament().getId();
        rewardRepository.delete(reward);

        return queryService.detail(tournamentId);
    }

    private void apply(Reward reward, RewardRequest request, UUID tournamentId) {
        reward.setName(request.name().trim());

        if (request.chosenBy() == null) {
            reward.setChosenBy(null);
            return;
        }

        TournamentPlayer picker = tournamentPlayerRepository.findById(request.chosenBy())
                .orElseThrow(() -> ApiErrors.notFound("Tournament player"));

        if (!picker.getTournament().getId().equals(tournamentId)) {
            throw ApiErrors.badRequest("El jugador no pertenece a este torneo");
        }

        boolean alreadyPicked = rewardRepository.findByTournamentIdOrderBySortOrderAsc(tournamentId).stream()
                .anyMatch(other -> !other.equals(reward) && other.getChosenBy() != null
                        && other.getChosenBy().getId().equals(picker.getId()));

        if (alreadyPicked) {
            throw ApiErrors.conflict("Ese jugador ya ha elegido otra recompensa");
        }

        reward.setChosenBy(picker);
    }
}
