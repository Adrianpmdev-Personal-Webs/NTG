package com.adrianpmdev.pokemon.features.lockes.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.adrianpmdev.pokemon.features.lockes.api.dto.request.LockeCreateRequest;
import com.adrianpmdev.pokemon.features.lockes.api.dto.request.LockeUpdateRequest;
import com.adrianpmdev.pokemon.features.lockes.api.dto.response.LockeResponse;
import com.adrianpmdev.pokemon.features.games.infrastructure.persistence.entity.Game;
import com.adrianpmdev.pokemon.features.lockes.infrastructure.persistence.entity.Locke;
import com.adrianpmdev.pokemon.features.lockes.api.mapper.LockeMapper;
import com.adrianpmdev.pokemon.features.games.infrastructure.persistence.repository.GameRepository;
import com.adrianpmdev.pokemon.features.lockes.infrastructure.persistence.repository.LockeRepository;

import java.util.List;
import java.util.UUID;

/**
 * Service for managing lockes.
 *
 * @author adrianpmdev
 */
@Service
@RequiredArgsConstructor
public class LockeService {

    private final LockeRepository lockeRepository;
    private final GameRepository gameRepository;

    public LockeResponse createLocke(LockeCreateRequest request) {
        Game game = getGameEntityById(request.gameId());
        Locke locke = LockeMapper.toEntity(request, game);
        return LockeMapper.toResponse(lockeRepository.save(locke));
    }

    public LockeResponse getLockeById(UUID id) {
        return LockeMapper.toResponse(getLockeEntityById(id));
    }

    public List<LockeResponse> getAllLockes() {
        return lockeRepository.findAll()
                .stream()
                .map(LockeMapper::toResponse)
                .toList();
    }

    public List<LockeResponse> getLockesByGameId(UUID gameId) {
        return lockeRepository.findByGameId(gameId)
                .stream()
                .map(LockeMapper::toResponse)
                .toList();
    }

    public LockeResponse updateLocke(UUID id, LockeUpdateRequest request) {
        Locke locke = getLockeEntityById(id);
        Game game = getGameEntityByIdOrNull(request.gameId());

        LockeMapper.updateEntity(locke, request, game);

        return LockeMapper.toResponse(lockeRepository.save(locke));
    }

    public void deleteLocke(UUID id) {
        Locke locke = getLockeEntityById(id);
        lockeRepository.delete(locke);
    }

    public List<LockeResponse> searchLockesByName(String name) {
        return lockeRepository.findByNameContainingIgnoreCase(name)
                .stream()
                .map(LockeMapper::toResponse)
                .toList();
    }

    private Locke getLockeEntityById(UUID id) {
        return lockeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Locke not found"));
    }

    private Game getGameEntityById(UUID id) {
        return gameRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Game not found"));
    }

    private Game getGameEntityByIdOrNull(UUID id) {
        if (id == null) {
            return null;
        }

        return getGameEntityById(id);
    }
}