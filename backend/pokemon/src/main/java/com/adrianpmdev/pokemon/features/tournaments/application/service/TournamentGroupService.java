package com.adrianpmdev.pokemon.features.tournaments.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.adrianpmdev.pokemon.features.tournaments.api.dto.request.TournamentGroupCreateRequest;
import com.adrianpmdev.pokemon.features.tournaments.api.dto.request.TournamentGroupUpdateRequest;
import com.adrianpmdev.pokemon.features.tournaments.api.dto.response.TournamentGroupResponse;
import com.adrianpmdev.pokemon.features.tournaments.infrastructure.persistence.entity.Tournament;
import com.adrianpmdev.pokemon.features.tournaments.infrastructure.persistence.entity.TournamentGroup;
import com.adrianpmdev.pokemon.features.tournaments.api.mapper.TournamentGroupMapper;
import com.adrianpmdev.pokemon.features.tournaments.infrastructure.persistence.repository.TournamentGroupRepository;
import com.adrianpmdev.pokemon.features.tournaments.infrastructure.persistence.repository.TournamentRepository;

import java.util.List;
import java.util.UUID;

/**
 * Service for managing tournament groups.
 *
 * @author adrianpmdev
 */
@Service
@RequiredArgsConstructor
public class TournamentGroupService {

    private final TournamentGroupRepository groupRepository;
    private final TournamentRepository tournamentRepository;

    public TournamentGroupResponse createGroup(TournamentGroupCreateRequest request) {
        Tournament tournament = getTournamentEntityById(request.tournamentId());
        TournamentGroup group = TournamentGroupMapper.toEntity(request, tournament);
        return TournamentGroupMapper.toResponse(groupRepository.save(group));
    }

    public TournamentGroupResponse getGroupById(UUID id) {
        return TournamentGroupMapper.toResponse(getGroupEntityById(id));
    }

    public List<TournamentGroupResponse> getAllGroups() {
        return groupRepository.findAll()
                .stream()
                .map(TournamentGroupMapper::toResponse)
                .toList();
    }

    public List<TournamentGroupResponse> getGroupsByTournamentId(UUID tournamentId) {
        return groupRepository.findByTournamentId(tournamentId)
                .stream()
                .map(TournamentGroupMapper::toResponse)
                .toList();
    }

    public TournamentGroupResponse getGroupByTournamentAndLetter(UUID tournamentId, String groupLetter) {
        return TournamentGroupMapper.toResponse(getGroupEntityByTournamentAndLetter(tournamentId, groupLetter));
    }

    public TournamentGroupResponse updateGroup(UUID id, TournamentGroupUpdateRequest request) {
        TournamentGroup group = getGroupEntityById(id);
        Tournament tournament = getTournamentEntityByIdOrNull(request.tournamentId());

        TournamentGroupMapper.updateEntity(group, request, tournament);

        return TournamentGroupMapper.toResponse(groupRepository.save(group));
    }

    public void deleteGroup(UUID id) {
        TournamentGroup group = getGroupEntityById(id);
        groupRepository.delete(group);
    }

    private TournamentGroup getGroupEntityById(UUID id) {
        return groupRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tournament group not found"));
    }

    private TournamentGroup getGroupEntityByTournamentAndLetter(UUID tournamentId, String groupLetter) {
        return groupRepository.findByTournamentIdAndGroupLetter(tournamentId, groupLetter)
                .orElseThrow(() -> new RuntimeException("Tournament group not found"));
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