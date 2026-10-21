package com.adrianpmdev.pokemon.features.tournaments.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.adrianpmdev.pokemon.features.tournaments.api.dto.request.TournamentStandingCreateRequest;
import com.adrianpmdev.pokemon.features.tournaments.api.dto.request.TournamentStandingUpdateRequest;
import com.adrianpmdev.pokemon.features.tournaments.api.dto.response.TournamentStandingResponse;
import com.adrianpmdev.pokemon.features.tournaments.infrastructure.persistence.entity.Tournament;
import com.adrianpmdev.pokemon.features.tournaments.infrastructure.persistence.entity.TournamentParticipant;
import com.adrianpmdev.pokemon.features.tournaments.infrastructure.persistence.entity.TournamentStanding;
import com.adrianpmdev.pokemon.features.tournaments.api.mapper.TournamentStandingMapper;
import com.adrianpmdev.pokemon.features.tournaments.infrastructure.persistence.repository.TournamentParticipantRepository;
import com.adrianpmdev.pokemon.features.tournaments.infrastructure.persistence.repository.TournamentRepository;
import com.adrianpmdev.pokemon.features.tournaments.infrastructure.persistence.repository.TournamentStandingRepository;

import java.util.List;
import java.util.UUID;

/**
 * Service for managing tournament standings.
 *
 * @author adrianpmdev
 */
@Service
@RequiredArgsConstructor
public class TournamentStandingService {

    private final TournamentStandingRepository standingRepository;
    private final TournamentRepository tournamentRepository;
    private final TournamentParticipantRepository tournamentParticipantRepository;

    public TournamentStandingResponse createStanding(TournamentStandingCreateRequest request) {
        Tournament tournament = getTournamentEntityById(request.tournamentId());
        TournamentParticipant tournamentParticipant = getTournamentParticipantEntityById(
                request.tournamentParticipantId()
        );

        TournamentStanding standing = TournamentStandingMapper.toEntity(
                request,
                tournament,
                tournamentParticipant
        );

        return TournamentStandingMapper.toResponse(standingRepository.save(standing));
    }

    public TournamentStandingResponse getStandingById(UUID id) {
        TournamentStanding standing = getStandingEntityById(id);
        return TournamentStandingMapper.toResponse(standing);
    }

    public List<TournamentStandingResponse> getAllStandings() {
        return standingRepository.findAll()
                .stream()
                .map(TournamentStandingMapper::toResponse)
                .toList();
    }

    public List<TournamentStandingResponse> getStandingsByTournamentId(UUID tournamentId) {
        return standingRepository.findByTournamentIdOrderByFinalPositionAsc(tournamentId)
                .stream()
                .map(TournamentStandingMapper::toResponse)
                .toList();
    }

    public TournamentStandingResponse getStandingByTournamentAndParticipant(
            UUID tournamentId,
            UUID participantId
    ) {
        TournamentStanding standing = standingRepository.findByTournamentIdAndTournamentParticipantId(
                        tournamentId,
                        participantId
                )
                .orElseThrow(() -> new RuntimeException("Tournament standing not found"));

        return TournamentStandingMapper.toResponse(standing);
    }

    public TournamentStandingResponse updateStanding(
            UUID id,
            TournamentStandingUpdateRequest request
    ) {
        TournamentStanding standing = getStandingEntityById(id);
        Tournament tournament = getTournamentEntityByIdOrNull(request.tournamentId());
        TournamentParticipant tournamentParticipant = getTournamentParticipantEntityByIdOrNull(
                request.tournamentParticipantId()
        );

        TournamentStandingMapper.updateEntity(
                standing,
                request,
                tournament,
                tournamentParticipant
        );

        return TournamentStandingMapper.toResponse(standingRepository.save(standing));
    }

    public void deleteStanding(UUID id) {
        TournamentStanding standing = getStandingEntityById(id);
        standingRepository.delete(standing);
    }

    private TournamentStanding getStandingEntityById(UUID id) {
        return standingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tournament standing not found"));
    }

    private Tournament getTournamentEntityById(UUID id) {
        return tournamentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tournament not found"));
    }

    private Tournament getTournamentEntityByIdOrNull(UUID id) {
        if (id == null) {
            return null;
        }

        return getTournamentEntityById(id);
    }

    private TournamentParticipant getTournamentParticipantEntityById(UUID id) {
        return tournamentParticipantRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tournament participant not found"));
    }

    private TournamentParticipant getTournamentParticipantEntityByIdOrNull(UUID id) {
        if (id == null) {
            return null;
        }

        return getTournamentParticipantEntityById(id);
    }
}