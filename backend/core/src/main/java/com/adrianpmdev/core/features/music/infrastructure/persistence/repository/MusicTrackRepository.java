package com.adrianpmdev.core.features.music.infrastructure.persistence.repository;

import com.adrianpmdev.core.features.music.infrastructure.persistence.entity.MusicTrack;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * JPA repository for the {@link MusicTrack} entity.
 *
 * @author adrianpmdev
 */
@Repository
public interface MusicTrackRepository extends JpaRepository<MusicTrack, UUID> {

    /**
     * Every song, by area and in playlist order.
     *
     * @return songs
     */
    List<MusicTrack> findAllByOrderByAreaAscPositionAscCreatedAtAsc();

    /**
     * The songs of one area, in playlist order.
     *
     * @param area area id
     * @return songs
     */
    List<MusicTrack> findAllByAreaOrderByPositionAscCreatedAtAsc(String area);
}
