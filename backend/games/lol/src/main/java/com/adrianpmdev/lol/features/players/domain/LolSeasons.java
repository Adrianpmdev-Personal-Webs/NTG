package com.adrianpmdev.lol.features.players.domain;

import com.adrianpmdev.platform.error.ApiErrors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * The seasons of the module, one per year, which an admin creates and dates from the web (table lol.seasons). They are
 * kept in memory until one changes. Games are placed in a season by their date, so changing a date moves them by itself.
 * The current season is the newest one already started.
 *
 * @author adrianpmdev
 */
@Component
public class LolSeasons {

    private static final Instant NO_END = Instant.parse("9999-01-01T00:00:00Z");

    private final JdbcTemplate jdbcTemplate;
    private volatile List<Season> seasons;

    /**
     * Builds the seasons.
     *
     * @param jdbcTemplate reads and writes the table
     */
    public LolSeasons(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * A season.
     *
     * @param key   short name, such as 26
     * @param label name shown to players, such as Season 26
     * @param date  first day
     * @param start first moment
     * @param end   moment the next one starts
     */
    public record Season(String key, String label, LocalDate date, Instant start, Instant end) {
    }

    /**
     * A season as it is stored.
     *
     * @param key  short name
     * @param date first day
     */
    record Entry(String key, LocalDate date) {
    }

    /**
     * Every season, oldest first.
     *
     * @return seasons
     */
    public List<Season> all() {
        List<Season> known = seasons;

        if (known == null) {
            known = build(jdbcTemplate.query("select key, start_date from lol.seasons",
                    (row, index) -> new Entry(row.getString(1), row.getObject(2, LocalDate.class))));

            if (known.isEmpty()) {
                throw new IllegalStateException("There are no seasons in lol.seasons");
            }

            seasons = known;
        }

        return known;
    }

    /**
     * The season being played.
     *
     * @return the newest season already started, or the first one before it starts
     */
    public Season current() {
        return currentOf(all(), Instant.now());
    }

    /**
     * A season by its short name.
     *
     * @param key short name
     * @return the season, if it exists
     */
    public Optional<Season> find(String key) {
        return all().stream().filter(season -> season.key().equals(key)).findFirst();
    }

    /**
     * Where the games followed begin.
     *
     * @return first moment of the oldest season
     */
    public Instant firstStart() {
        return all().getFirst().start();
    }

    /**
     * Creates a season or changes its date.
     *
     * @param key  short name
     * @param date first day
     */
    public void save(String key, LocalDate date) {
        if (all().stream().anyMatch(season -> !season.key().equals(key) && season.date().equals(date))) {
            throw ApiErrors.conflict("Ya hay otra temporada que empieza ese día");
        }

        jdbcTemplate.update("""
                insert into lol.seasons (key, start_date) values (?, ?)
                on conflict (key) do update set start_date = excluded.start_date
                """, key, date);
        seasons = null;
    }

    /**
     * Deletes a season and the final ranks kept for it. Its games stay and go to the season that now covers their date.
     *
     * @param key short name
     */
    public void delete(String key) {
        if (find(key).isEmpty()) {
            throw ApiErrors.notFound("Season");
        }

        if (all().size() == 1) {
            throw ApiErrors.conflict("Tiene que quedar al menos una temporada");
        }

        jdbcTemplate.update("""
                with ranks as (delete from lol.season_ranks where season = ?)
                delete from lol.seasons where key = ?
                """, key, key);
        seasons = null;
    }

    /**
     * Puts seasons in order and works out when each ends.
     *
     * @param entries seasons as stored
     * @return seasons, oldest first
     */
    static List<Season> build(List<Entry> entries) {
        List<Entry> sorted = entries.stream().sorted(Comparator.comparing(Entry::date)).toList();
        List<Season> built = new ArrayList<>();

        for (int index = 0; index < sorted.size(); index++) {
            Entry entry = sorted.get(index);
            Instant end = index + 1 < sorted.size() ? startOf(sorted.get(index + 1).date()) : NO_END;

            built.add(new Season(entry.key(), "Season " + entry.key(), entry.date(), startOf(entry.date()), end));
        }

        return List.copyOf(built);
    }

    /**
     * The season of a moment.
     *
     * @param seasons seasons, oldest first
     * @param now     the moment
     * @return the newest season already started, or the first one before it starts
     */
    static Season currentOf(List<Season> seasons, Instant now) {
        Season found = seasons.getFirst();

        for (Season season : seasons) {
            if (!season.start().isAfter(now)) {
                found = season;
            }
        }

        return found;
    }

    private static Instant startOf(LocalDate date) {
        return date.atStartOfDay(ZoneOffset.UTC).toInstant();
    }
}
