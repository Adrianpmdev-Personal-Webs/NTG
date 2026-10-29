package com.adrianpmdev.wowforever.features.gamedata.api.dto.response;

import java.util.List;
import java.util.Map;

/**
 * Responses of the WoW Forever game data API: the data file already turned into the models every web client works with
 * (texts in Spanish, full icon URLs, prerequisites resolved), so a client only shows it.
 *
 * @author adrianpmdev
 */
public final class GameDataResponses {

    private GameDataResponses() {
    }

    /* ─── Shared ─── */

    /**
     * A playable class.
     *
     * @param slug lowercase name for URLs, for example {@code warrior}
     * @param name name as the game writes it
     * @param icon icon URL
     */
    public record GameClass(String slug, String name, String icon) {
    }

    /**
     * What the licence of the data asks to show, plus the game build it comes from when the file says so.
     *
     * @param attribution credit line
     * @param generated   day the data was generated
     * @param build       game build, or null
     */
    public record DataCredit(String attribution, String generated, String build) {
    }

    /**
     * An ability written as name, text and icon.
     *
     * @param name     name
     * @param text     description
     * @param icon     icon URL
     * @param cooldown cooldown of an active ability ("2 min", "10 sec"), or null when it is passive or has none
     */
    public record Ability(String name, String text, String icon, String cooldown) {
    }

    /* ─── Index ─── */

    /**
     * Everything the home and the class pickers need.
     *
     * @param credit            credit of the data
     * @param classes           playable classes, in file order
     * @param features          tools of the area
     * @param factions          name of each faction by id ({@code ALLIANCE}, {@code HORDE})
     * @param talentPointLevels levels that give a talent point, lowest first
     */
    public record GameIndex(DataCredit credit, List<GameClass> classes, List<Feature> features,
                            Map<String, String> factions, List<Integer> talentPointLevels) {
    }

    /**
     * A tool of the area.
     *
     * @param id          id, also its route segment
     * @param label       name
     * @param description one line about it
     * @param available   false while it is not built yet
     */
    public record Feature(String id, String label, String description, boolean available) {
    }

    /* ─── Talent trees (talents and Legacy perks) ─── */

    /**
     * A talent (or a Legacy perk).
     *
     * @param id             unique in its class (or in the Legacy trees): tree id * 100 + position in the share code
     * @param treeId         tree it belongs to
     * @param name           name
     * @param row            zero based, top row first
     * @param column         zero based, left column first
     * @param maxRanks       points it takes; 0 means it cannot be bought (a slot kept for something to come)
     * @param pointsRequired points that must already be spent before it opens (see {@link TalentTree#gateScope()})
     * @param requires       talents of the same tree it needs at full rank (any one of them)
     * @param icon           icon URL
     * @param descriptions   text of each rank
     * @param cost           cost line of an active talent, or null
     * @param classic        how it compares with Classic ({@code same}, {@code moved}, {@code changed},
     *                       {@code new}), or null
     */
    public record Talent(int id, int treeId, String name, int row, int column, int maxRanks, int pointsRequired,
                         List<Integer> requires, String icon, List<String> descriptions, String cost, String classic) {
    }

    /**
     * A talent tree (or a Legacy perk tree).
     *
     * @param id        tree id (the game's own id for talents, so share codes keep their format)
     * @param slug      lowercase name, for example {@code beast-mastery}
     * @param name      name
     * @param icon      icon URL
     * @param rows      number of rows
     * @param columns   number of columns
     * @param gateScope what a point gate counts: {@code rows-above} for talents, {@code tree} for Legacy perks
     * @param talents   in share code order (top row first, left to right)
     */
    public record TalentTree(int id, String slug, String name, String icon, int rows, int columns, String gateScope,
                             List<Talent> talents) {
    }

    /**
     * Everything the talent calculator needs for one class.
     *
     * @param gameClass class
     * @param trees     its talent trees
     * @param credit    credit of the data
     */
    public record ClassTalents(GameClass gameClass, List<TalentTree> trees, DataCredit credit) {
    }

    /* ─── Spellbook ─── */

    /**
     * A line of a tooltip, with text on both sides.
     *
     * @param left  left text
     * @param right right text
     */
    public record TooltipLine(String left, String right) {
    }

    /**
     * A rank of a spell with its tooltip.
     *
     * @param label              rank as the game writes it ("Rank 3"), empty for single-rank spells
     * @param level              level it is learned at, or null
     * @param description        description
     * @param lines              tooltip lines
     * @param school             magic school, or null
     * @param coefficient        spell power coefficient, or null
     * @param classic            comparison with Classic, or null
     * @param classicDescription Classic text, or null
     * @param learnedFrom        where it is learned when it is not the class trainer, or null
     * @param note               extra note, or null
     */
    public record SpellRank(String label, Integer level, String description, List<TooltipLine> lines, String school,
                            String coefficient, String classic, String classicDescription, String learnedFrom,
                            String note) {
    }

    /**
     * A spell with its ranks.
     *
     * @param name       name
     * @param icon       icon URL
     * @param fromTalent whether a talent puts it in the book
     * @param ranks      lowest rank first
     */
    public record Spell(String name, String icon, boolean fromTalent, List<SpellRank> ranks) {
    }

    /**
     * A tab of the spellbook.
     *
     * @param name   name
     * @param icon   icon URL: the icon of the talent tree of the same name, or the one config.json gives the tab
     * @param spells spells
     */
    public record SpellbookTab(String name, String icon, List<Spell> spells) {
    }

    /**
     * The spellbook of a class.
     *
     * @param gameClass  class
     * @param tabs       tabs with spells
     * @param highlights what changed for the class, in short
     * @param removed    Classic spells the class no longer has
     * @param credit     credit of the data
     */
    public record ClassSpellbook(GameClass gameClass, List<SpellbookTab> tabs, List<Ability> highlights,
                                 List<String> removed, DataCredit credit) {
    }

    /* ─── Legacy ─── */

    /**
     * An account challenge.
     *
     * @param name        name (also what the client remembers as ticked)
     * @param description description
     * @param points      points it gives
     */
    public record LegacyChallenge(String name, String description, int points) {
    }

    /**
     * Challenges of one group.
     *
     * @param name       group name, in Spanish
     * @param challenges its challenges
     * @param points     points they give together
     */
    public record ChallengeGroup(String name, List<LegacyChallenge> challenges, int points) {
    }

    /**
     * The Legacy System.
     *
     * @param points          points every character can spend in its trees
     * @param trees           perk trees
     * @param challengeGroups challenges by group
     * @param challengePoints points all the challenges give together
     * @param credit          credit of the data
     */
    public record LegacyData(int points, List<TalentTree> trees, List<ChallengeGroup> challengeGroups,
                             int challengePoints, DataCredit credit) {
    }

    /* ─── Racials ─── */

    /**
     * Abilities of one class that change with the race.
     *
     * @param className class name
     * @param abilities abilities
     */
    public record ClassRacials(String className, List<Ability> abilities) {
    }

    /**
     * A playable race.
     *
     * @param slug         lowercase name
     * @param name         name
     * @param faction      {@code ALLIANCE} or {@code HORDE}
     * @param icon         icon URL
     * @param classNames   classes it can play
     * @param abilities    racial abilities
     * @param classRacials abilities of some classes that change with this race
     */
    public record Race(String slug, String name, String faction, String icon, List<String> classNames,
                       List<Ability> abilities, List<ClassRacials> classRacials) {
    }

    /**
     * Every race with its racial abilities.
     *
     * @param races  races of both factions
     * @param credit credit of the data
     */
    public record RacialsData(List<Race> races, DataCredit credit) {
    }
}
