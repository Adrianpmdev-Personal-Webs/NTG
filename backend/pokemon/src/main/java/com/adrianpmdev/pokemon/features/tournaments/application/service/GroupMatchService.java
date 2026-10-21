package com.adrianpmdev.pokemon.features.tournaments.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.adrianpmdev.pokemon.features.tournaments.api.dto.request.GroupMatchCreateRequest;
import com.adrianpmdev.pokemon.features.tournaments.api.dto.request.GroupMatchUpdateRequest;
import com.adrianpmdev.pokemon.features.tournaments.api.dto.response.GroupMatchResponse;
import com.adrianpmdev.pokemon.features.tournaments.infrastructure.persistence.entity.GroupMatch;
import com.adrianpmdev.pokemon.features.tournaments.infrastructure.persistence.entity.TournamentGroup;
import com.adrianpmdev.pokemon.features.tournaments.infrastructure.persistence.entity.TournamentParticipant;
import com.adrianpmdev.pokemon.features.tournaments.api.mapper.GroupMatchMapper;
import com.adrianpmdev.pokemon.features.tournaments.infrastructure.persistence.repository.GroupMatchRepository;
import com.adrianpmdev.pokemon.features.tournaments.infrastructure.persistence.repository.TournamentGroupRepository;
import com.adrianpmdev.pokemon.features.tournaments.infrastructure.persistence.repository.TournamentParticipantRepository;

import java.util.List;
import java.util.UUID;

/**
 * Service for managing group matches.
 *
 * @author adrianpmdev
 */
@Service
@RequiredArgsConstructor
public class GroupMatchService {

    private final GroupMatchRepository matchRepository;
    private final TournamentGroupRepository groupRepository;
    private final TournamentParticipantRepository participantRepository;

    public GroupMatchResponse createMatch(GroupMatchCreateRequest request) {
        TournamentGroup group = getGroupEntityById(request.groupId());
        TournamentParticipant participant1 = getParticipantEntityById(request.participant1Id());
        TournamentParticipant participant2 = getParticipantEntityById(request.participant2Id());
        TournamentParticipant winner = getParticipantEntityByIdOrNull(request.winnerId());

        GroupMatch match = GroupMatchMapper.toEntity(
                request,
                group,
                participant1,
                participant2,
                winner
        );

        return GroupMatchMapper.toResponse(matchRepository.save(match));
    }

    public GroupMatchResponse getMatchById(UUID id) {
        return GroupMatchMapper.toResponse(getMatchEntityById(id));
    }

    public List<GroupMatchResponse> getAllMatches() {
        return matchRepository.findAll()
                .stream()
                .map(GroupMatchMapper::toResponse)
                .toList();
    }

    public List<GroupMatchResponse> getMatchesByGroupId(UUID groupId) {
        return matchRepository.findByGroupId(groupId)
                .stream()
                .map(GroupMatchMapper::toResponse)
                .toList();
    }

    public List<GroupMatchResponse> getMatchesByGroupIdAndStatus(UUID groupId, String status) {
        return matchRepository.findByGroupIdAndStatus(groupId, status)
                .stream()
                .map(GroupMatchMapper::toResponse)
                .toList();
    }

    public GroupMatchResponse updateMatch(UUID id, GroupMatchUpdateRequest request) {
        GroupMatch match = getMatchEntityById(id);
        TournamentGroup group = getGroupEntityByIdOrNull(request.groupId());
        TournamentParticipant participant1 = getParticipantEntityByIdOrNull(request.participant1Id());
        TournamentParticipant participant2 = getParticipantEntityByIdOrNull(request.participant2Id());
        TournamentParticipant winner = getParticipantEntityByIdOrNull(request.winnerId());

        GroupMatchMapper.updateEntity(
                match,
                request,
                group,
                participant1,
                participant2,
                winner
        );

        return GroupMatchMapper.toResponse(matchRepository.save(match));
    }

    public void deleteMatch(UUID id) {
        GroupMatch match = getMatchEntityById(id);
        matchRepository.delete(match);
    }

    private GroupMatch getMatchEntityById(UUID id) {
        return matchRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Group match not found"));
    }

    private TournamentGroup getGroupEntityById(UUID id) {
        return groupRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tournament group not found"));
    }

    private TournamentGroup getGroupEntityByIdOrNull(UUID id) {
        if (id == null) {
            return null;
        }

        return getGroupEntityById(id);
    }

    private TournamentParticipant getParticipantEntityById(UUID id) {
        return participantRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tournament participant not found"));
    }

    private TournamentParticipant getParticipantEntityByIdOrNull(UUID id) {
        if (id == null) {
            return null;
        }

        return getParticipantEntityById(id);
    }
}