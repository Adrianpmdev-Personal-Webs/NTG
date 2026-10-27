package com.adrianpmdev.pokemon.features.rankings.application.service;

import com.adrianpmdev.pokemon.features.players.infrastructure.persistence.entity.Player;
import com.adrianpmdev.pokemon.features.rankings.api.dto.response.PlayerStandingResponse;
import com.adrianpmdev.pokemon.features.tournaments.application.service.TournamentResults;
import com.adrianpmdev.pokemon.features.tournaments.domain.Standings;
import com.adrianpmdev.pokemon.features.tournaments.infrastructure.persistence.entity.Match;
import com.adrianpmdev.pokemon.features.tournaments.infrastructure.persistence.entity.Tournament;
import com.adrianpmdev.pokemon.features.tournaments.infrastructure.persistence.entity.TournamentPlayer;
import com.adrianpmdev.pokemon.features.tournaments.infrastructure.persistence.repository.MatchRepository;
import com.adrianpmdev.pokemon.features.tournaments.infrastructure.persistence.repository.TournamentPlayerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Standings over several tournaments:
 * <ul>
 *     <li>A randomlocke: the points of its tournaments add up, starting from 0. Every tournament also has
 *     its own standings (see the tournament page), which decide the order of its rewards.</li>
 *     <li>The global ranking: every randomlocke added together, as an all-time record.</li>
 * </ul>
 * Order: points, then series won, then name.
 *
 * @author adrianpmdev
 */
@Service
@RequiredArgsConstructor
public class RankingService {

    private final TournamentPlayerRepository tournamentPlayerRepository;
    private final MatchRepository matchRepository;

    /**
     * Standings of a randomlocke and the champion of each of its finished tournaments.
     *
     * @param standings  players in order
     * @param champions  champion player by tournament id (finished tournaments only)
     */
    public record LockeResults(List<PlayerStandingResponse> standings, Map<UUID, Player> champions) {
    }

    /**
     * Standings of a randomlocke.
     *
     * @param lockeId locke id
     * @return standings and champions
     */
    @Transactional(readOnly = true)
    public LockeResults lockeResults(UUID lockeId) {
        return aggregate(tournamentPlayerRepository.findByLocke(lockeId), matchRepository.findByLocke(lockeId));
    }

    /**
     * Global all-time ranking.
     *
     * @return players in order
     */
    @Transactional(readOnly = true)
    public List<PlayerStandingResponse> globalRanking() {
        return aggregate(tournamentPlayerRepository.findAllWithPlayerAndTournament(), matchRepository.findAllWithGames()).standings();
    }

    private static LockeResults aggregate(List<TournamentPlayer> entries, List<Match> matches) {
        Map<Tournament, List<TournamentPlayer>> entriesByTournament = entries.stream()
                .collect(Collectors.groupingBy(TournamentPlayer::getTournament, LinkedHashMap::new, Collectors.toList()));
        Map<UUID, List<Match>> matchesByTournament = matches.stream()
                .collect(Collectors.groupingBy(match -> match.getTournament().getId()));

        Map<UUID, Totals> totals = new LinkedHashMap<>();
        Map<UUID, Player> champions = new HashMap<>();

        entriesByTournament.forEach((tournament, tournamentEntries) -> {
            TournamentResults.Result result = TournamentResults.compute(tournament, tournamentEntries,
                    matchesByTournament.getOrDefault(tournament.getId(), List.of()));
            Map<UUID, Standings.Row> rows = result.table().stream()
                    .collect(Collectors.toMap(Standings.Row::participant, row -> row));

            for (TournamentPlayer entry : tournamentEntries) {
                Standings.Row row = rows.get(entry.getId());
                Totals playerTotals = totals.computeIfAbsent(entry.getPlayer().getId(), id -> new Totals(entry.getPlayer()));
                playerTotals.points += row.points();
                playerTotals.wins += row.wins();
                playerTotals.losses += row.losses();
                playerTotals.tournaments++;
                playerTotals.lockes.add(tournament.getLocke().getId());

                if (entry.getId().equals(result.champion())) {
                    playerTotals.titles++;
                    champions.put(tournament.getId(), entry.getPlayer());
                }
            }
        });

        List<Totals> ordered = new ArrayList<>(totals.values());
        ordered.sort(Comparator.comparingInt((Totals t) -> t.points).reversed()
                .thenComparing(Comparator.comparingInt((Totals t) -> t.wins).reversed())
                .thenComparing(t -> t.player.getName(), String.CASE_INSENSITIVE_ORDER));

        List<PlayerStandingResponse> standings = new ArrayList<>();

        for (int i = 0; i < ordered.size(); i++) {
            Totals t = ordered.get(i);
            standings.add(new PlayerStandingResponse(t.player.getId(), t.player.getName(), i + 1, t.points, t.wins,
                    t.losses, t.tournaments, t.titles, t.lockes.size()));
        }

        return new LockeResults(standings, champions);
    }

    /** Running totals of one player. */
    private static final class Totals {

        private final Player player;
        private final Set<UUID> lockes = new HashSet<>();
        private int points;
        private int wins;
        private int losses;
        private int tournaments;
        private int titles;

        private Totals(Player player) {
            this.player = player;
        }
    }
}
