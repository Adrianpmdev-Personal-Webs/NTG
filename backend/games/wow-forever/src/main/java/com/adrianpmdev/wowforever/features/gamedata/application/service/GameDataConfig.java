package com.adrianpmdev.wowforever.features.gamedata.application.service;

import java.util.List;
import java.util.Map;

/**
 * What the data file does not carry, read from {@code wow-forever/config.json}: where icons come from, the texts of
 * the area in Spanish, the rules of the talent calculator and the launcher content. Changing a text is editing that
 * file, never the web client.
 *
 * @param iconBaseUrl  host of the game icons; an icon URL is {@code <iconBaseUrl>/<icon>.jpg}
 * @param raceIconBaseUrl host of the race icons, which the game icon host lacks for some races (Night Elf, Undead and
 *                     the Skyborne of WoW Forever); the same as {@code iconBaseUrl} when left out
 * @param fallbackIcon icon for anything the data gives no icon for
 * @param factions     name of each faction ({@code ALLIANCE}, {@code HORDE})
 * @param features     tools of the area shown on its home
 * @param news         where the launcher news come from and how often they are read again
 * @param talents      rules of the talent calculator
 * @param spellbook    texts of the spellbook
 * @param legacy       texts of the Legacy System
 * @param client       where the game data is read from in the game client, and what the client does not name
 *
 * @author adrianpmdev
 */
public record GameDataConfig(
        String iconBaseUrl,
        String raceIconBaseUrl,
        String fallbackIcon,
        Map<String, String> factions,
        List<Feature> features,
        News news,
        Talents talents,
        Spellbook spellbook,
        Legacy legacy,
        Client client
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
     * Where the launcher news come from.
     *
     * @param feedUrl        RSS feed read by the backend
     * @param category       only items of this RSS category are shown
     * @param limit          most recent items shown
     * @param refreshMinutes how long a read feed is kept before it is read again (only when someone asks for it)
     * @param source         name of the source, shown as its credit
     * @param sourceUrl      page of the source the credit links to
     */
    public record News(String feedUrl, String category, int limit, int refreshMinutes, String source,
                       String sourceUrl) {
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
     * @param extraTabs        tabs of a class besides its talent trees, by class name: the client skill lines each
     *                         tab holds, by tab name (in the order they are shown)
     * @param tabIcons         icon of the tabs that are not a talent tree (General and the extra tabs), by tab name;
     *                         a talent tree tab shows the icon of its tree
     * @param learnedFromQuest where a rank taught by a class quest is learned
     * @param learnedFrom      where a rank is learned, by the trainer code of the data
     */
    public record Spellbook(String generalTabName, Map<String, Map<String, List<String>>> extraTabs,
                            Map<String, String> tabIcons, String learnedFromQuest, Map<String, String> learnedFrom) {
    }

    /**
     * Texts of the Legacy System.
     *
     * @param placeholderPerkName name of a slot kept for a perk still to come
     * @param challengeGroups     Spanish name of each challenge group (a group not listed keeps the data's name)
     */
    public record Legacy(String placeholderPerkName, Map<String, String> challengeGroups) {
    }

    /**
     * Where the game data comes from: the tables of the game client that wago.tools publishes for every build.
     *
     * @param product                  wago.tools product code of the game
     * @param classicProduct           wago.tools product code of Classic Era, which talents and spells are compared
     *                                 with
     * @param checkMinutes             how often wago.tools is asked whether there is a new build
     * @param attribution              credit line of the data
     * @param talentCurrencyId         client currency spent in the talent trees
     * @param legacyCurrencyId         client currency spent in the Legacy System perk trees
     * @param legacyChallengeFlag      achievement flag that marks a Legacy System challenge
     * @param legacyPointsPerChallenge Legacy points a challenge gives
     * @param legacyTrees              name and icon of each Legacy tree by client tree id (the client names none), in
     *                                 the order they are shown
     */
    public record Client(String product, String classicProduct, int checkMinutes, String attribution,
                         int talentCurrencyId,
                         int legacyCurrencyId, long legacyChallengeFlag, int legacyPointsPerChallenge,
                         Map<String, LegacyTree> legacyTrees) {
    }

    /**
     * A Legacy System tree as the game shows it.
     *
     * @param name name
     * @param icon icon, as the icon host names it
     */
    public record LegacyTree(String name, String icon) {
    }
}
