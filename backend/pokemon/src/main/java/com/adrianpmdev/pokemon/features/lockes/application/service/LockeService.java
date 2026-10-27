package com.adrianpmdev.pokemon.features.lockes.application.service;

import com.adrianpmdev.pokemon.features.lockes.api.dto.request.LockeRequest;
import com.adrianpmdev.pokemon.features.lockes.api.dto.response.LockeDetailResponse;
import com.adrianpmdev.pokemon.features.lockes.api.dto.response.LockeSummaryResponse;
import com.adrianpmdev.pokemon.features.lockes.infrastructure.persistence.entity.Locke;
import com.adrianpmdev.pokemon.features.lockes.infrastructure.persistence.repository.LockeRepository;
import com.adrianpmdev.pokemon.features.players.infrastructure.persistence.entity.Player;
import com.adrianpmdev.pokemon.features.players.infrastructure.persistence.repository.PlayerRepository;
import com.adrianpmdev.pokemon.features.rankings.application.service.RankingService;
import com.adrianpmdev.pokemon.features.tournaments.api.dto.response.TournamentSummaryResponse;
import com.adrianpmdev.pokemon.features.tournaments.application.service.TeamImageService;
import com.adrianpmdev.pokemon.features.tournaments.infrastructure.persistence.entity.Tournament;
import com.adrianpmdev.pokemon.features.tournaments.infrastructure.persistence.repository.TournamentPlayerRepository;
import com.adrianpmdev.pokemon.features.tournaments.infrastructure.persistence.repository.TournamentRepository;
import com.adrianpmdev.pokemon.shared.dto.RefResponse;
import com.adrianpmdev.pokemon.shared.error.ApiErrors;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Randomlockes: list (optionally the ones played in a game), page with tournaments and standings,
 * create, replace, set the roster and delete (with every tournament and team picture in it).
 *
 * @author adrianpmdev
 */
@Service
@RequiredArgsConstructor
public class LockeService {

    /** Rules are free JSON; the entity keeps them as a Jackson tree. */
    private static final ObjectMapper JSON = new ObjectMapper();

    private final LockeRepository lockeRepository;
    private final PlayerRepository playerRepository;
    private final TournamentRepository tournamentRepository;
    private final TournamentPlayerRepository tournamentPlayerRepository;
    private final RankingService rankingService;
    private final TeamImageService teamImageService;

    /**
     * Randomlockes, newest first.
     *
     * @param gameId when set, only the ones with a tournament in that game
     * @return lockes
     */
    @Transactional(readOnly = true)
    public List<LockeSummaryResponse> list(UUID gameId) {
        Map<UUID, List<Tournament>> tournamentsByLocke = tournamentRepository.findAllByOrderByPositionAsc().stream()
                .collect(Collectors.groupingBy(t -> t.getLocke().getId()));

        return lockeRepository.findAllByOrderByCreatedAtDesc().stream()
                .filter(locke -> gameId == null || tournamentsByLocke.getOrDefault(locke.getId(), List.of()).stream()
                        .anyMatch(t -> t.getGame().getId().equals(gameId)))
                .map(locke -> {
                    List<Tournament> tournaments = tournamentsByLocke.getOrDefault(locke.getId(), List.of());

                    return new LockeSummaryResponse(locke.getId(), locke.getName(), locke.getDescription(),
                            gamesOf(tournaments), tournaments.size(), locke.getRoster().size());
                })
                .toList();
    }

    /**
     * Page of a randomlocke.
     *
     * @param id locke id
     * @return locke detail
     */
    @Transactional(readOnly = true)
    public LockeDetailResponse get(UUID id) {
        Locke locke = find(id);
        List<Tournament> tournaments = tournamentRepository.findByLockeIdOrderByPositionAsc(id);
        RankingService.LockeResults results = rankingService.lockeResults(id);

        List<TournamentSummaryResponse> summaries = tournaments.stream()
                .map(t -> {
                    Player champion = results.champions().get(t.getId());

                    return new TournamentSummaryResponse(t.getId(), new RefResponse(t.getGame().getId(), t.getGame().getName()),
                            t.getPosition(), t.getName(), t.getFormat().value(), t.getStatus().value(),
                            champion == null ? null : new RefResponse(champion.getId(), champion.getName()));
                })
                .toList();

        List<RefResponse> roster = locke.getRoster().stream()
                .sorted(Comparator.comparing(Player::getName, String.CASE_INSENSITIVE_ORDER))
                .map(player -> new RefResponse(player.getId(), player.getName()))
                .toList();

        return new LockeDetailResponse(locke.getId(), locke.getName(), locke.getDescription(), toObject(locke.getRules()),
                gamesOf(tournaments), roster, summaries, results.standings());
    }

    /**
     * Creates a randomlocke.
     *
     * @param request locke data
     * @return the new locke
     */
    @Transactional
    public LockeDetailResponse create(LockeRequest request) {
        Locke locke = new Locke();
        apply(locke, request);

        return get(lockeRepository.save(locke).getId());
    }

    /**
     * Replaces the data of a randomlocke.
     *
     * @param id      locke id
     * @param request locke data
     * @return the updated locke
     */
    @Transactional
    public LockeDetailResponse update(UUID id, LockeRequest request) {
        Locke locke = find(id);
        apply(locke, request);
        lockeRepository.save(locke);

        return get(id);
    }

    /**
     * Sets the fixed roster of a randomlocke. Tournaments already created keep their players.
     *
     * @param id        locke id
     * @param playerIds players
     * @return the updated locke
     */
    @Transactional
    public LockeDetailResponse setRoster(UUID id, List<UUID> playerIds) {
        Locke locke = find(id);
        List<Player> players = playerRepository.findAllById(new HashSet<>(playerIds));

        if (players.size() != new HashSet<>(playerIds).size()) {
            throw ApiErrors.notFound("Player");
        }

        locke.getRoster().clear();
        locke.getRoster().addAll(players);
        lockeRepository.save(locke);

        return get(id);
    }

    /**
     * Deletes a randomlocke with its tournaments and their team pictures.
     *
     * @param id locke id
     */
    @Transactional
    public void delete(UUID id) {
        Locke locke = find(id);
        List<UUID> tournamentIds = tournamentRepository.findIdsByLocke(id);
        List<String> pictures = tournamentIds.isEmpty() ? List.of() : tournamentPlayerRepository.findTeamImages(tournamentIds);

        // The database cascades the delete to tournaments and everything inside them.
        lockeRepository.delete(locke);
        lockeRepository.flush();
        teamImageService.deleteQuietly(pictures);
    }

    private Locke find(UUID id) {
        return lockeRepository.findWithRosterById(id).orElseThrow(() -> ApiErrors.notFound("Locke"));
    }

    private static void apply(Locke locke, LockeRequest request) {
        locke.setName(request.name().trim());
        locke.setDescription(request.description() == null || request.description().isBlank() ? null : request.description().trim());
        locke.setRules(request.rules() == null ? null : JSON.valueToTree(request.rules()));
    }

    private static Object toObject(JsonNode rules) {
        return rules == null ? null : JSON.convertValue(rules, Object.class);
    }

    private static List<RefResponse> gamesOf(List<Tournament> tournaments) {
        return tournaments.stream()
                .map(Tournament::getGame)
                .distinct()
                .map(game -> new RefResponse(game.getId(), game.getName()))
                .toList();
    }
}
