package com.adrianpmdev.wowforever.features.progress.infrastructure.persistence;

import com.adrianpmdev.wowforever.features.progress.api.dto.response.SavedBuildResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The talent builds each player keeps and the Legacy challenges they ticked.
 *
 * @author adrianpmdev
 */
@Repository
@RequiredArgsConstructor
public class PlayerProgressRepository {

    private final JdbcTemplate jdbcTemplate;

    /**
     * The builds of a player in a class.
     *
     * @param userId    player
     * @param classSlug class
     * @return the builds by slot
     */
    public Map<Integer, SavedBuildResponse> builds(UUID userId, String classSlug) {
        Map<Integer, SavedBuildResponse> builds = new LinkedHashMap<>();

        jdbcTemplate.query("""
                select slot, name, level, build, point_order
                from wow_forever.saved_builds
                where user_id = ? and class_slug = ?
                """, row -> {
                    builds.put(row.getInt(1),
                            new SavedBuildResponse(row.getString(2), row.getInt(3), row.getString(4), row.getString(5)));
                }, userId, classSlug);

        return builds;
    }

    /**
     * Creates or replaces a build.
     *
     * @param userId    player
     * @param classSlug class
     * @param slot      slot
     * @param name      name
     * @param level     level
     * @param build     share code
     * @param order     order the points were spent in, null when it was not recorded
     */
    public void saveBuild(UUID userId, String classSlug, int slot, String name, int level, String build, String order) {
        jdbcTemplate.update("""
                insert into wow_forever.saved_builds (user_id, class_slug, slot, name, level, build, point_order)
                values (?, ?, ?, ?, ?, ?, ?)
                on conflict (user_id, class_slug, slot) do update
                    set name = excluded.name, level = excluded.level, build = excluded.build,
                        point_order = excluded.point_order, updated_at = now()
                """, userId, classSlug, slot, name, level, build, order);
    }

    /**
     * Empties a slot.
     *
     * @param userId    player
     * @param classSlug class
     * @param slot      slot
     */
    public void deleteBuild(UUID userId, String classSlug, int slot) {
        jdbcTemplate.update("delete from wow_forever.saved_builds where user_id = ? and class_slug = ? and slot = ?",
                userId, classSlug, slot);
    }

    /**
     * The challenges a player ticked.
     *
     * @param userId player
     * @return names of the challenges
     */
    public List<String> challenges(UUID userId) {
        return jdbcTemplate.queryForList("select name from wow_forever.legacy_challenges where user_id = ? order by name",
                String.class, userId);
    }

    /**
     * Ticks challenges; those already ticked stay.
     *
     * @param userId player
     * @param names  names of the challenges
     */
    public void addChallenges(UUID userId, Collection<String> names) {
        jdbcTemplate.batchUpdate("""
                insert into wow_forever.legacy_challenges (user_id, name) values (?, ?)
                on conflict (user_id, name) do nothing
                """, names.stream().map(name -> new Object[]{userId, name}).toList());
    }

    /**
     * Unticks challenges.
     *
     * @param userId player
     * @param names  names of the challenges
     */
    public void removeChallenges(UUID userId, Collection<String> names) {
        jdbcTemplate.batchUpdate("delete from wow_forever.legacy_challenges where user_id = ? and name = ?",
                names.stream().map(name -> new Object[]{userId, name}).toList());
    }

    /**
     * Deletes everything a player keeps here.
     *
     * @param userId player
     */
    public void deleteAll(UUID userId) {
        jdbcTemplate.update("delete from wow_forever.saved_builds where user_id = ?", userId);
        jdbcTemplate.update("delete from wow_forever.legacy_challenges where user_id = ?", userId);
    }
}
