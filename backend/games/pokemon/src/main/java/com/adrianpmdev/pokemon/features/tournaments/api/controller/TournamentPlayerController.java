package com.adrianpmdev.pokemon.features.tournaments.api.controller;

import com.adrianpmdev.pokemon.features.tournaments.api.dto.request.TournamentPlayerUpdateRequest;
import com.adrianpmdev.pokemon.features.tournaments.api.dto.response.TeamImageResponse;
import com.adrianpmdev.pokemon.features.tournaments.api.dto.response.TournamentDetailResponse;
import com.adrianpmdev.pokemon.features.tournaments.application.service.TeamImageService;
import com.adrianpmdev.pokemon.features.tournaments.application.service.TournamentPlayerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/**
 * A player inside a tournament.
 *
 * <p>The team picture routes are the only writes of the module open to players: core lets anyone with
 * the pokemon tag call them, and {@link TeamImageService} only accepts the player of that entry (or an
 * admin). Everything else here is admin only.
 *
 * @author adrianpmdev
 */
@RestController
@RequestMapping("/api/v1/pokemon/tournament-players")
@RequiredArgsConstructor
public class TournamentPlayerController {

    private final TournamentPlayerService tournamentPlayerService;
    private final TeamImageService teamImageService;

    @PutMapping("/{id}")
    public TournamentDetailResponse update(@PathVariable UUID id, @Valid @RequestBody TournamentPlayerUpdateRequest request) {
        return tournamentPlayerService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public TournamentDetailResponse remove(@PathVariable UUID id) {
        return tournamentPlayerService.remove(id);
    }

    /** Uploads or replaces the team picture (multipart field {@code file}). */
    @PutMapping(path = "/{id}/team-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public TeamImageResponse uploadTeamImage(@PathVariable UUID id, @RequestPart("file") MultipartFile file) {
        return teamImageService.upload(id, file);
    }

    @DeleteMapping("/{id}/team-image")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeTeamImage(@PathVariable UUID id) {
        teamImageService.remove(id);
    }
}
