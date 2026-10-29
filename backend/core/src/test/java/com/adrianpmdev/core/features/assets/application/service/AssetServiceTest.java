package com.adrianpmdev.core.features.assets.application.service;

import com.adrianpmdev.core.features.assets.infrastructure.persistence.repository.AssetRepository;
import com.adrianpmdev.platform.asset.ImageSlot;
import com.adrianpmdev.platform.asset.ImageSlotProvider;
import com.adrianpmdev.platform.image.CompressedImage;
import com.adrianpmdev.platform.image.ImageCompressor;
import com.adrianpmdev.platform.image.ImageSpec;
import com.adrianpmdev.platform.port.ObjectStorage;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Limits applied to an upload: those of its slot when it sets them (icons), the general ones otherwise.
 *
 * @author adrianpmdev
 */
class AssetServiceTest {

    private static final ImageSpec ICON_SPEC = new ImageSpec(128, 100_000);

    private final AssetRepository assetRepository = mock(AssetRepository.class);
    private final ImageCompressor imageCompressor = mock(ImageCompressor.class);
    private final AssetService service = new AssetService(assetRepository, imageCompressor,
            mock(ObjectStorage.class), slots(
                    new ImageSlot("platform/games/pokemon", "Portadas", "Pokémon"),
                    new ImageSlot("platform/game-icons/pokemon", "Iconos", "Pokémon", ICON_SPEC)));

    @Test
    void anIconIsStoredWithTheLimitsOfItsSlot() {
        assertThat(specUsedFor("platform/game-icons/pokemon")).isEqualTo(ICON_SPEC);
    }

    @Test
    void anyOtherPictureIsStoredWithTheGeneralLimits() {
        assertThat(specUsedFor("platform/games/pokemon")).isEqualTo(new ImageSpec(1920, 1_500_000));
    }

    private ImageSpec specUsedFor(String key) {
        when(assetRepository.findById(key)).thenReturn(Optional.empty());
        when(imageCompressor.compress(any(), any()))
                .thenReturn(new CompressedImage(new byte[]{1}, "image/png", "png", 64, 64));

        service.upload(key, new MockMultipartFile("file", "icon.png", "image/png", new byte[]{1, 2, 3}),
                UUID.randomUUID());

        ArgumentCaptor<ImageSpec> spec = ArgumentCaptor.forClass(ImageSpec.class);
        verify(imageCompressor).compress(any(), spec.capture());

        return spec.getValue();
    }

    @SuppressWarnings("unchecked")
    private static ObjectProvider<ImageSlotProvider> slots(ImageSlot... slots) {
        ObjectProvider<ImageSlotProvider> provider = mock(ObjectProvider.class);
        ImageSlotProvider module = () -> List.of(slots);
        when(provider.orderedStream()).thenAnswer(invocation -> Stream.of(module));

        return provider;
    }
}
