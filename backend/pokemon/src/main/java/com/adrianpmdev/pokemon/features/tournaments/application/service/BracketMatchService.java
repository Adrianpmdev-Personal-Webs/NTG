package com.adrianpmdev.pokemon.features.tournaments.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.adrianpmdev.pokemon.features.tournaments.api.dto.request.BracketMatchCreateRequest;
import com.adrianpmdev.pokemon.features.tournaments.api.dto.request.BracketMatchUpdateRequest;
import com.adrianpmdev.pokemon.features.tournaments.api.dto.response.BracketMatchResponse;
import com.adrianpmdev.pokemon.features.tournaments.infrastructure.persistence.entity.BracketMatch;
import com.adrianpmdev.pokemon.features.tournaments.infrastructure.persistence.entity.BracketStage;
import com.adrianpmdev.pokemon.features.tournaments.infrastructure.persistence.entity.Tournament;
import com.adrianpmdev.pokemon.features.tournaments.infrastructure.persistence.entity.TournamentParticipant;
import com.adrianpmdev.pokemon.features.tournaments.api.mapper.BracketMatchMapper;
import com.adrianpmdev.pokemon.features.tournaments.infrastructure.persistence.repository.BracketMatchRepository;
import com.adrianpmdev.pokemon.features.tournaments.infrastructure.persistence.repository.BracketStageRepository;
import com.adrianpmdev.pokemon.features.tournaments.infrastructure.persistence.repository.TournamentParticipantRepository;
import com.adrianpmdev.pokemon.features.tournaments.infrastructure.persistence.repository.TournamentRepository;

import java.util.List;
import java.util.UUID;

/**
 * Service for managing bracket matches.
 *
 * @author adrianpmdev
 */
@Service
@RequiredArgsConstructor
public class BracketMatchService {

    private final BracketMatchRepository matchRepository;
    private final TournamentRepository tournamentRepository;
    private final BracketStageRepository bracketStageRepository;
    private final TournamentParticipantRepository participantRepository;

    public BracketMatchResponse createMatch(BracketMatchCreateRequest request) {
        Tournament tournament = getTournamentEntityById(request.tournamentId());
        BracketStage bracketStage = getBracketStageEntityById(request.bracketStageId());
        TournamentParticipant participant1 = getParticipantEntityByIdOrNull(request.participant1Id());
        TournamentParticipant participant2 = getParticipantEntityByIdOrNull(request.participant2Id());
        TournamentParticipant winner = getParticipantEntityByIdOrNull(request.winnerId());

        BracketMatch match = BracketMatchMapper.toEntity(
                request,
                tournament,
                bracketStage,
                participant1,
                participant2,
                winner
        );

        return BracketMatchMapper.toResponse(matchRepository.save(match));
    }

    public BracketMatchResponse getMatchById(UUID id) {
        return BracketMatchMapper.toResponse(getMatchEntityById(id));
    }

    public List<BracketMatchResponse> getAllMatches() {
        return matchRepository.findAll()
                .stream()
                .map(BracketMatchMapper::toResponse)
                .toList();
    }

    public List<BracketMatchResponse> getMatchesByBracketStageId(UUID stageId) {
        return matchRepository.findByBracketStageId(stageId)
                .stream()
                .map(BracketMatchMapper::toResponse)
                .toList();
    }

    public List<BracketMatchResponse> getMatchesByTournamentId(UUID tournamentId) {
        return matchRepository.findByTournamentId(tournamentId)
                .stream()
                .map(BracketMatchMapper::toResponse)
                .toList();
    }

    public BracketMatchResponse updateMatch(UUID id, BracketMatchUpdateRequest request) {
        BracketMatch match = getMatchEntityById(id);
        Tournament tournament = getTournamentEntityByIdOrNull(request.tournamentId());
        BracketStage bracketStage = getBracketStageEntityByIdOrNull(request.bracketStageId());
        TournamentParticipant participant1 = getParticipantEntityByIdOrNull(request.participant1Id());
        TournamentParticipant participant2 = getParticipantEntityByIdOrNull(request.participant2Id());
        TournamentParticipant winner = getParticipantEntityByIdOrNull(request.winnerId());

        BracketMatchMapper.updateEntity(
                match,
                request,
                tournament,
                bracketStage,
                participant1,
                participant2,
                winner
        );

        return BracketMatchMapper.toResponse(matchRepository.save(match));
    }

    public void deleteMatch(UUID id) {
        BracketMatch match = getMatchEntityById(id);
        matchRepository.delete(match);
    }

    private BracketMatch getMatchEntityById(UUID id) {
        return matchRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Bracket match not found"));
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

    private BracketStage getBracketStageEntityById(UUID id) {
        return bracketStageRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Bracket stage not found"));
    }

    private BracketStage getBracketStageEntityByIdOrNull(UUID id) {
        if (id == null) {
            return null;
        }

        return getBracketStageEntityById(id);
    }

    private TournamentParticipant getParticipantEntityByIdOrNull(UUID id) {
        if (id == null) {
            return null;
        }

        return participantRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tournament participant not found"));
    }
}