package com.adrianpmdev.pokemon.features.tournaments.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.adrianpmdev.pokemon.features.tournaments.api.dto.request.GroupParticipantCreateRequest;
import com.adrianpmdev.pokemon.features.tournaments.api.dto.request.GroupParticipantUpdateRequest;
import com.adrianpmdev.pokemon.features.tournaments.api.dto.response.GroupParticipantResponse;
import com.adrianpmdev.pokemon.features.tournaments.infrastructure.persistence.entity.GroupParticipant;
import com.adrianpmdev.pokemon.features.tournaments.infrastructure.persistence.entity.TournamentGroup;
import com.adrianpmdev.pokemon.features.tournaments.infrastructure.persistence.entity.TournamentParticipant;
import com.adrianpmdev.pokemon.features.tournaments.api.mapper.GroupParticipantMapper;
import com.adrianpmdev.pokemon.features.tournaments.infrastructure.persistence.repository.GroupParticipantRepository;
import com.adrianpmdev.pokemon.features.tournaments.infrastructure.persistence.repository.TournamentGroupRepository;
import com.adrianpmdev.pokemon.features.tournaments.infrastructure.persistence.repository.TournamentParticipantRepository;

import java.util.List;
import java.util.UUID;

/**
 * Service for managing group participants.
 *
 * @author adrianpmdev
 */
@Service
@RequiredArgsConstructor
public class GroupParticipantService {

    private final GroupParticipantRepository participantRepository;
    private final TournamentGroupRepository groupRepository;
    private final TournamentParticipantRepository tournamentParticipantRepository;

    public GroupParticipantResponse createParticipant(GroupParticipantCreateRequest request) {
        TournamentGroup group = getGroupEntityById(request.groupId());
        TournamentParticipant tournamentParticipant = getTournamentParticipantEntityById(
                request.tournamentParticipantId()
        );

        GroupParticipant participant = GroupParticipantMapper.toEntity(
                request,
                group,
                tournamentParticipant
        );

        return GroupParticipantMapper.toResponse(participantRepository.save(participant));
    }

    public GroupParticipantResponse getParticipantById(UUID id) {
        return GroupParticipantMapper.toResponse(getParticipantEntityById(id));
    }

    public List<GroupParticipantResponse> getAllParticipants() {
        return participantRepository.findAll()
                .stream()
                .map(GroupParticipantMapper::toResponse)
                .toList();
    }

    public List<GroupParticipantResponse> getParticipantsByGroupId(UUID groupId) {
        return participantRepository.findByGroupIdOrderByGroupPositionAsc(groupId)
                .stream()
                .map(GroupParticipantMapper::toResponse)
                .toList();
    }

    public GroupParticipantResponse updateParticipant(UUID id, GroupParticipantUpdateRequest request) {
        GroupParticipant participant = getParticipantEntityById(id);
        TournamentGroup group = getGroupEntityByIdOrNull(request.groupId());
        TournamentParticipant tournamentParticipant = getTournamentParticipantEntityByIdOrNull(
                request.tournamentParticipantId()
        );

        GroupParticipantMapper.updateEntity(
                participant,
                request,
                group,
                tournamentParticipant
        );

        return GroupParticipantMapper.toResponse(participantRepository.save(participant));
    }

    public void deleteParticipant(UUID id) {
        GroupParticipant participant = getParticipantEntityById(id);
        participantRepository.delete(participant);
    }

    private GroupParticipant getParticipantEntityById(UUID id) {
        return participantRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Group participant not found"));
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