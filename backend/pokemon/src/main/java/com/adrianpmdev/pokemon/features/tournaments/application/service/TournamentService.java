package com.adrianpmdev.pokemon.features.tournaments.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.adrianpmdev.pokemon.features.tournaments.api.dto.request.TournamentCreateRequest;
import com.adrianpmdev.pokemon.features.tournaments.api.dto.request.TournamentUpdateRequest;
import com.adrianpmdev.pokemon.features.tournaments.api.dto.response.TournamentResponse;
import com.adrianpmdev.pokemon.features.lockes.infrastructure.persistence.entity.Locke;
import com.adrianpmdev.pokemon.features.tournaments.infrastructure.persistence.entity.Tournament;
import com.adrianpmdev.pokemon.features.tournaments.api.mapper.TournamentMapper;
import com.adrianpmdev.pokemon.features.lockes.infrastructure.persistence.repository.LockeRepository;
import com.adrianpmdev.pokemon.features.tournaments.infrastructure.persistence.repository.TournamentRepository;

import java.util.List;
import java.util.UUID;

/**
 * Service for managing tournaments.
 *
 * @author adrianpmdev
 */
@Service
@RequiredArgsConstructor
public class TournamentService {

    private final TournamentRepository tournamentRepository;
    private final LockeRepository lockeRepository;

    public TournamentResponse createTournament(TournamentCreateRequest request) {
        Locke locke = getLockeEntityById(request.lockeId());
        Tournament tournament = TournamentMapper.toEntity(request, locke);
        return TournamentMapper.toResponse(tournamentRepository.save(tournament));
    }

    public TournamentResponse getTournamentById(UUID id) {
        return TournamentMapper.toResponse(getTournamentEntityById(id));
    }

    public List<TournamentResponse> getAllTournaments() {
        return tournamentRepository.findAll()
                .stream()
                .map(TournamentMapper::toResponse)
                .toList();
    }

    public List<TournamentResponse> getTournamentsByLockeId(UUID lockeId) {
        return tournamentRepository.findByLockeId(lockeId)
                .stream()
                .map(TournamentMapper::toResponse)
                .toList();
    }

    public List<TournamentResponse> getTournamentsByStatus(String status) {
        return tournamentRepository.findByStatus(status)
                .stream()
                .map(TournamentMapper::toResponse)
                .toList();
    }

    public TournamentResponse updateTournament(UUID id, TournamentUpdateRequest request) {
        Tournament tournament = getTournamentEntityById(id);
        Locke locke = getLockeEntityByIdOrNull(request.lockeId());

        TournamentMapper.updateEntity(tournament, request, locke);

        return TournamentMapper.toResponse(tournamentRepository.save(tournament));
    }

    public void deleteTournament(UUID id) {
        Tournament tournament = getTournamentEntityById(id);
        tournamentRepository.delete(tournament);
    }

    public List<TournamentResponse> searchTournamentsByName(String name) {
        return tournamentRepository.findByNameContainingIgnoreCase(name)
                .stream()
                .map(TournamentMapper::toResponse)
                .toList();
    }

    private Tournament getTournamentEntityById(UUID id) {
        return tournamentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tournament not found"));
    }

    private Locke getLockeEntityById(UUID id) {
        return lockeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Locke not found"));
    }

    private Locke getLockeEntityByIdOrNull(UUID id) {
        if (id == null) {
            return null;
        }

        return getLockeEntityById(id);
    }
}