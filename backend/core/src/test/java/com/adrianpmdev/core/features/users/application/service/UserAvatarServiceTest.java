package com.adrianpmdev.core.features.users.application.service;

import com.adrianpmdev.core.features.users.infrastructure.persistence.entity.User;
import com.adrianpmdev.core.features.users.infrastructure.persistence.repository.UserRepository;
import com.adrianpmdev.platform.image.CompressedImage;
import com.adrianpmdev.platform.image.ImageCompressor;
import com.adrianpmdev.platform.port.ObjectStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Uploading an avatar replaces the previous upload of its owner and never touches anything else.
 *
 * @author adrianpmdev
 */
class UserAvatarServiceTest {

    private static final UUID ID = UUID.randomUUID();
    private static final String BASE = "https://cdn.example/";
    private static final String OWN_UPLOAD = BASE + "avatars/" + ID + "-old.jpg";

    private final UserRepository repository = mock(UserRepository.class);
    private final ImageCompressor compressor = mock(ImageCompressor.class);
    private final ObjectStorage storage = mock(ObjectStorage.class);
    private final UserAvatarService service = new UserAvatarService(repository, compressor, storage);
    private final User user = mock(User.class);
    private final MockMultipartFile file = new MockMultipartFile("file", "a.png", "image/png", new byte[]{1, 2});

    @BeforeEach
    void setUp() {
        when(repository.findWithTagsById(ID)).thenReturn(Optional.of(user));
        when(user.getId()).thenReturn(ID);
        when(user.getTags()).thenReturn(new HashSet<>());
        when(compressor.compress(any(), any())).thenReturn(new CompressedImage(new byte[]{9}, "image/jpeg", "jpg", 256, 256));
        when(storage.publicUrl(eq("public-assets"), anyString())).thenAnswer(call -> BASE + call.getArgument(1));
    }

    @Test
    void theUploadIsCompressedAndGetsANameOfItsOwner() {
        service.upload(ID, file);

        ArgumentCaptor<String> name = ArgumentCaptor.forClass(String.class);
        verify(storage).upload(eq("public-assets"), name.capture(), eq(new byte[]{9}), eq("image/jpeg"), any());
        assertThat(name.getValue()).startsWith("avatars/" + ID + "-").endsWith(".jpg");
        verify(user).setAvatarUrl(BASE + name.getValue());
    }

    @Test
    void thePreviousUploadOfTheOwnerIsDeleted() {
        when(user.getAvatarUrl()).thenReturn(OWN_UPLOAD);

        service.upload(ID, file);

        verify(storage).delete("public-assets", List.of("avatars/" + ID + "-old.jpg"));
    }

    @Test
    void anAvatarTypedAsAUrlIsNotDeleted() {
        when(user.getAvatarUrl()).thenReturn("https://elsewhere.example/me.png");

        service.upload(ID, file);

        verify(storage, never()).delete(any(), any());
    }

    @Test
    void theUploadOfAnotherUserIsNeverDeleted() {
        when(user.getAvatarUrl()).thenReturn(BASE + "avatars/" + UUID.randomUUID() + "-other.jpg");

        service.upload(ID, file);

        verify(storage, never()).delete(any(), any());
    }

    @Test
    void aFailureDeletingThePreviousOneDoesNotStopTheChange() {
        when(user.getAvatarUrl()).thenReturn(OWN_UPLOAD);
        doThrow(new IllegalStateException("down")).when(storage).delete(any(), any());

        service.upload(ID, file);

        verify(user).setAvatarUrl(anyString());
    }

    @Test
    void pastingALinkDeletesTheUploadItReplaces() {
        service.deleteReplacedUpload(ID, OWN_UPLOAD, "https://elsewhere.example/me.png");

        verify(storage).delete("public-assets", List.of("avatars/" + ID + "-old.jpg"));
    }

    @Test
    void savingTheSameAvatarDeletesNothing() {
        service.deleteReplacedUpload(ID, OWN_UPLOAD, OWN_UPLOAD);

        verify(storage, never()).delete(any(), any());
    }

    @Test
    void changingALinkDeletesNothing() {
        service.deleteReplacedUpload(ID, "https://elsewhere.example/a.png", "https://elsewhere.example/b.png");

        verify(storage, never()).delete(any(), any());
    }

    @Test
    void anEmptyFileIsRefused() {
        MockMultipartFile empty = new MockMultipartFile("file", new byte[0]);

        assertThatThrownBy(() -> service.upload(ID, empty)).isInstanceOf(ResponseStatusException.class);
        verify(storage, never()).upload(any(), any(), any(), any(), any());
    }

    @Test
    void removingTheAvatarClearsItAndDeletesTheUpload() {
        when(user.getAvatarUrl()).thenReturn(OWN_UPLOAD);

        service.delete(ID);

        verify(user).setAvatarUrl(null);
        verify(storage).delete("public-assets", List.of("avatars/" + ID + "-old.jpg"));
    }
}
