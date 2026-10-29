package com.adrianpmdev.wowforever.features.gamedata.application.service.export;

import com.adrianpmdev.wowforever.features.gamedata.domain.SpellInfo;
import com.adrianpmdev.wowforever.features.gamedata.domain.SpellLookup;
import com.adrianpmdev.wowforever.features.gamedata.domain.SpellTextResolver;
import com.adrianpmdev.wowforever.features.gamedata.domain.TooltipFormat;
import com.adrianpmdev.wowforever.features.gamedata.infrastructure.wago.Db2Table.Row;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;

/**
 * The spells of the game client: names, icons, texts already resolved for a character of the top level, what their
 * tooltips show above the text (see SpellTooltips) and the spellbook details (school, spell power coefficient and the
 * level each one is learned at).
 *
 * @author adrianpmdev
 */
final class ClientSpells implements SpellLookup {

    /** The spell is a hidden helper: the game never lists it. */
    private static final int HIDDEN_ATTRIBUTE = 0x80;
    private static final int DAMAGE_EFFECT = 2;
    private static final int LEECH_EFFECT = 9;
    private static final int HEAL_EFFECT = 10;
    private static final int AURA_EFFECT = 6;
    private static final int LEARN_SPELL_EFFECT = 36;
    private static final int LEARN_PET_SPELL_EFFECT = 57;
    private static final int PERIODIC_DAMAGE_AURA = 3;
    private static final int PERIODIC_HEAL_AURA = 8;
    private static final int PERIODIC_TRIGGER_AURA = 23;
    private static final int PERIODIC_LEECH_AURA = 53;
    private static final String PER_TICK = "per tick";
    private static final String SEPARATOR = " | ";
    /** Names of the magic schools, by bit of the school mask. */
    private static final List<String> SCHOOLS = List.of("Physical", "Holy", "Fire", "Nature", "Frost", "Shadow",
            "Arcane");

    private final ClientTables tables;
    private final SpellTooltips tooltips;
    private final SpellTextResolver resolver;
    private final Map<Integer, Optional<SpellInfo>> spells = new HashMap<>();
    private Indexes indexes;
    private Map<Integer, String> iconNames;

    /** The rows of every spell table by spell id (or by their own id for the shared lists). */
    private record Indexes(Map<Integer, Row> names, Map<Integer, Row> texts, Map<Integer, List<Row>> effects,
                           Map<Integer, Row> misc, Map<Integer, Row> levels, Map<Integer, Row> auraOptions,
                           Map<Integer, Row> targetRestrictions, Map<Integer, Row> durations, Map<Integer, Row> radii,
                           Map<Integer, Row> ranges, Map<Integer, Row> variableLinks, Map<Integer, Row> variables,
                           Map<Integer, Row> classOptions) {
    }

    /**
     * Builds the spells on the tables of one export.
     *
     * @param tables      client tables
     * @param playerLevel level the texts are read for
     */
    ClientSpells(ClientTables tables, int playerLevel) {
        this.tables = tables;
        this.tooltips = new SpellTooltips(tables);
        this.resolver = new SpellTextResolver(this, playerLevel);
    }

    /* ─── Lookup ─── */

    @Override
    public Optional<SpellInfo> spell(int id) {
        Optional<SpellInfo> spell = spells.get(id);

        if (spell == null) {
            spell = Optional.ofNullable(build(id));
            spells.put(id, spell);
        }

        return spell;
    }

    @Override
    public List<String> tooltipLine(int id) {
        return tooltip(id).parts();
    }

    @Override
    public Optional<String> cost(int id) {
        return tooltips.firstCost(id);
    }

    /* ─── Names and icons ─── */

    /** Name of a spell, or an empty text. */
    String name(int id) {
        Row row = indexes().names().get(id);

        return row == null ? "" : row.text("Name_lang");
    }

    /** Rank label of a spell ("Rank 3", "Passive"), or an empty text. */
    String rankLabel(int id) {
        Row row = indexes().texts().get(id);

        return row == null ? "" : row.text("NameSubtext_lang");
    }

    /** Whether the spell is passive. */
    boolean passive(int id) {
        Row misc = indexes().misc().get(id);

        return misc != null && (misc.integer("Attributes_0") & SpellTooltips.PASSIVE_ATTRIBUTE) != 0;
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

    /* ─── Texts ─── */

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
     * The tooltip parts and the description in one text, as an ability list shows it: "Instant, 3 min cooldown.
     * Instantly removes all Stun effects." A passive spell has only its description.
     *
     * @param id spell id
     * @return the text, or an empty text for an unknown spell
     */
    String abilityText(int id) {
        List<String> parts = tooltipLine(id);
        String description = description(id);

        return parts.isEmpty() ? description : String.join(", ", parts) + ". " + description;
    }

    /* ─── Tooltip ─── */

    /** What the tooltip of a spell shows above the text. */
    SpellTooltip tooltip(int id) {
        return tooltips.of(id);
    }

    /**
     * The tooltip parts of an active spell in one line: "15 Rage | Melee Range | Instant | 20 sec cooldown | Requires
     * Battle Stance".
     *
     * @param id spell id
     * @return the line, or null when the spell is passive or unknown
     */
    String costLine(int id) {
        List<String> parts = tooltipLine(id);

        return parts.isEmpty() ? null : String.join(SEPARATOR, parts);
    }

    /* ─── Spellbook details ─── */

    /** The magic school of a spell ("Fire", "Holy/Shadow"), or null. */
    String school(int id) {
        Row misc = indexes().misc().get(id);
        int mask = misc == null ? 0 : misc.integer("SchoolMask");
        StringJoiner names = new StringJoiner("/");

        for (int bit = 0; bit < SCHOOLS.size(); bit++) {
            if ((mask & (1 << bit)) != 0) {
                names.add(SCHOOLS.get(bit));
            }
        }

        return names.length() == 0 ? null : names.toString();
    }

    /**
     * How much of the spell power the spell adds, by kind of effect: "85.7% of spell power (direct)", "28.6% of spell
     * power (heal), 9.5% of spell power (per tick)".
     *
     * @param id spell id
     * @return the text, or null when no effect grows with spell power
     */
    String coefficient(int id) {
        Map<String, List<String>> percentsByKind = new LinkedHashMap<>();
        addCoefficients(id, null, true, percentsByKind);
        StringJoiner text = new StringJoiner(", ");
        percentsByKind.forEach((kind, percents) ->
                text.add(String.join(" and ", percents) + " of spell power (" + kind + ")"));

        return text.length() == 0 ? null : text.toString();
    }

    /**
     * Adds the coefficients of the damage and healing effects of a spell, and of the spells its other effects trigger
     * (one level down: Arcane Missiles deals its damage through a triggered spell).
     */
    private void addCoefficients(int id, String forcedKind, boolean followTriggers,
                                 Map<String, List<String>> percentsByKind) {
        for (Row effect : indexes().effects().getOrDefault(id, List.of())) {
            String kind = effectKind(effect);
            double coefficient = effect.decimal("EffectBonusCoefficient");
            int triggered = effect.integer("EffectTriggerSpell");

            if (kind != null && coefficient > 0) {
                percentsByKind.computeIfAbsent(forcedKind == null ? kind : forcedKind, key -> new ArrayList<>())
                        .add(TooltipFormat.number(Math.round(coefficient * 1000) / 10.0) + "%");
            } else if (kind == null && followTriggers && triggered > 0) {
                addCoefficients(triggered, effect.integer("EffectAura") == PERIODIC_TRIGGER_AURA ? PER_TICK : null,
                        false, percentsByKind);
            }
        }
    }

    /** "direct", "heal" or "per tick" for an effect that deals damage or heals, otherwise null. */
    private static String effectKind(Row effect) {
        int aura = effect.integer("EffectAura");

        return switch (effect.integer("Effect")) {
            case DAMAGE_EFFECT, LEECH_EFFECT -> "direct";
            case HEAL_EFFECT -> "heal";
            case AURA_EFFECT -> aura == PERIODIC_DAMAGE_AURA || aura == PERIODIC_HEAL_AURA
                    || aura == PERIODIC_LEECH_AURA ? PER_TICK : null;
            default -> null;
        };
    }

    /**
     * The spell a spellbook entry teaches, when the entry is only a "learn" spell (a hunter's pet abilities are
     * learned through one): its tooltip is the one the player reads. Otherwise the spell itself.
     *
     * @param id spell id
     * @return the spell to show
     */
    int taughtSpell(int id) {
        for (Row effect : indexes().effects().getOrDefault(id, List.of())) {
            int type = effect.integer("Effect");

            if ((type == LEARN_SPELL_EFFECT || type == LEARN_PET_SPELL_EFFECT)
                    && effect.integer("EffectTriggerSpell") > 0) {
                return effect.integer("EffectTriggerSpell");
            }
        }

        return id;
    }

    /** The level a spell is learned at (1 when the client gives none). */
    int learnLevel(int id) {
        Row levels = indexes().levels().get(id);
        int level = levels == null ? 0 : levels.integer("BaseLevel");

        if (level <= 0 && levels != null) {
            level = levels.integer("SpellLevel");
        }

        return Math.max(1, level);
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
            double[] value = baseAndVariance(effect);
            effects.add(new SpellInfo.Effect(effect.integer("EffectIndex"), value[0],
                    effect.decimal("EffectRealPointsPerLevel"), value[1],
                    effect.integer("EffectAuraPeriod"), effect.decimal("EffectAmplitude"),
                    effect.integer("EffectChainTargets"),
                    Math.max(radius(effect.integer("EffectRadiusIndex_0")),
                            radius(effect.integer("EffectRadiusIndex_1"))),
                    effect.decimal("EffectPointsPerResource"), effect.decimal("EffectChainAmplitude"),
                    effect.integer("EffectMiscValue_0")));
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

    /**
     * The base value and variance of an effect. Older clients (Classic Era) give a base and the sides of a die
     * instead: the value goes from base + 1 to base + sides, which is the same as its middle with a spread.
     */
    private static double[] baseAndVariance(Row effect) {
        double base = effect.decimal("EffectBasePointsF");
        int oldBase = effect.integer("EffectBasePoints");
        int dieSides = effect.integer("EffectDieSides");

        if (base != 0 || (oldBase == 0 && dieSides == 0)) {
            return new double[]{base, effect.decimal("Variance")};
        }

        if (dieSides <= 1) {
            return new double[]{oldBase + dieSides, 0};
        }

        double min = oldBase + 1;
        double max = oldBase + dieSides;
        double middle = (min + max) / 2;

        return new double[]{middle, middle == 0 ? 0 : (max - min) / Math.abs(middle)};
    }

    private double radius(int radiusIndex) {
        Row radius = indexes().radii().get(radiusIndex);

        return radius == null ? 0 : radius.decimal("Radius");
    }

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
                    tables.get(ClientTable.SPELL_DURATION).indexBy("ID"),
                    tables.get(ClientTable.SPELL_RADIUS).indexBy("ID"),
                    tables.get(ClientTable.SPELL_RANGE).indexBy("ID"),
                    tables.get(ClientTable.SPELL_X_DESCRIPTION_VARIABLES).indexBy("SpellID"),
                    tables.get(ClientTable.SPELL_DESCRIPTION_VARIABLES).indexBy("ID"),
                    tables.get(ClientTable.SPELL_CLASS_OPTIONS).indexBy("SpellID"));
        }

        return indexes;
    }
}
