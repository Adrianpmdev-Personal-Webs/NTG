package com.adrianpmdev.lol.features.players.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Which season a moment belongs to and how each season ends.
 *
 * @author adrianpmdev
 */
class LolSeasonsTest {

    private final List<LolSeasons.Season> seasons = LolSeasons.build(List.of(
            new LolSeasons.Entry("27", LocalDate.parse("2027-01-07")),
            new LolSeasons.Entry("26", LocalDate.parse("2026-01-08"))));

    @Test
    void seasonsAreSortedAndEachEndsWhenTheNextStarts() {
        assertThat(seasons).extracting(LolSeasons.Season::label).containsExactly("Season 26", "Season 27");
        assertThat(seasons.getFirst().end()).isEqualTo(seasons.get(1).start());
        assertThat(seasons.getFirst().start()).isEqualTo(Instant.parse("2026-01-08T00:00:00Z"));
    }

    @Test
    void theCurrentSeasonIsTheNewestAlreadyStarted() {
        assertThat(LolSeasons.currentOf(seasons, Instant.parse("2026-11-02T00:00:00Z")).key()).isEqualTo("26");
        assertThat(LolSeasons.currentOf(seasons, Instant.parse("2027-01-07T00:00:00Z")).key()).isEqualTo("27");
    }

    @Test
    void beforeTheFirstSeasonTheFirstIsUsed() {
        assertThat(LolSeasons.currentOf(seasons, Instant.parse("2025-12-31T00:00:00Z")).key()).isEqualTo("26");
    }
}
