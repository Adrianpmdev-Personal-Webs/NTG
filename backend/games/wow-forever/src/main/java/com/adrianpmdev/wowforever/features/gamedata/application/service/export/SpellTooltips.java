package com.adrianpmdev.wowforever.features.gamedata.application.service.export;

import com.adrianpmdev.wowforever.features.gamedata.domain.TooltipFormat;
import com.adrianpmdev.wowforever.features.gamedata.infrastructure.wago.Db2Table.Row;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;

/**
 * Builds what a spell tooltip shows above the text, as the game writes it: cost, combo points, range, cast time,
 * cooldown, requirements (forms, a shield, the one kind of weapon it needs) and reagents.
 *
 * @author adrianpmdev
 */
final class SpellTooltips {

    /** The spell is passive: its tooltip has no cost or cast time. */
    static final int PASSIVE_ATTRIBUTE = 0x40;
    /** The spell hits with the next melee swing instead of being cast. */
    private static final int NEXT_MELEE_ATTRIBUTE = 0x4;
    /** The spell is channeled (two attributes of the client mean it). */
    private static final int CHANNELED_ATTRIBUTES = 0x4 | 0x40;
    /** The spell can be cast outside the forms it lists: they are not a requirement. */
    private static final int FORMS_OPTIONAL_ATTRIBUTE = 0x80000;
    /** The range of the spell is melee range. */
    private static final int MELEE_RANGE_FLAG = 0x1;
    /** Ranges this long mean anywhere. */
    private static final double UNLIMITED_RANGE = 50000;
    private static final int ARMOR_ITEM_CLASS = 4;
    private static final int WEAPON_ITEM_CLASS = 2;
    private static final int REAGENT_SLOTS = 8;
    private static final int TOOL_SLOTS = 2;
    private static final String COMBO_POINTS = "COMBO_POINTS";
    private static final String NEXT_MELEE = "Next melee";
    /** Power type of spells paid with health, which the power type table does not list. */
    private static final int HEALTH_POWER = -2;

    private final ClientTables tables;
    private Indexes indexes;

    /** The rows of the tables a tooltip reads, by spell id (or by their own id for the shared lists). */
    private record Indexes(Map<Integer, Row> misc, Map<Integer, Row> cooldowns, Map<Integer, List<Row>> powers,
                           Map<Integer, Row> ranges, Map<Integer, Row> castTimes, Map<Integer, Row> shapeshifts,
                           Map<Integer, Row> forms, Map<Integer, Row> equippedItems, Map<Integer, Row> powerTypes,
                           Map<Integer, Row> reagents, Map<Integer, Row> tools, Map<Integer, Row> toolCategories,
                           Map<Integer, Row> items) {
    }

    SpellTooltips(ClientTables tables) {
        this.tables = tables;
    }

    /**
     * The tooltip of a spell.
     *
     * @param id spell id
     * @return its tooltip; a spell the client does not know reads as passive
     */
    SpellTooltip of(int id) {
        Row misc = indexes().misc().get(id);

        if (misc == null || (misc.integer("Attributes_0") & PASSIVE_ATTRIBUTE) != 0) {
            return SpellTooltip.passiveSpell();
        }

        List<String> costs = new ArrayList<>();
        String comboPoints = null;

        for (Cost cost : costs(id)) {
            if (cost.comboPoints()) {
                comboPoints = cost.text();
            } else {
                costs.add(cost.text());
            }
        }

        String castTime = castTime(misc);
        List<String> ranges = NEXT_MELEE.equals(castTime) ? List.of() : ranges(misc.integer("RangeIndex"));

        return new SpellTooltip(false, List.copyOf(costs), comboPoints, ranges, castTime, cooldown(id),
                requirements(id, misc), reagents(id));
    }

    /** What casting a spell costs, the first cost only ("55% of base Mana"), or empty. */
    Optional<String> firstCost(int id) {
        return costs(id).stream().map(Cost::text).findFirst();
    }

    /* ─── Parts ─── */

    /** One cost of a spell. */
    private record Cost(String text, boolean comboPoints) {
    }

    private List<Cost> costs(int id) {
        List<Cost> costs = new ArrayList<>();
        List<Row> powers = new ArrayList<>(indexes().powers().getOrDefault(id, List.of()));
        powers.sort(Comparator.comparingInt(row -> row.integer("OrderIndex")));

        for (Row power : powers) {
            Row type = indexes().powerTypes().get(power.integer("PowerType"));
            int modifier = type == null ? 1 : Math.max(1, type.integer("DisplayModifier"));
            String tag = type != null ? type.text("NameGlobalStringTag")
                    : power.integer("PowerType") == HEALTH_POWER ? "HEALTH" : "";
            String amount = TooltipFormat.text((double) power.integer("ManaCost") / modifier);
            String perSecond = power.integer("ManaPerSecond") > 0
                    ? ", plus " + TooltipFormat.text((double) power.integer("ManaPerSecond") / modifier) + " per sec"
                    : "";

            if (power.integer("ManaCost") > 0 && COMBO_POINTS.equals(tag)) {
                costs.add(new Cost(amount + " to " + type.integer("MaxBasePower") + " " + words(tag), true));
            } else if (power.integer("ManaCost") > 0) {
                costs.add(new Cost(amount + " " + words(tag) + perSecond, false));
            } else if (power.decimal("PowerCostPct") > 0) {
                costs.add(new Cost(TooltipFormat.number(power.decimal("PowerCostPct")) + "% of base "
                        + words(tag).toLowerCase(Locale.ROOT), false));
            }
        }

        return costs;
    }

    private List<String> ranges(int rangeIndex) {
        Row range = indexes().ranges().get(rangeIndex);

        if (range == null) {
            return List.of();
        }

        if ((range.integer("Flags") & MELEE_RANGE_FLAG) != 0) {
            return List.of("Melee Range");
        }

        if (range.decimal("RangeMax_0") >= UNLIMITED_RANGE) {
            return List.of("Unlimited range");
        }

        String enemy = yards(range.decimal("RangeMin_0"), range.decimal("RangeMax_0"));
        String friendly = yards(range.decimal("RangeMin_1"), range.decimal("RangeMax_1"));

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

    private String castTime(Row misc) {
        if ((misc.integer("Attributes_0") & NEXT_MELEE_ATTRIBUTE) != 0) {
            return NEXT_MELEE;
        }

        if ((misc.integer("Attributes_1") & CHANNELED_ATTRIBUTES) != 0) {
            return "Channeled";
        }

        Row castTime = indexes().castTimes().get(misc.integer("CastingTimeIndex"));
        int milliseconds = castTime == null ? 0 : castTime.integer("Base");

        return milliseconds <= 0 ? "Instant" : TooltipFormat.number(milliseconds / 1000.0) + " sec cast";
    }

    private String cooldown(int id) {
        Row cooldown = indexes().cooldowns().get(id);
        int milliseconds = cooldown == null ? 0
                : Math.max(cooldown.integer("RecoveryTime"), cooldown.integer("CategoryRecoveryTime"));

        return milliseconds <= 0 ? null : TooltipFormat.duration(milliseconds / 1000.0) + " cooldown";
    }

    /**
     * What the spell needs, one line each: the forms or stances it can only be cast in, then a shield or the one kind
     * of weapon it needs (a spell that takes any weapon of a kind, such as any melee weapon, shows no requirement).
     */
    private List<String> requirements(int id, Row misc) {
        List<String> lines = new ArrayList<>();
        List<String> forms = new ArrayList<>();
        Row shapeshift = indexes().shapeshifts().get(id);

        if (shapeshift != null && (misc.integer("Attributes_2") & FORMS_OPTIONAL_ATTRIBUTE) == 0) {
            long mask = shapeshift.longValue("ShapeshiftMask_0");

            for (int bit = 0; bit < Integer.SIZE; bit++) {
                Row form = (mask & (1L << bit)) == 0 ? null : indexes().forms().get(bit + 1);

                if (form != null) {
                    forms.add(form.text("Name_lang"));
                }
            }
        }

        if (!forms.isEmpty()) {
            lines.add("Requires " + String.join(", ", forms));
        }

        Row items = indexes().equippedItems().get(id);

        if (items != null) {
            int itemClass = items.integer("EquippedItemClass");
            List<String> names = itemNames(itemClass, items.longValue("EquippedItemSubclass"));

            if (!names.isEmpty() && (itemClass == ARMOR_ITEM_CLASS
                    || (itemClass == WEAPON_ITEM_CLASS && names.size() == 1))) {
                lines.add("Requires " + String.join(", ", names));
            }
        }

        return List.copyOf(lines);
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

    /** "Reagents: Dust of Decay x3, Leaded Vial" and "Tools: Water Totem". */
    private List<String> reagents(int id) {
        List<String> lines = new ArrayList<>();
        Row reagents = indexes().reagents().get(id);

        if (reagents != null) {
            StringJoiner names = new StringJoiner(", ");

            for (int slot = 0; slot < REAGENT_SLOTS; slot++) {
                String name = itemName(reagents.integer("Reagent_" + slot));
                int count = reagents.integer("ReagentCount_" + slot);

                if (!name.isEmpty()) {
                    names.add(count > 1 ? name + " x" + count : name);
                }
            }

            if (names.length() > 0) {
                lines.add("Reagents: " + names);
            }
        }

        Row tools = indexes().tools().get(id);

        if (tools != null) {
            StringJoiner names = new StringJoiner(", ");

            for (int slot = 0; slot < TOOL_SLOTS; slot++) {
                Row category = indexes().toolCategories().get(tools.integer("RequiredTotemCategoryID_" + slot));
                String name = category == null ? itemName(tools.integer("Totem_" + slot)) : category.text("Name_lang");

                if (!name.isEmpty()) {
                    names.add(name);
                }
            }

            if (names.length() > 0) {
                lines.add("Tools: " + names);
            }
        }

        return List.copyOf(lines);
    }

    private String itemName(int itemId) {
        Row item = itemId == 0 ? null : indexes().items().get(itemId);

        return item == null ? "" : item.text("Display_lang");
    }

    /** A client tag as words: COMBO_POINTS reads Combo Points. */
    private static String words(String tag) {
        StringJoiner words = new StringJoiner(" ");

        for (String word : tag.toLowerCase(Locale.ROOT).split("_")) {
            if (!word.isEmpty()) {
                words.add(Character.toUpperCase(word.charAt(0)) + word.substring(1));
            }
        }

        return words.toString();
    }

    private Indexes indexes() {
        if (indexes == null) {
            indexes = new Indexes(
                    tables.get(ClientTable.SPELL_MISC).indexBy("SpellID"),
                    tables.get(ClientTable.SPELL_COOLDOWNS).indexBy("SpellID"),
                    tables.get(ClientTable.SPELL_POWER).groupBy("SpellID"),
                    tables.get(ClientTable.SPELL_RANGE).indexBy("ID"),
                    tables.get(ClientTable.SPELL_CAST_TIMES).indexBy("ID"),
                    tables.get(ClientTable.SPELL_SHAPESHIFT).indexBy("SpellID"),
                    tables.get(ClientTable.SPELL_SHAPESHIFT_FORM).indexBy("ID"),
                    tables.get(ClientTable.SPELL_EQUIPPED_ITEMS).indexBy("SpellID"),
                    tables.get(ClientTable.POWER_TYPE).indexBy("PowerTypeEnum"),
                    tables.get(ClientTable.SPELL_REAGENTS).indexBy("SpellID"),
                    tables.get(ClientTable.SPELL_TOTEMS).indexBy("SpellID"),
                    tables.get(ClientTable.TOTEM_CATEGORY).indexBy("ID"),
                    tables.get(ClientTable.ITEM_NAMES).indexBy("ID"));
        }

        return indexes;
    }
}
