package com.adrianpmdev.core.features.music.application.service;

import com.adrianpmdev.core.features.catalog.api.dto.response.GameResponse;
import com.adrianpmdev.core.features.catalog.application.service.GameCatalogService;
import com.adrianpmdev.core.features.music.api.dto.response.MusicAreaResponse;
import com.adrianpmdev.core.features.music.infrastructure.persistence.entity.MusicTrack;
import com.adrianpmdev.core.features.music.infrastructure.persistence.repository.MusicTrackRepository;
import com.adrianpmdev.core.security.authorization.Authorities;
import com.adrianpmdev.platform.module.GameModule;
import com.adrianpmdev.platform.port.ObjectStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Who hears which playlist, what an upload accepts and how the order is set.
 *
 * @author adrianpmdev
 */
class MusicServiceTest {

    private final MusicTrackRepository trackRepository = mock(MusicTrackRepository.class);
    private final ObjectStorage storage = mock(ObjectStorage.class);
    private final GameCatalogService catalog = mock(GameCatalogService.class);
    private final MusicService service = new MusicService(trackRepository, storage, catalog,
            List.of(GameModule.of("pokemon", "/api/v1/pokemon"), GameModule.of("wow-forever", "/api/v1/wow-forever")));

    @BeforeEach
    void setUp() {
        when(catalog.getVisible(true, Set.of())).thenReturn(List.of(
                new GameResponse("wow-forever", "WoW Forever", null, null, false, true, 1),
                new GameResponse("pokemon", "Pokemon", null, null, false, true, 2),
                new GameResponse("lol", "League of Legends", null, null, true, true, 3)));
        when(storage.publicUrl(anyString(), anyString())).thenAnswer(call -> "https://cdn/" + call.getArgument(1));
        when(trackRepository.save(any())).thenAnswer(call -> call.getArgument(0));
    }

    @Test
    void aPlayerHearsTheGeneralMusicAndOnlyTheGamesOfTheirTags() {
        List<MusicAreaResponse> areas = service.getAreas(false, Set.of(Authorities.tag("pokemon")));

        assertThat(areas).extracting(MusicAreaResponse::area).containsExactly("platform", "pokemon");
    }

    @Test
    void anAdminGetsEveryGameModuleInCatalogOrderAndTheSongsOfAreasNoModuleDeclares() {
        when(trackRepository.findAllByOrderByAreaAscPositionAscCreatedAtAsc())
                .thenReturn(List.of(track("old-game", 0)));

        List<MusicAreaResponse> areas = service.getAreas(true, Set.of());

        assertThat(areas).extracting(MusicAreaResponse::area)
                .containsExactly("platform", "wow-forever", "pokemon", "old-game");
        assertThat(areas.getLast().declared()).isFalse();
        assertThat(areas.getLast().tracks()).hasSize(1);
    }

    @Test
    void aSongGoesLastInItsAreaFolder() {
        when(trackRepository.findAllByAreaOrderByPositionAscCreatedAtAsc("pokemon"))
                .thenReturn(List.of(track("pokemon", 0), track("pokemon", 3)));

        var stored = service.upload("pokemon", m4a(), "  Battle  ", 120, UUID.randomUUID());

        assertThat(stored.title()).isEqualTo("Battle");
        assertThat(stored.url()).startsWith("https://cdn/pokemon/").endsWith(".m4a");
        verify(storage).upload(eq(MusicService.BUCKET), anyString(), any(), eq("audio/mp4"), any());
        verify(trackRepository).save(argThat(track -> track.getPosition() == 4));
    }

    @Test
    void aFileThatIsNotAnM4aIsRefusedBeforeItIsStored() {
        MockMultipartFile mp3 = new MockMultipartFile("file", "song.mp3", "audio/mpeg",
                "ID3\u0004\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000".getBytes(StandardCharsets.US_ASCII));

        assertThatThrownBy(() -> service.upload("pokemon", mp3, "Song", 60, UUID.randomUUID()))
                .isInstanceOf(ResponseStatusException.class);
        verify(storage, never()).upload(anyString(), anyString(), any(), anyString(), any());
    }

    @Test
    void anAreaWithoutAModuleTakesNoSongs() {
        assertThatThrownBy(() -> service.upload("lol", m4a(), "Song", 60, UUID.randomUUID()))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void theNewOrderMustNameEverySongOfTheAreaOnce() {
        MusicTrack first = track("platform", 0);
        MusicTrack second = track("platform", 1);
        when(trackRepository.findAllByAreaOrderByPositionAscCreatedAtAsc("platform")).thenReturn(List.of(first, second));

        assertThatThrownBy(() -> service.reorder("platform", List.of(first.getId(), first.getId()), UUID.randomUUID()))
                .isInstanceOf(ResponseStatusException.class);

        service.reorder("platform", List.of(second.getId(), first.getId()), UUID.randomUUID());

        assertThat(second.getPosition()).isZero();
        assertThat(first.getPosition()).isEqualTo(1);
    }

    private static MusicTrack track(String area, int position) {
        MusicTrack track = new MusicTrack();
        track.setId(UUID.randomUUID());
        track.setArea(area);
        track.setTitle("Song");
        track.setObjectName(area + "/" + track.getId() + ".m4a");
        track.setSizeBytes(1000);
        track.setDurationSeconds(60);
        track.setPosition(position);

        return track;
    }

    private static MockMultipartFile m4a() {
        byte[] header = {0, 0, 0, 0x20, 'f', 't', 'y', 'p', 'M', '4', 'A', ' ', 0, 0, 0, 0};

        return new MockMultipartFile("file", "song.m4a", "audio/mp4", header);
    }
}
