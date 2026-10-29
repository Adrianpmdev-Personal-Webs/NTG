package com.adrianpmdev.wowforever.features.gamedata.application.service.export;

import com.adrianpmdev.wowforever.features.gamedata.domain.SpellInfo;
import com.adrianpmdev.wowforever.features.gamedata.domain.SpellLookup;
import com.adrianpmdev.wowforever.features.gamedata.domain.SpellTextResolver;
import com.adrianpmdev.wowforever.features.gamedata.domain.TooltipFormat;
import com.adrianpmdev.wowforever.features.gamedata.infrastructure.wago.Db2Table.Row;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;

/**
 * The spells of the game client: names, icons, tooltip texts already resolved for a character of the top level, and
 * the line a tooltip shows above the text (cost, range, cast time, cooldown and requirements).
 *
 * @author adrianpmdev
 */
final class ClientSpells implements SpellLookup {

    /** The spell is passive: it shows no cost line. */
    private static final int PASSIVE_ATTRIBUTE = 0x40;
    /** The spell is a hidden helper: the game never lists it. */
    private static final int HIDDEN_ATTRIBUTE = 0x80;
    /** The spell is channeled (two attributes of the client mean it). */
    private static final int CHANNELED_ATTRIBUTES = 0x4 | 0x40;
    /** The spell can be cast outside the forms it lists: they are not a requirement. */
    private static final int FORMS_OPTIONAL_ATTRIBUTE = 0x80000;
    private static final String COMBO_POINTS = "COMBO_POINTS";
    /** The range of the spell is melee range. */
    private static final int MELEE_RANGE_FLAG = 0x1;
    private static final int ARMOR_ITEM_CLASS = 4;
    private static final int WEAPON_ITEM_CLASS = 2;
    private static final String SEPARATOR = " | ";

    private final ClientTables tables;
    private final SpellTextResolver resolver;
    private final Map<Integer, Optional<SpellInfo>> spells = new HashMap<>();
    private Indexes indexes;
    private Map<Integer, String> iconNames;

    /** The rows of every spell table by spell id (or by their own id for the shared lists). */
    private record Indexes(Map<Integer, Row> names, Map<Integer, Row> texts, Map<Integer, List<Row>> effects,
                           Map<Integer, Row> misc, Map<Integer, Row> levels, Map<Integer, Row> auraOptions,
                           Map<Integer, Row> targetRestrictions,
                           Map<Integer, Row> cooldowns, Map<Integer, List<Row>> powers, Map<Integer, Row> durations,
                           Map<Integer, Row> radii, Map<Integer, Row> ranges, Map<Integer, Row> castTimes,
                           Map<Integer, Row> variableLinks, Map<Integer, Row> variables,
                           Map<Integer, Row> shapeshifts, Map<Integer, Row> forms, Map<Integer, Row> equippedItems,
                           Map<Integer, Row> classOptions, Map<Integer, Row> powerTypes) {
    }

    /**
     * Builds the spells on the tables of one export.
     *
     * @param tables      client tables
     * @param playerLevel level the texts are read for
     */
    ClientSpells(ClientTables tables, int playerLevel) {
        this.tables = tables;
        this.resolver = new SpellTextResolver(this, playerLevel);
    }

    @Override
    public Optional<SpellInfo> spell(int id) {
        Optional<SpellInfo> spell = spells.get(id);

        if (spell == null) {
            spell = Optional.ofNullable(build(id));
            spells.put(id, spell);
        }

        return spell;
    }

    /** Name of a spell, or an empty text. */
    String name(int id) {
        Row row = indexes().names().get(id);

        return row == null ? "" : row.text("Name_lang");
    }

    /** Rank label of a spell ("Rank 3"), or an empty text. */
    String rankLabel(int id) {
        Row row = indexes().texts().get(id);

        return row == null ? "" : row.text("NameSubtext_lang");
    }

    /** Whether the spell is passive. */
    boolean passive(int id) {
        Row misc = indexes().misc().get(id);

        return misc != null && (misc.integer("Attributes_0") & PASSIVE_ATTRIBUTE) != 0;
    }

    /** Whether the spell is a hidden helper the game never lists. */
    boolean hidden(int id) {
        Row misc = indexes().misc().get(id);

        return misc != null && (misc.integer("Attributes_0") & HIDDEN_ATTRIBUTE) != 0;
    }

    /** Icon of a spell, as the icon host names it (lower case, no extension), or an empty text. */
    String icon(int id) {
        Row misc = indexes().misc().get(id);

        return misc == null ? "" : iconFile(misc.integer("SpellIconFileDataID"));
    }

    /** Icon of a client file id, as the icon host names it, or an empty text. */
    String iconFile(int fileId) {
        if (iconNames == null) {
            iconNames = new HashMap<>();

            for (Row row : tables.get(ClientTable.ICON_FILES).rows()) {
                String file = row.text("FileName").toLowerCase(Locale.ROOT);
                int dot = file.lastIndexOf('.');
                iconNames.put(row.integer("ID"), dot > 0 ? file.substring(0, dot) : file);
            }
        }

        return iconNames.getOrDefault(fileId, "");
    }

    /** The class set of a spell (which class it belongs to), or 0. */
    int classSet(int id) {
        Row row = indexes().classOptions().get(id);

        return row == null ? 0 : row.integer("SpellClassSet");
    }

    /**
     * The description of a spell as the game shows it.
     *
     * @param id spell id
     * @return the text, or an empty text for an unknown spell
     */
    String description(int id) {
        return description(id, Map.of());
    }

    /**
     * The description of a spell at one rank of a talent or perk.
     *
     * @param id     spell id
     * @param points value of each effect at that rank, by effect index
     * @return the text, or an empty text for an unknown spell
     */
    String description(int id, Map<Integer, Double> points) {
        return description(id, points, "");
    }

    /**
     * The description of a spell at one rank of a talent or perk, or a text shown instead of it with the values of
     * that spell.
     *
     * @param id       spell id
     * @param points   value of each effect at that rank, by effect index
     * @param override text shown instead of the description, or an empty text
     * @return the text, or an empty text for an unknown spell
     */
    String description(int id, Map<Integer, Double> points, String override) {
        return spell(id)
                .map(spell -> override.isEmpty() ? spell : spell.withDescription(override))
                .map(spell -> resolver.description(spell, points))
                .orElse("");
    }

    /**
     * The line above the text of an active spell: "15 Rage | Melee Range | Instant | 20 sec cooldown | Requires
     * Battle Stance". Passive spells have none.
     *
     * @param id spell id
     * @return the line, or null when the spell is passive or unknown
     */
    /**
     * The tooltip line and the description in one text, as an ability list shows it: "Instant, 3 min cooldown.
     * Instantly removes all Stun effects." A passive spell has only its description.
     *
     * @param id spell id
     * @return the text, or an empty text for an unknown spell
     */
    String abilityText(int id) {
        List<String> line = tooltipLine(id);
        String description = description(id);

        return line.isEmpty() ? description : String.join(", ", line) + ". " + description;
    }

    String costLine(int id) {
        List<String> parts = tooltipLine(id);

        return parts.isEmpty() ? null : String.join(SEPARATOR, parts);
    }

    @Override
    public List<String> tooltipLine(int id) {
        Row misc = indexes().misc().get(id);

        if (misc == null || passive(id)) {
            return List.of();
        }

        List<String> parts = new ArrayList<>(powerCosts(id));
        parts.addAll(range(misc.integer("RangeIndex")));
        parts.add((misc.integer("Attributes_1") & CHANNELED_ATTRIBUTES) != 0 ? "Channeled"
                : castTime(misc.integer("CastingTimeIndex")));
        cooldown(id).ifPresent(parts::add);
        requirements(id, misc).ifPresent(parts::add);

        return List.copyOf(parts);
    }

    @Override
    public Optional<String> cost(int id) {
        return powerCosts(id).stream().findFirst();
    }

    /* ─── Building a spell ─── */

    private SpellInfo build(int id) {
        Indexes index = indexes();
        Row name = index.names().get(id);
        Row text = index.texts().get(id);
        List<Row> effectRows = index.effects().getOrDefault(id, List.of());

        if (name == null && text == null && effectRows.isEmpty()) {
            return null;
        }

        Row misc = index.misc().get(id);
        Row levels = index.levels().get(id);
        Row aura = index.auraOptions().get(id);
        Row targets = index.targetRestrictions().get(id);
        Row variablesLink = index.variableLinks().get(id);
        Row variables = variablesLink == null ? null
                : index.variables().get(variablesLink.integer("SpellDescriptionVariablesID"));
        Row duration = misc == null ? null : index.durations().get(misc.integer("DurationIndex"));
        Row range = misc == null ? null : index.ranges().get(misc.integer("RangeIndex"));
        int spellLevel = levels == null ? 0 : levels.integer("SpellLevel");

        List<SpellInfo.Effect> effects = new ArrayList<>();

        for (Row effect : effectRows) {
            effects.add(new SpellInfo.Effect(effect.integer("EffectIndex"), effect.decimal("EffectBasePointsF"),
                    effect.decimal("EffectRealPointsPerLevel"), effect.decimal("Variance"),
                    effect.integer("EffectAuraPeriod"), effect.decimal("EffectAmplitude"),
                    effect.integer("EffectChainTargets"),
                    Math.max(radius(effect.integer("EffectRadiusIndex_0")),
                            radius(effect.integer("EffectRadiusIndex_1")))));
        }

        return new SpellInfo(id, name == null ? "" : name.text("Name_lang"),
                text == null ? "" : text.text("Description_lang"),
                text == null ? "" : text.text("AuraDescription_lang"),
                variables == null ? "" : variables.text("Variables"),
                duration == null ? 0 : duration.integer("Duration"),
                spellLevel > 0 ? spellLevel : levels == null ? 0 : levels.integer("BaseLevel"),
                levels == null ? 0 : levels.integer("MaxLevel"),
                range == null ? 0 : range.decimal("RangeMax_0"),
                aura == null ? 0 : aura.integer("CumulativeAura"),
                aura == null ? 0 : aura.integer("ProcCharges"),
                aura == null ? 0 : aura.integer("ProcChance"),
                aura == null ? 0 : aura.integer("ProcCategoryRecovery"),
                targets == null ? 0 : targets.integer("MaxTargets"),
                List.copyOf(effects));
    }

    private double radius(int radiusIndex) {
        Row radius = indexes().radii().get(radiusIndex);

        return radius == null ? 0 : radius.decimal("Radius");
    }

    /* ─── Cost line ─── */

    private List<String> powerCosts(int id) {
        List<String> costs = new ArrayList<>();
        List<Row> powers = new ArrayList<>(indexes().powers().getOrDefault(id, List.of()));
        powers.sort(Comparator.comparingInt(row -> row.integer("OrderIndex")));

        for (Row power : powers) {
            Row type = indexes().powerTypes().get(power.integer("PowerType"));
            int modifier = type == null ? 1 : Math.max(1, type.integer("DisplayModifier"));
            String tag = type == null ? "" : type.text("NameGlobalStringTag");
            String powerName = capitalized(tag);
            String cost = TooltipFormat.number((double) power.integer("ManaCost") / modifier);

            if (power.integer("ManaCost") > 0 && COMBO_POINTS.equals(tag)) {
                costs.add(cost + " to " + type.integer("MaxBasePower") + " " + powerName);
            } else if (power.integer("ManaCost") > 0) {
                costs.add(cost + " " + powerName);
            } else if (power.decimal("PowerCostPct") > 0) {
                costs.add(TooltipFormat.number(power.decimal("PowerCostPct")) + "% of base "
                        + powerName.toLowerCase(Locale.ROOT));
            }
        }

        return costs;
    }

    private List<String> range(int rangeIndex) {
        Row range = indexes().ranges().get(rangeIndex);

        if (range == null) {
            return List.of();
        }

        String enemy = yards(range.decimal("RangeMin_0"), range.decimal("RangeMax_0"));
        String friendly = yards(range.decimal("RangeMin_1"), range.decimal("RangeMax_1"));

        if ((range.integer("Flags") & MELEE_RANGE_FLAG) != 0) {
            return List.of("Melee Range");
        }

        if (enemy.isEmpty() && friendly.isEmpty()) {
            return List.of();
        }

        if (enemy.equals(friendly) || friendly.isEmpty()) {
            return List.of(enemy + " yd range");
        }

        return List.of("Enemy: " + enemy + " yd range", "Friendly: " + friendly + " yd range");
    }

    /** A range in yards, "8-45" when it has a minimum, or an empty text when it has none. */
    private static String yards(double min, double max) {
        if (max <= 0) {
            return "";
        }

        return (min > 0 ? TooltipFormat.number(min) + "-" : "") + TooltipFormat.number(max);
    }

    private String castTime(int castTimeIndex) {
        Row castTime = indexes().castTimes().get(castTimeIndex);
        int milliseconds = castTime == null ? 0 : castTime.integer("Base");

        return milliseconds <= 0 ? "Instant" : TooltipFormat.number(milliseconds / 1000.0) + " sec cast";
    }

    private Optional<String> cooldown(int id) {
        Row cooldown = indexes().cooldowns().get(id);
        int milliseconds = cooldown == null ? 0
                : Math.max(cooldown.integer("RecoveryTime"), cooldown.integer("CategoryRecoveryTime"));

        return milliseconds <= 0 ? Optional.empty()
                : Optional.of(TooltipFormat.duration(milliseconds / 1000.0) + " cooldown");
    }

    /**
     * What the spell needs: the forms or stances it can only be cast in, a shield, or the one kind of weapon it needs
     * (a spell that takes any weapon of a kind, such as any melee weapon, shows no requirement).
     */
    private Optional<String> requirements(int id, Row misc) {
        List<String> required = new ArrayList<>();
        Row shapeshift = indexes().shapeshifts().get(id);

        if (shapeshift != null && (misc.integer("Attributes_2") & FORMS_OPTIONAL_ATTRIBUTE) == 0) {
            long mask = shapeshift.longValue("ShapeshiftMask_0");

            for (int bit = 0; bit < Integer.SIZE; bit++) {
                Row form = (mask & (1L << bit)) == 0 ? null : indexes().forms().get(bit + 1);

                if (form != null) {
                    required.add(form.text("Name_lang"));
                }
            }
        }

        Row items = indexes().equippedItems().get(id);

        if (items != null) {
            int itemClass = items.integer("EquippedItemClass");

            List<String> names = itemNames(itemClass, items.longValue("EquippedItemSubclass"));

            if (itemClass == ARMOR_ITEM_CLASS || (itemClass == WEAPON_ITEM_CLASS && names.size() == 1)) {
                required.addAll(names);
            }
        }

        return required.isEmpty() ? Optional.empty() : Optional.of("Requires " + String.join(", ", required));
    }

    private List<String> itemNames(int itemClass, long subclassMask) {
        List<String> names = new ArrayList<>();

        for (Row subclass : tables.get(ClientTable.ITEM_SUB_CLASS).rows()) {
            int bit = subclass.integer("SubClassID");

            if (subclass.integer("ClassID") == itemClass && bit >= 0 && bit < Long.SIZE
                    && (subclassMask & (1L << bit)) != 0) {
                String verbose = subclass.text("VerboseName_lang");
                names.add(verbose.isEmpty() ? subclass.text("DisplayName_lang") : verbose);
            }
        }

        return names;
    }

    /** A client tag as words: COMBO_POINTS reads Combo Points. */
    private static String capitalized(String tag) {
        StringJoiner words = new StringJoiner(" ");

        for (String word : tag.toLowerCase(Locale.ROOT).split("_")) {
            if (!word.isEmpty()) {
                words.add(Character.toUpperCase(word.charAt(0)) + word.substring(1));
            }
        }

        return words.toString();
    }

    /* ─── Indexes ─── */

    private Indexes indexes() {
        if (indexes == null) {
            indexes = new Indexes(
                    tables.get(ClientTable.SPELL_NAME).indexBy("ID"),
                    tables.get(ClientTable.SPELL).indexBy("ID"),
                    tables.get(ClientTable.SPELL_EFFECT).groupBy("SpellID"),
                    tables.get(ClientTable.SPELL_MISC).indexBy("SpellID"),
                    tables.get(ClientTable.SPELL_LEVELS).indexBy("SpellID"),
                    tables.get(ClientTable.SPELL_AURA_OPTIONS).indexBy("SpellID"),
                    tables.get(ClientTable.SPELL_TARGET_RESTRICTIONS).indexBy("SpellID"),
                    tables.get(ClientTable.SPELL_COOLDOWNS).indexBy("SpellID"),
                    tables.get(ClientTable.SPELL_POWER).groupBy("SpellID"),
                    tables.get(ClientTable.SPELL_DURATION).indexBy("ID"),
                    tables.get(ClientTable.SPELL_RADIUS).indexBy("ID"),
                    tables.get(ClientTable.SPELL_RANGE).indexBy("ID"),
                    tables.get(ClientTable.SPELL_CAST_TIMES).indexBy("ID"),
                    tables.get(ClientTable.SPELL_X_DESCRIPTION_VARIABLES).indexBy("SpellID"),
                    tables.get(ClientTable.SPELL_DESCRIPTION_VARIABLES).indexBy("ID"),
                    tables.get(ClientTable.SPELL_SHAPESHIFT).indexBy("SpellID"),
                    tables.get(ClientTable.SPELL_SHAPESHIFT_FORM).indexBy("ID"),
                    tables.get(ClientTable.SPELL_EQUIPPED_ITEMS).indexBy("SpellID"),
                    tables.get(ClientTable.SPELL_CLASS_OPTIONS).indexBy("SpellID"),
                    tables.get(ClientTable.POWER_TYPE).indexBy("PowerTypeEnum"));
        }

        return indexes;
    }
}
