package com.adrianpmdev.wowforever.features.gamedata.application.service;

import com.adrianpmdev.wowforever.features.gamedata.api.dto.response.GameDataResponses.Ability;
import com.adrianpmdev.wowforever.features.gamedata.api.dto.response.GameDataResponses.ChallengeGroup;
import com.adrianpmdev.wowforever.features.gamedata.api.dto.response.GameDataResponses.ClassRacials;
import com.adrianpmdev.wowforever.features.gamedata.api.dto.response.GameDataResponses.ClassSpellbook;
import com.adrianpmdev.wowforever.features.gamedata.api.dto.response.GameDataResponses.ClassTalents;
import com.adrianpmdev.wowforever.features.gamedata.api.dto.response.GameDataResponses.DataCredit;
import com.adrianpmdev.wowforever.features.gamedata.api.dto.response.GameDataResponses.GameClass;
import com.adrianpmdev.wowforever.features.gamedata.api.dto.response.GameDataResponses.LegacyChallenge;
import com.adrianpmdev.wowforever.features.gamedata.api.dto.response.GameDataResponses.LegacyData;
import com.adrianpmdev.wowforever.features.gamedata.api.dto.response.GameDataResponses.Race;
import com.adrianpmdev.wowforever.features.gamedata.api.dto.response.GameDataResponses.RacialsData;
import com.adrianpmdev.wowforever.features.gamedata.api.dto.response.GameDataResponses.Spell;
import com.adrianpmdev.wowforever.features.gamedata.api.dto.response.GameDataResponses.SpellRank;
import com.adrianpmdev.wowforever.features.gamedata.api.dto.response.GameDataResponses.SpellbookTab;
import com.adrianpmdev.wowforever.features.gamedata.api.dto.response.GameDataResponses.Talent;
import com.adrianpmdev.wowforever.features.gamedata.api.dto.response.GameDataResponses.TalentTree;
import com.adrianpmdev.wowforever.features.gamedata.api.dto.response.GameDataResponses.TooltipLine;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Turns the sections of the data file (the export of talentsforever.com, kept as it is) into the API models. This is
 * the only class that knows the format of the file: if it changes, only this class changes. Every field a hand-edited
 * file might leave out has a safe default, so a missing value shows as empty instead of breaking a screen.
 *
 * @author adrianpmdev
 */
final class GameDataMapper {

    /** Talent ids are {@code <tree id> * 100 + <position>}, unique in a class as long as a tree has under 100. */
    private static final int TALENT_ID_FACTOR = 100;

    private static final Pattern BUILD_PATTERN = Pattern.compile("\\d+\\.\\d+\\.\\d+\\.\\d+");
    private static final Pattern RANK_PATTERN = Pattern.compile("(\\d+)");
    private static final Pattern LEVEL_PATTERN = Pattern.compile("level (\\d+)", Pattern.CASE_INSENSITIVE);
    private static final Set<String> CLASSIC_STATUSES = Set.of("same", "moved", "changed", "new");
    private static final String SPELL_KEY_SEPARATOR = "|";
    private static final String QUEST_SOURCE = "quest";
    private static final String GATE_ROWS_ABOVE = "rows-above";
    private static final String GATE_TREE = "tree";

    private final GameDataConfig config;

    /**
     * Builds the mapper.
     *
     * @param config texts and rules the file does not carry
     */
    GameDataMapper(GameDataConfig config) {
        this.config = config;
    }

    /* ─── Shared ─── */

    /** Lower case, words joined by dashes: "Night Elf" becomes "night-elf". */
    static String slugify(String text) {
        return text.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("^-+|-+$", "");
    }

    /** Full URL of a game icon, or of the question mark when the file gives none. */
    String iconUrl(String icon) {
        String name = icon == null || icon.isBlank() ? config.fallbackIcon() : icon;

        return config.iconBaseUrl() + "/" + name + ".jpg";
    }

    GameClass gameClass(String slug, String name, String icon) {
        return new GameClass(slug, name, iconUrl(icon));
    }

    /** Credit of the data, with the first game build (1.60.1.70425) found in the sources. */
    DataCredit credit(JsonNode root, String... sources) {
        String build = null;

        for (String source : sources) {
            Matcher matcher = source == null ? null : BUILD_PATTERN.matcher(source);

            if (matcher != null && matcher.find()) {
                build = matcher.group();
                break;
            }
        }

        return new DataCredit(root.path("attribution").asText(""), root.path("generated").asText(""), build);
    }

    private Ability ability(JsonNode node) {
        return new Ability(node.path(0).asText(""), node.path(1).asText(""), iconUrl(node.path(2).asText("")));
    }

    private List<Ability> abilities(JsonNode array) {
        List<Ability> abilities = new ArrayList<>();
        array.forEach(node -> abilities.add(ability(node)));

        return abilities;
    }

    private static String classicStatus(JsonNode node) {
        String status = node.asText(null);

        return status != null && CLASSIC_STATUSES.contains(status) ? status : null;
    }

    private static String textOrNull(JsonNode node) {
        String text = node.asText(null);

        return text == null || text.isEmpty() ? null : text;
    }

    private static List<String> strings(JsonNode array) {
        List<String> values = new ArrayList<>();
        array.forEach(node -> values.add(node.asText("")));

        return values;
    }

    /* ─── Trees (talents and Legacy perks) ─── */

    /** One item of a tree as the file gives it (row and column one based). */
    private record TreeItem(String name, int maxRanks, int row, int column, String icon, List<String> descriptions,
                            String requires, int pointsRequired, String cost, String classic) {
    }

    /**
     * Builds a tree: items in share code order (top row first, left to right), prerequisites resolved by name inside
     * the tree. A prerequisite that does not exist is dropped, and so is one of two items that need each other, so a
     * hand-edited file can never lock a tree.
     */
    private TalentTree tree(int id, String name, String icon, String gateScope, List<TreeItem> source) {
        List<TreeItem> items = new ArrayList<>(source);
        items.sort(Comparator.comparingInt(TreeItem::row).thenComparingInt(TreeItem::column));

        Map<String, Integer> positionByName = new HashMap<>();

        for (int position = 0; position < items.size(); position++) {
            positionByName.putIfAbsent(items.get(position).name(), position);
        }

        List<Talent> talents = new ArrayList<>();
        int rows = 1;
        int columns = 1;

        for (int position = 0; position < items.size(); position++) {
            TreeItem item = items.get(position);
            Integer required = requiredPosition(items, positionByName, position);
            // Two items that need each other: only the one further down (or to the right) keeps its prerequisite.
            boolean mutual = required != null
                    && Integer.valueOf(position).equals(requiredPosition(items, positionByName, required));
            List<Integer> requires = required == null || (mutual && required > position)
                    ? List.of()
                    : List.of(id * TALENT_ID_FACTOR + required);
            int row = Math.max(0, item.row() - 1);
            int column = Math.max(0, item.column() - 1);

            talents.add(new Talent(id * TALENT_ID_FACTOR + position, id, item.name(), row, column,
                    Math.max(0, item.maxRanks()), Math.max(0, item.pointsRequired()), requires, iconUrl(item.icon()),
                    item.descriptions(), item.cost(), item.classic()));
            rows = Math.max(rows, row + 1);
            columns = Math.max(columns, column + 1);
        }

        return new TalentTree(id, slugify(name), name, iconUrl(icon), rows, columns, gateScope, talents);
    }

    private static Integer requiredPosition(List<TreeItem> items, Map<String, Integer> positionByName, int position) {
        String requires = items.get(position).requires();
        Integer required = requires == null ? null : positionByName.get(requires);

        return required == null || required == position ? null : required;
    }

    /* ─── Talents ─── */

    ClassTalents classTalents(JsonNode root, GameClass gameClass, JsonNode section) {
        List<TalentTree> trees = new ArrayList<>();
        int index = 0;

        for (JsonNode tree : section.path("trees")) {
            index++;
            String treeName = tree.path("name").asText("");
            int treeId = config.talents().treeIds().getOrDefault(gameClass.slug() + "-" + slugify(treeName), index);
            List<TreeItem> items = new ArrayList<>();

            for (JsonNode talent : tree.path("talents")) {
                int row = talent.path("row").asInt(1);
                items.add(new TreeItem(talent.path("name").asText(""), talent.path("max").asInt(0), row,
                        talent.path("col").asInt(1), talent.path("icon").asText(""), strings(talent.path("desc")),
                        talent.path("req").asText(null), (row - 1) * config.talents().pointsPerRow(),
                        talent.path("cost").asText(null), classicStatus(talent.path("classic").path("status"))));
            }

            trees.add(tree(treeId, treeName, tree.path("icon").asText(""), GATE_ROWS_ABOVE, items));
        }

        return new ClassTalents(gameClass, trees, credit(root, section.path("source").asText(null)));
    }

    /* ─── Legacy ─── */

    LegacyData legacy(JsonNode root, JsonNode section) {
        List<TalentTree> trees = new ArrayList<>();

        for (JsonNode tree : section.path("trees")) {
            List<TreeItem> items = new ArrayList<>();

            for (JsonNode perk : tree.path("perks")) {
                boolean placeholder = perk.path("placeholder").asBoolean(false);
                items.add(new TreeItem(
                        placeholder ? config.legacy().placeholderPerkName() : perk.path("name").asText(""),
                        placeholder ? 0 : perk.path("max").asInt(0), perk.path("row").asInt(1),
                        perk.path("col").asInt(1), perk.path("icon").asText(""), strings(perk.path("ranks")),
                        placeholder ? null : perk.path("req").asText(null), perk.path("gate").asInt(0),
                        perk.path("cost").asText(null), null));
            }

            trees.add(tree(tree.path("id").asInt(0), tree.path("name").asText(""), tree.path("icon").asText(""),
                    GATE_TREE, items));
        }

        Map<String, List<LegacyChallenge>> challengesByGroup = new LinkedHashMap<>();

        for (JsonNode challenge : section.path("challenges")) {
            challengesByGroup.computeIfAbsent(challenge.path("group").asText(""), group -> new ArrayList<>())
                    .add(new LegacyChallenge(challenge.path("name").asText(""), challenge.path("desc").asText(""),
                            challenge.path("points").asInt(1)));
        }

        List<ChallengeGroup> groups = new ArrayList<>();
        int challengePoints = 0;

        for (Map.Entry<String, List<LegacyChallenge>> entry : challengesByGroup.entrySet()) {
            int points = entry.getValue().stream().mapToInt(LegacyChallenge::points).sum();
            String name = config.legacy().challengeGroups().getOrDefault(entry.getKey(), entry.getKey());
            groups.add(new ChallengeGroup(name, entry.getValue(), points));
            challengePoints += points;
        }

        return new LegacyData(section.path("points").asInt(0), trees, groups, challengePoints,
                credit(root, section.path("source").asText(null)));
    }

    /* ─── Spellbook ─── */

    ClassSpellbook classSpellbook(JsonNode root, GameClass gameClass, String className) {
        JsonNode book = root.path("spellbooks").path(className);
        JsonNode texts = root.path("spell_desc");
        String textPrefix = className + SPELL_KEY_SEPARATOR;

        Set<String> fromTalent = new HashSet<>(strings(book.path("talents")));
        List<SpellbookTab> tabs = new ArrayList<>();
        tabs.add(new SpellbookTab(config.spellbook().generalTabName(),
                spells(book.path("general"), book, texts, textPrefix, fromTalent)));

        for (JsonNode tab : book.path("tabs")) {
            tabs.add(new SpellbookTab(tab.path("name").asText(""),
                    spells(tab.path("spells"), book, texts, textPrefix, fromTalent)));
        }

        tabs.removeIf(tab -> tab.spells().isEmpty());

        return new ClassSpellbook(gameClass, tabs, abilities(root.path("class_abilities").path(className)),
                strings(book.path("gone")), credit(root, book.path("build").asText(null)));
    }

    /** One spell per name, its ranks in order, in the order the book first lists each name. */
    private List<Spell> spells(JsonNode refs, JsonNode book, JsonNode texts, String textPrefix, Set<String> fromTalent) {
        Map<String, List<String>> ranksByName = new LinkedHashMap<>();

        for (JsonNode ref : refs) {
            ranksByName.computeIfAbsent(ref.path(0).asText(""), name -> new ArrayList<>()).add(ref.path(1).asText(""));
        }

        List<Spell> spells = new ArrayList<>();

        ranksByName.forEach((name, labels) -> {
            List<String> sorted = new ArrayList<>(labels);
            sorted.sort(Comparator.comparingInt(GameDataMapper::rankNumber));
            JsonNode levels = book.path("levels").path(name);
            List<SpellRank> ranks = new ArrayList<>();

            for (int index = 0; index < sorted.size(); index++) {
                String label = sorted.get(index);
                JsonNode fallbackLevel = levels.path(index);
                ranks.add(rank(label, texts.path(textPrefix + name + SPELL_KEY_SEPARATOR + label),
                        fallbackLevel.isNumber() ? fallbackLevel.asInt() : null));
            }

            spells.add(new Spell(name, iconUrl(book.path("icons").path(name).asText("")), fromTalent.contains(name),
                    ranks));
        });

        return spells;
    }

    private SpellRank rank(String label, JsonNode text, Integer fallbackLevel) {
        List<TooltipLine> lines = new ArrayList<>();

        for (JsonNode line : text.path("l")) {
            String left = line.path(0).asText("");
            String right = line.path(1).asText("");

            if (!left.isEmpty() || !right.isEmpty()) {
                lines.add(new TooltipLine(left, right));
            }
        }

        return new SpellRank(label, levelOf(text, fallbackLevel), text.path("d").asText(""), lines,
                textOrNull(text.path("sc")), textOrNull(text.path("co")), classicStatus(text.path("cs")),
                textOrNull(text.path("cd")), learnedFrom(text), textOrNull(text.path("fx")));
    }

    private String learnedFrom(JsonNode text) {
        if (QUEST_SOURCE.equals(text.path("ns").asText(null))) {
            return config.spellbook().learnedFromQuest();
        }

        JsonNode trainer = text.path("nt");

        return trainer.isMissingNode() || trainer.isNull() ? null : config.spellbook().learnedFrom().get(trainer.asText());
    }

    private static Integer levelOf(JsonNode text, Integer fallback) {
        String level = text.path("lv").asText(null);
        Matcher matcher = level == null ? null : LEVEL_PATTERN.matcher(level);

        return matcher != null && matcher.find() ? Integer.valueOf(matcher.group(1)) : fallback;
    }

    private static int rankNumber(String label) {
        Matcher matcher = RANK_PATTERN.matcher(label);

        return matcher.find() ? Integer.parseInt(matcher.group(1)) : 0;
    }

    /* ─── Racials ─── */

    RacialsData racials(JsonNode root) {
        List<Race> races = new ArrayList<>();
        JsonNode classRacials = root.path("class_racials");

        for (Map.Entry<String, JsonNode> faction : root.path("racials").properties()) {
            String factionId = "horde".equalsIgnoreCase(faction.getKey()) ? "HORDE" : "ALLIANCE";

            for (JsonNode race : faction.getValue()) {
                String raceName = race.path("race").asText("");
                List<ClassRacials> byClass = new ArrayList<>();

                for (Map.Entry<String, JsonNode> group : classRacials.properties()) {
                    List<Ability> abilities = abilities(group.getValue().path("races").path(raceName));

                    if (!abilities.isEmpty()) {
                        byClass.add(new ClassRacials(group.getKey(), abilities));
                    }
                }

                races.add(new Race(slugify(raceName), raceName, factionId, iconUrl(race.path("icon").asText("")),
                        strings(race.path("classes")), abilities(race.path("abilities")), byClass));
            }
        }

        return new RacialsData(races, credit(root));
    }
}
