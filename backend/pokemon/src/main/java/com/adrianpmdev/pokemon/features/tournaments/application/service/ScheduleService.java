package com.adrianpmdev.pokemon.features.tournaments.application.service;

import com.adrianpmdev.pokemon.features.tournaments.domain.DoubleEliminationBracket;
import com.adrianpmdev.pokemon.features.tournaments.domain.DoubleEliminationBracket.Place;
import com.adrianpmdev.pokemon.features.tournaments.domain.GroupsToBracket;
import com.adrianpmdev.pokemon.features.tournaments.domain.MatchStage;
import com.adrianpmdev.pokemon.features.tournaments.domain.RoundRobin;
import com.adrianpmdev.pokemon.features.tournaments.domain.SeriesScore;
import com.adrianpmdev.pokemon.features.tournaments.domain.Standings;
import com.adrianpmdev.pokemon.features.tournaments.domain.TournamentFormat;
import com.adrianpmdev.pokemon.features.tournaments.infrastructure.persistence.entity.Match;
import com.adrianpmdev.pokemon.features.tournaments.infrastructure.persistence.entity.Tournament;
import com.adrianpmdev.pokemon.features.tournaments.infrastructure.persistence.entity.TournamentPlayer;
import com.adrianpmdev.pokemon.features.tournaments.infrastructure.persistence.repository.MatchRepository;
import com.adrianpmdev.pokemon.features.tournaments.infrastructure.persistence.repository.TournamentPlayerRepository;
import com.adrianpmdev.pokemon.features.tournaments.infrastructure.persistence.repository.TournamentRepository;
import com.adrianpmdev.pokemon.shared.error.ApiErrors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Generates the series of a tournament and keeps a generated bracket up to date.
 *
 * <ul>
 *     <li><b>Calendar:</b> a round robin league, or the two groups (drawn at random, or as the admin set
 *     them) with a round robin inside each.</li>
 *     <li><b>Bracket:</b> once the groups are over, every player goes to a double-elimination bracket:
 *     first of one group against last of the other, and so on (see {@link GroupsToBracket}).</li>
 *     <li><b>Sync:</b> after every result, the players of the later bracket series are worked out again
 *     from the structure. If a corrected result changes who plays a series, the games stored for it no
 *     longer apply and are removed.</li>
 * </ul>
 *
 * Nothing is regenerated over recorded games: the admin has to clear them first.
 *
 * @author adrianpmdev
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduleService {

    private static final List<MatchStage> CALENDAR_STAGES = List.of(MatchStage.GROUP, MatchStage.LEAGUE);
    private static final List<MatchStage> BRACKET_STAGES = List.of(MatchStage.UPPER, MatchStage.LOWER, MatchStage.GRAND_FINAL);
    private static final int MIN_LEAGUE_PLAYERS = 2;
    private static final int MAX_SYNC_PASSES = 16;

    private final TournamentRepository tournamentRepository;
    private final TournamentPlayerRepository tournamentPlayerRepository;
    private final MatchRepository matchRepository;

    /**
     * Creates the calendar (league or groups) of a tournament, replacing the previous one.
     *
     * @param tournamentId tournament id
     * @param randomGroups draw the groups again (groups_bracket only)
     */
    @Transactional
    public void generateCalendar(UUID tournamentId, boolean randomGroups) {
        Tournament tournament = findTournament(tournamentId);

        if (matchRepository.hasGames(tournamentId, CALENDAR_STAGES) || matchRepository.hasGames(tournamentId, BRACKET_STAGES)) {
            throw ApiErrors.conflict("El torneo ya tiene partidas registradas: bórralas antes de generar el calendario");
        }

        List<TournamentPlayer> players = new ArrayList<>(tournamentPlayerRepository.findByTournament(tournamentId));
        List<Match> existing = matchRepository.findByTournament(tournamentId);
        matchRepository.deleteAll(existing);
        matchRepository.flush();
        tournament.setBracketSize(null);

        if (tournament.getFormat() == TournamentFormat.ROUND_ROBIN) {
            if (players.size() < MIN_LEAGUE_PLAYERS) {
                throw ApiErrors.conflict("Una liga necesita al menos " + MIN_LEAGUE_PLAYERS + " jugadores");
            }

            players.forEach(tp -> tp.setGroupName(null));
            Collections.shuffle(players);
            createRoundRobin(tournament, MatchStage.LEAGUE, players, new HashMap<>());
            return;
        }

        if (players.size() < GroupsToBracket.MIN_PLAYERS) {
            throw ApiErrors.conflict("Un torneo con grupos necesita al menos " + GroupsToBracket.MIN_PLAYERS + " jugadores");
        }

        if (randomGroups) {
            drawGroups(players);
        } else {
            requireValidGroups(players);
        }

        Map<Integer, Integer> slotsByRound = new HashMap<>();

        for (String group : List.of(GroupsToBracket.GROUP_A, GroupsToBracket.GROUP_B)) {
            List<TournamentPlayer> members = new ArrayList<>(players.stream().filter(tp -> group.equals(tp.getGroupName())).toList());
            Collections.shuffle(members);
            createRoundRobin(tournament, MatchStage.GROUP, members, slotsByRound);
        }
    }

    /**
     * Creates the double-elimination bracket from the final group tables, replacing the previous one.
     *
     * @param tournamentId tournament id
     */
    @Transactional
    public void generateBracket(UUID tournamentId) {
        Tournament tournament = findTournament(tournamentId);

        if (tournament.getFormat() != TournamentFormat.GROUPS_BRACKET) {
            throw ApiErrors.conflict("Solo los torneos con grupos tienen bracket");
        }

        if (matchRepository.hasGames(tournamentId, BRACKET_STAGES)) {
            throw ApiErrors.conflict("El bracket ya tiene partidas registradas: bórralas antes de generarlo de nuevo");
        }

        List<TournamentPlayer> players = tournamentPlayerRepository.findByTournament(tournamentId);
        List<Match> matches = matchRepository.findByTournament(tournamentId);
        List<Match> groupMatches = matches.stream().filter(m -> m.getStage() == MatchStage.GROUP).toList();

        if (groupMatches.isEmpty() || !groupMatches.stream().map(TournamentResults::toScore).allMatch(SeriesScore::decided)) {
            throw ApiErrors.conflict("La fase de grupos no ha terminado");
        }

        TournamentResults.Result results = TournamentResults.compute(tournament, players, groupMatches);
        List<UUID> groupA = idsOf(results.groups().get(GroupsToBracket.GROUP_A));
        List<UUID> groupB = idsOf(results.groups().get(GroupsToBracket.GROUP_B));
        List<UUID> direct = players.stream().filter(tp -> tp.getGroupName() == null).map(TournamentPlayer::getId).toList();

        if (groupA.size() != groupB.size()) {
            throw ApiErrors.conflict("Los dos grupos deben tener los mismos jugadores");
        }

        List<UUID[]> firstRound = GroupsToBracket.firstRound(groupA, groupB, direct);
        DoubleEliminationBracket bracket = DoubleEliminationBracket.ofSize(DoubleEliminationBracket.sizeFor(firstRound.size()));
        Map<UUID, TournamentPlayer> byId = players.stream().collect(Collectors.toMap(TournamentPlayer::getId, Function.identity()));

        matchRepository.deleteAll(matches.stream().filter(m -> m.getStage().isBracket()).toList());
        matchRepository.flush();

        for (Place place : bracket.places()) {
            Match match = newMatch(tournament, place.stage(), place.round(), place.slot());

            if (place.stage() == MatchStage.UPPER && place.round() == 1 && place.slot() <= firstRound.size()) {
                UUID[] pair = firstRound.get(place.slot() - 1);
                match.setPlayer1(byId.get(pair[0]));
                match.setPlayer2(pair[1] == null ? null : byId.get(pair[1]));
            }

            matchRepository.save(match);
        }

        tournament.setBracketSize((short) bracket.size());
        tournamentRepository.save(tournament);
        syncBracket(tournament);
    }

    /**
     * Works out again who plays every later series of a generated bracket. Imported brackets are left
     * as they are.
     *
     * @param tournament tournament
     */
    @Transactional
    public void syncBracket(Tournament tournament) {
        if (tournament.getBracketSize() == null) {
            return;
        }

        DoubleEliminationBracket bracket = DoubleEliminationBracket.ofSize(tournament.getBracketSize());
        Map<UUID, TournamentPlayer> byId = tournamentPlayerRepository.findByTournament(tournament.getId()).stream()
                .collect(Collectors.toMap(TournamentPlayer::getId, Function.identity()));
        Map<Place, Match> byPlace = matchRepository.findByTournament(tournament.getId()).stream()
                .filter(match -> match.getStage().isBracket())
                .collect(Collectors.toMap(TournamentQueryService::placeOf, Function.identity()));

        // A correction can cascade (series 1 changes, so series 2 loses its games, so series 3...).
        for (int pass = 0; pass < MAX_SYNC_PASSES; pass++) {
            Map<Place, SeriesScore> scores = new HashMap<>();
            byPlace.forEach((place, match) -> scores.put(place, TournamentResults.toScore(match)));
            Map<Place, DoubleEliminationBracket.Resolved> resolved = bracket.resolve(scores);
            boolean changed = false;

            for (Place place : bracket.places()) {
                Match match = byPlace.get(place);

                if (match == null || (place.stage() == MatchStage.UPPER && place.round() == 1)) {
                    continue;
                }

                TournamentPlayer player1 = byId.get(resolved.get(place).side1().player());
                TournamentPlayer player2 = byId.get(resolved.get(place).side2().player());

                if (!Objects.equals(idOf(match.getPlayer1()), idOf(player1)) || !Objects.equals(idOf(match.getPlayer2()), idOf(player2))) {
                    if (!match.getGames().isEmpty()) {
                        log.info("Bracket series {} of tournament {} changed players: its games are removed", place, tournament.getId());
                        match.getGames().clear();
                    }

                    match.setPlayer1(player1);
                    match.setPlayer2(player2);
                    changed = true;
                }
            }

            if (!changed) {
                return;
            }
        }
    }

    private void createRoundRobin(Tournament tournament, MatchStage stage, List<TournamentPlayer> players,
                                  Map<Integer, Integer> slotsByRound) {
        Map<UUID, TournamentPlayer> byId = players.stream().collect(Collectors.toMap(TournamentPlayer::getId, Function.identity()));

        for (RoundRobin.Pairing pairing : RoundRobin.schedule(players.stream().map(TournamentPlayer::getId).toList())) {
            int slot = slotsByRound.merge(pairing.round(), 1, Integer::sum);
            Match match = newMatch(tournament, stage, pairing.round(), slot);
            match.setPlayer1(byId.get(pairing.player1()));
            match.setPlayer2(byId.get(pairing.player2()));
            matchRepository.save(match);
        }
    }

    private static void drawGroups(List<TournamentPlayer> players) {
        List<TournamentPlayer> shuffled = new ArrayList<>(players);
        Collections.shuffle(shuffled);
        Map<UUID, TournamentPlayer> byId = shuffled.stream().collect(Collectors.toMap(TournamentPlayer::getId, Function.identity()));
        GroupsToBracket.Draw draw = GroupsToBracket.draw(shuffled.stream().map(TournamentPlayer::getId).toList());

        draw.groupA().forEach(id -> byId.get(id).setGroupName(GroupsToBracket.GROUP_A));
        draw.groupB().forEach(id -> byId.get(id).setGroupName(GroupsToBracket.GROUP_B));
        draw.direct().forEach(id -> byId.get(id).setGroupName(null));
    }

    private static void requireValidGroups(List<TournamentPlayer> players) {
        long groupA = players.stream().filter(tp -> GroupsToBracket.GROUP_A.equals(tp.getGroupName())).count();
        long groupB = players.stream().filter(tp -> GroupsToBracket.GROUP_B.equals(tp.getGroupName())).count();
        long direct = players.size() - groupA - groupB;
        long expectedDirect = players.size() % 2;

        if (groupA != groupB || direct != expectedDirect) {
            throw ApiErrors.conflict("Los grupos A y B deben tener los mismos jugadores"
                    + (expectedDirect == 1 ? " y solo un jugador sin grupo" : " y todos deben tener grupo"));
        }
    }

    private static Match newMatch(Tournament tournament, MatchStage stage, int round, int slot) {
        Match match = new Match();
        match.setTournament(tournament);
        match.setStage(stage);
        match.setRound((short) round);
        match.setSlot((short) slot);

        return match;
    }

    private Tournament findTournament(UUID id) {
        return tournamentRepository.findById(id).orElseThrow(() -> ApiErrors.notFound("Tournament"));
    }

    private static List<UUID> idsOf(List<Standings.Row> rows) {
        return rows == null ? List.of() : rows.stream().map(Standings.Row::participant).toList();
    }

    private static UUID idOf(TournamentPlayer player) {
        return player == null ? null : player.getId();
    }
}
