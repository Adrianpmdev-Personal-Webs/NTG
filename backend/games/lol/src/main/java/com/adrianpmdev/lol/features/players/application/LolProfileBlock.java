package com.adrianpmdev.lol.features.players.application;

import com.adrianpmdev.lol.LolModule;
import com.adrianpmdev.lol.features.players.api.LolResponses;
import com.adrianpmdev.lol.features.players.domain.LolQueue;
import com.adrianpmdev.lol.features.players.infrastructure.LolAccount;
import com.adrianpmdev.lol.features.players.infrastructure.LolAccountRepository;
import com.adrianpmdev.lol.features.players.infrastructure.LolMatchRepository;
import com.adrianpmdev.platform.profile.ProfileBlock;
import com.adrianpmdev.platform.profile.ProfileBlockProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * What League of Legends shows on the profile of a player: their official rank and how each queue went this season.
 *
 * @author adrianpmdev
 */
@Component
@RequiredArgsConstructor
public class LolProfileBlock implements ProfileBlockProvider {

    private final LolAccountRepository accountRepository;
    private final LolMatchRepository matchRepository;
    private final LolSyncWorker worker;

    @Override
    public String gameKey() {
        return LolModule.TAG;
    }

    @Override
    public Optional<ProfileBlock> block(UUID userId) {
        return accountRepository.findById(userId).map(this::blockOf);
    }

    private ProfileBlock blockOf(LolAccount account) {
        List<LolResponses.Rank> ranks = LolPlayerService.ranksOf(account);
        List<LolMatchRepository.ChampionTotals> totals =
                matchRepository.championTotals(account.getUserId(), worker.seasonStart());
        List<ProfileBlock.Stat> stats = new ArrayList<>();
        List<ProfileBlock.Bar> bars = new ArrayList<>();
        long allGames = 0;

        for (LolResponses.Rank rank : ranks) {
            stats.add(new ProfileBlock.Stat(rank.queue(), rank.tier() + " · " + rank.lp() + " LP"));
        }

        for (LolQueue queue : LolQueue.ALL) {
            long games = totals.stream().filter(row -> row.getQueueId() == queue.id())
                    .mapToLong(LolMatchRepository.ChampionTotals::getGames).sum();
            long wins = totals.stream().filter(row -> row.getQueueId() == queue.id())
                    .mapToLong(LolMatchRepository.ChampionTotals::getWins).sum();

            allGames += games;

            if (games > 0) {
                bars.add(new ProfileBlock.Bar("Victorias en " + queue.label(), (int) wins, (int) games, null));
            }
        }

        stats.add(new ProfileBlock.Stat("Partidas de la temporada", String.valueOf(allGames)));

        List<String> summary = new ArrayList<>();

        if (!ranks.isEmpty()) {
            summary.add(ranks.getFirst().queue() + " " + ranks.getFirst().tier());
        }

        summary.add(allGames + " partidas");

        return new ProfileBlock(summary, List.of(new ProfileBlock.Section(account.getGameName() + "#"
                + account.getTagLine(), stats, bars, List.of())));
    }
}
