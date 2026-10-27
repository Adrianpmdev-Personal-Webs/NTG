package com.adrianpmdev.wowforever.features.gamedata.application.service;

import com.adrianpmdev.platform.asset.ImageSlot;
import com.adrianpmdev.platform.asset.ImageSlotProvider;
import com.adrianpmdev.platform.asset.PublicAssets;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Pictures of WoW Forever, uploaded by an admin from the web: the faction backgrounds and the logo of the launcher, and
 * one background per talent tree (the trees come from the game data, so a new class or tree asks for its picture on
 * its own).
 *
 * @author adrianpmdev
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GameImagesService implements ImageSlotProvider {

    static final String PREFIX = "wow-forever/";

    private static final String LAUNCHER_GROUP = "WoW Forever · Inicio";
    private static final String TALENTS_GROUP = "WoW Forever · Árboles de talentos";

    private final GameDataService gameDataService;
    private final PublicAssets publicAssets;

    /**
     * URLs of the uploaded pictures, by key without the {@value #PREFIX} prefix (for example {@code logo},
     * {@code backgrounds/horde} or {@code talents/warrior-arms}).
     *
     * @return URL by short key
     */
    public Map<String, String> getUrls() {
        Map<String, String> urls = new LinkedHashMap<>();

        publicAssets.urls(PREFIX).forEach((key, url) -> urls.put(key.substring(PREFIX.length()), url));

        return urls;
    }

    @Override
    public List<ImageSlot> imageSlots() {
        List<ImageSlot> slots = new ArrayList<>();
        slots.add(new ImageSlot(PREFIX + "logo", LAUNCHER_GROUP, "Logo"));
        slots.add(new ImageSlot(PREFIX + "backgrounds/alliance", LAUNCHER_GROUP, "Fondo de la Alianza"));
        slots.add(new ImageSlot(PREFIX + "backgrounds/horde", LAUNCHER_GROUP, "Fondo de la Horda"));

        try {
            gameDataService.getTalentTreeSlugs().forEach((classSlug, trees) -> trees.forEach(tree ->
                    slots.add(new ImageSlot(PREFIX + "talents/" + classSlug + "-" + tree, TALENTS_GROUP,
                            classSlug + " · " + tree))));
        } catch (RuntimeException exception) {
            // Without game data there are no trees to illustrate; the launcher pictures are still listed.
            log.warn("Talent tree pictures are not listed: {}", exception.getMessage());
        }

        return slots;
    }
}
