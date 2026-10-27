package com.adrianpmdev.wowforever.features.gamedata.application.service;

import java.util.List;
import java.util.Map;

/**
 * What the data file does not carry, read from {@code wow-forever/config.json}: where icons come from, the texts of
 * the area in Spanish, the rules of the talent calculator and the launcher content. Changing a text is editing that
 * file, never the web client.
 *
 * @param iconBaseUrl  host of the game icons; an icon URL is {@code <iconBaseUrl>/<icon>.jpg}
 * @param fallbackIcon icon for anything the data gives no icon for
 * @param factions     name of each faction ({@code ALLIANCE}, {@code HORDE})
 * @param features     tools of the area shown on its home
 * @param news         news of the launcher panel
 * @param talents      rules of the talent calculator
 * @param spellbook    texts of the spellbook
 * @param legacy       texts of the Legacy System
 *
 * @author adrianpmdev
 */
public record GameDataConfig(
        String iconBaseUrl,
        String fallbackIcon,
        Map<String, String> factions,
        List<Feature> features,
        List<NewsItem> news,
        Talents talents,
        Spellbook spellbook,
        Legacy legacy
) {

    /**
     * A tool of the area.
     *
     * @param id          id, also its route segment
     * @param label       name
     * @param description one line about it
     * @param available   false while it is not built yet (shown as coming soon)
     */
    public record Feature(String id, String label, String description, boolean available) {
    }

    /**
     * A news item of the launcher.
     *
     * @param id    id
     * @param date  date as text, optional
     * @param title title
     * @param image picture URL, optional
     */
    public record NewsItem(int id, String date, String title, String image) {
    }

    /**
     * Rules of the talent calculator.
     *
     * @param firstPointLevel first level that gives a talent point
     * @param lastPointLevel  last level that gives a talent point (one point per level in between)
     * @param pointsPerRow    points that open each row (row 2 needs this many in the rows above, row 3 twice...)
     * @param treeIds         id of each tree in the share codes, by {@code <class>-<tree>} (the game's own ids)
     */
    public record Talents(int firstPointLevel, int lastPointLevel, int pointsPerRow, Map<String, Integer> treeIds) {
    }

    /**
     * Texts of the spellbook.
     *
     * @param generalTabName   name of the tab with the spells every character has
     * @param learnedFromQuest where a rank taught by a class quest is learned
     * @param learnedFrom      where a rank is learned, by the trainer code of the data
     */
    public record Spellbook(String generalTabName, String learnedFromQuest, Map<String, String> learnedFrom) {
    }

    /**
     * Texts of the Legacy System.
     *
     * @param placeholderPerkName name of a slot kept for a perk still to come
     * @param challengeGroups     Spanish name of each challenge group (a group not listed keeps the data's name)
     */
    public record Legacy(String placeholderPerkName, Map<String, String> challengeGroups) {
    }
}
