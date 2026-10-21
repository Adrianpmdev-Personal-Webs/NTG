package com.adrianpmdev.pokemon.features.tournaments.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.adrianpmdev.pokemon.features.tournaments.api.dto.request.TournamentParticipantCreateRequest;
import com.adrianpmdev.pokemon.features.tournaments.api.dto.request.TournamentParticipantUpdateRequest;
import com.adrianpmdev.pokemon.features.tournaments.api.dto.response.TournamentParticipantResponse;
import com.adrianpmdev.pokemon.features.tournaments.infrastructure.persistence.entity.Tournament;
import com.adrianpmdev.pokemon.features.tournaments.infrastructure.persistence.entity.TournamentParticipant;
import com.adrianpmdev.pokemon.features.tournaments.api.mapper.TournamentParticipantMapper;
import com.adrianpmdev.pokemon.features.tournaments.infrastructure.persistence.repository.TournamentParticipantRepository;
import com.adrianpmdev.pokemon.features.tournaments.infrastructure.persistence.repository.TournamentRepository;

import java.util.List;
import java.util.UUID;

/**
 * Service for managing tournament participants.
 *
 * @author adrianpmdev
 */
@Service
@RequiredArgsConstructor
public class TournamentParticipantService {

    private final TournamentParticipantRepository participantRepository;
    private final TournamentRepository tournamentRepository;

    public TournamentParticipantResponse createParticipant(TournamentParticipantCreateRequest request) {
        Tournament tournament = getTournamentEntityById(request.tournamentId());
        TournamentParticipant participant = TournamentParticipantMapper.toEntity(request, tournament);
        return TournamentParticipantMapper.toResponse(participantRepository.save(participant));
    }

    public TournamentParticipantResponse getParticipantById(UUID id) {
        return TournamentParticipantMapper.toResponse(getParticipantEntityById(id));
    }

    public List<TournamentParticipantResponse> getAllParticipants() {
        return participantRepository.findAll()
                .stream()
                .map(TournamentParticipantMapper::toResponse)
                .toList();
    }

    public List<TournamentParticipantResponse> getParticipantsByTournamentId(UUID tournamentId) {
        return participantRepository.findByTournamentId(tournamentId)
                .stream()
                .map(TournamentParticipantMapper::toResponse)
                .toList();
    }

    public TournamentParticipantResponse getParticipantByTournamentAndUser(UUID tournamentId, UUID userId) {
        TournamentParticipant participant = participantRepository.findByTournamentIdAndUserId(tournamentId, userId)
                .orElseThrow(() -> new RuntimeException("Tournament participant not found"));

        return TournamentParticipantMapper.toResponse(participant);
    }

    public List<TournamentParticipantResponse> getParticipantsByUserId(UUID userId) {
        return participantRepository.findByUserId(userId)
                .stream()
                .map(TournamentParticipantMapper::toResponse)
                .toList();
    }

    public List<TournamentParticipantResponse> getParticipantsByTournamentIdAndStatus(
            UUID tournamentId,
            String status
    ) {
        return participantRepository.findByTournamentIdAndStatus(tournamentId, status)
                .stream()
                .map(TournamentParticipantMapper::toResponse)
                .toList();
    }

    public TournamentParticipantResponse updateParticipant(
            UUID id,
            TournamentParticipantUpdateRequest request
    ) {
        TournamentParticipant participant = getParticipantEntityById(id);
        Tournament tournament = getTournamentEntityByIdOrNull(request.tournamentId());

        TournamentParticipantMapper.updateEntity(participant, request, tournament);

        return TournamentParticipantMapper.toResponse(participantRepository.save(participant));
    }

    public void deleteParticipant(UUID id) {
        TournamentParticipant participant = getParticipantEntityById(id);
        participantRepository.delete(participant);
    }

    private TournamentParticipant getParticipantEntityById(UUID id) {
        return participantRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tournament participant not found"));
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
}