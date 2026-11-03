package com.adrianpmdev.lol.features.players.application;

import com.adrianpmdev.platform.asset.ImageSlot;
import com.adrianpmdev.platform.asset.ImageSlotProvider;
import com.adrianpmdev.platform.asset.PublicAssets;
import com.adrianpmdev.platform.image.ImageSpec;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The emblem of each tier, which an admin uploads from the Pictures screen: Riot gives them as a download, not through
 * its API or Data Dragon. A tier without its emblem shows without a picture.
 *
 * @author adrianpmdev
 */
@Component
@RequiredArgsConstructor
public class LolRankEmblems implements ImageSlotProvider {

    private static final String PREFIX = "lol/ranks/";
    private static final String GROUP = "League of Legends · Rangos";
    private static final ImageSpec SPEC = new ImageSpec(256, 150_000);
    private static final Map<String, String> TIERS = tiers();

    private final PublicAssets publicAssets;

    private static Map<String, String> tiers() {
        Map<String, String> tiers = new LinkedHashMap<>();
        tiers.put("IRON", "Hierro");
        tiers.put("BRONZE", "Bronce");
        tiers.put("SILVER", "Plata");
        tiers.put("GOLD", "Oro");
        tiers.put("PLATINUM", "Platino");
        tiers.put("EMERALD", "Esmeralda");
        tiers.put("DIAMOND", "Diamante");
        tiers.put("MASTER", "Maestro");
        tiers.put("GRANDMASTER", "Gran Maestro");
        tiers.put("CHALLENGER", "Aspirante");

        return tiers;
    }

    /**
     * The URLs of the uploaded emblems.
     *
     * @return URL by full picture key
     */
    public Map<String, String> urls() {
        return publicAssets.urls(PREFIX);
    }

    /**
     * The URL of the emblem of a tier.
     *
     * @param urls the URLs of the uploaded emblems
     * @param tier tier as Riot writes it
     * @return its URL, or null while it is not uploaded
     */
    public static String urlOf(Map<String, String> urls, String tier) {
        return urls.get(keyOf(tier));
    }

    @Override
    public List<ImageSlot> imageSlots() {
        return TIERS.entrySet().stream()
                .map(tier -> new ImageSlot(keyOf(tier.getKey()), GROUP, tier.getValue(), SPEC))
                .toList();
    }

    private static String keyOf(String tier) {
        return PREFIX + tier.toLowerCase(Locale.ROOT);
    }
}
