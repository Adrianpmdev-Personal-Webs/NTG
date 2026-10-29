package com.adrianpmdev.wowforever.features.gamedata.application.service.export;

import com.adrianpmdev.wowforever.features.gamedata.infrastructure.wago.Db2Table;

import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * The tables of the game client the export reads, with the only columns and rows it uses. Spell tables keep only the
 * rows of the normal difficulty (0); the file list keeps only icons. Columns marked {@code ?} exist in only one of the
 * two clients read (WoW Forever and, for the comparison, Classic Era).
 *
 * @author adrianpmdev
 */
enum ClientTable {

    SPELL_NAME("SpellName", "ID", "Name_lang"),
    SPELL("Spell", "ID", "NameSubtext_lang", "Description_lang", "AuraDescription_lang"),
    SPELL_EFFECT(true, "SpellEffect", "SpellID", "EffectIndex", "Effect", "EffectAura", "EffectBasePointsF",
            "EffectRealPointsPerLevel", "Variance", "EffectAuraPeriod", "EffectAmplitude", "EffectChainTargets",
            "EffectRadiusIndex_0", "EffectRadiusIndex_1", "EffectBonusCoefficient", "EffectPointsPerResource",
            "EffectChainAmplitude", "EffectTriggerSpell", "EffectMiscValue_0", "?EffectBasePoints", "?EffectDieSides"),
    SPELL_MISC(true, "SpellMisc", "SpellID", "Attributes_0", "Attributes_1", "Attributes_2", "DurationIndex",
            "RangeIndex", "CastingTimeIndex", "SchoolMask", "SpellIconFileDataID"),
    SPELL_LEVELS(true, "SpellLevels", "SpellID", "BaseLevel", "SpellLevel", "MaxLevel"),
    SPELL_AURA_OPTIONS(true, "SpellAuraOptions", "SpellID", "CumulativeAura", "ProcChance", "ProcCharges",
            "ProcCategoryRecovery"),
    SPELL_TARGET_RESTRICTIONS(true, "SpellTargetRestrictions", "SpellID", "MaxTargets"),
    SPELL_COOLDOWNS(true, "SpellCooldowns", "SpellID", "RecoveryTime", "CategoryRecoveryTime"),
    SPELL_POWER("SpellPower", "SpellID", "OrderIndex", "ManaCost", "ManaPerSecond", "PowerCostPct", "PowerType"),
    SPELL_DURATION("SpellDuration", "ID", "Duration"),
    SPELL_RADIUS("SpellRadius", "ID", "Radius"),
    SPELL_RANGE("SpellRange", "ID", "Flags", "RangeMin_0", "RangeMin_1", "RangeMax_0", "RangeMax_1"),
    SPELL_CAST_TIMES("SpellCastTimes", "ID", "Base"),
    SPELL_X_DESCRIPTION_VARIABLES("SpellXDescriptionVariables", "SpellID", "SpellDescriptionVariablesID"),
    SPELL_DESCRIPTION_VARIABLES("SpellDescriptionVariables", "ID", "Variables"),
    SPELL_SHAPESHIFT("SpellShapeshift", "SpellID", "ShapeshiftMask_0"),
    SPELL_SHAPESHIFT_FORM("SpellShapeshiftForm", "ID", "Name_lang"),
    SPELL_EQUIPPED_ITEMS("SpellEquippedItems", "SpellID", "EquippedItemClass", "EquippedItemSubclass"),
    SPELL_CLASS_OPTIONS("SpellClassOptions", "SpellID", "SpellClassSet"),
    SPELL_REAGENTS("SpellReagents", "SpellID", "Reagent_0", "Reagent_1", "Reagent_2", "Reagent_3",
            "Reagent_4", "Reagent_5", "Reagent_6", "Reagent_7", "ReagentCount_0", "ReagentCount_1",
            "ReagentCount_2", "ReagentCount_3", "ReagentCount_4", "ReagentCount_5", "ReagentCount_6", "ReagentCount_7"),
    SPELL_TOTEMS("SpellTotems", "SpellID", "RequiredTotemCategoryID_0", "RequiredTotemCategoryID_1", "Totem_0",
            "Totem_1"),
    TOTEM_CATEGORY("TotemCategory", "ID", "Name_lang"),
    ITEM_NAMES("ItemSparse", "ID", "Display_lang"),
    ITEM_SUB_CLASS("ItemSubClass", "ClassID", "SubClassID", "DisplayName_lang", "VerboseName_lang"),
    POWER_TYPE("PowerType", "PowerTypeEnum", "NameGlobalStringTag", "DisplayModifier", "MaxBasePower"),
    ICON_FILES("ManifestInterfaceData", "ID", "FilePath", "FileName") {
        @Override
        boolean keeps(Db2Table.Row row) {
            return row.text("FilePath").toLowerCase(Locale.ROOT).replace('/', '\\').startsWith("interface\\icons");
        }
    },
    TRAIT_TREE_X_TRAIT_CURRENCY("TraitTreeXTraitCurrency", "TraitTreeID", "TraitCurrencyID"),
    TRAIT_CURRENCY("TraitCurrency", "ID", "SourcedMax"),
    TRAIT_NODE("TraitNode", "ID", "TraitTreeID", "PosX", "PosY", "Flags"),
    TRAIT_NODE_X_TRAIT_NODE_ENTRY("TraitNodeXTraitNodeEntry", "TraitNodeID", "TraitNodeEntryID", "_Index"),
    TRAIT_NODE_ENTRY("TraitNodeEntry", "ID", "TraitDefinitionID", "MaxRanks"),
    TRAIT_DEFINITION("TraitDefinition", "ID", "SpellID", "OverrideName_lang", "OverrideDescription_lang",
            "OverrideIcon"),
    TRAIT_DEFINITION_EFFECT_POINTS("TraitDefinitionEffectPoints", "TraitDefinitionID", "EffectIndex",
            "OperationType", "CurveID"),
    CURVE_POINT("CurvePoint", "CurveID", "Pos_0", "Pos_1", "OrderIndex"),
    TRAIT_EDGE("TraitEdge", "LeftTraitNodeID", "RightTraitNodeID"),
    TRAIT_NODE_GROUP_X_TRAIT_NODE("TraitNodeGroupXTraitNode", "TraitNodeGroupID", "TraitNodeID"),
    TRAIT_NODE_GROUP_X_TRAIT_COND("TraitNodeGroupXTraitCond", "TraitNodeGroupID", "TraitCondID"),
    TRAIT_COND("TraitCond", "ID", "SpentAmountRequired", "TraitCurrencyID"),
    TALENT_TAB("TalentTab", "ID", "Name_lang", "ClassMask", "OrderIndex", "SpellIconID"),
    CHR_CLASSES("ChrClasses", "ID", "Name_lang", "IconFileDataID", "SpellClassSet"),
    CHR_RACES("ChrRaces", "ID", "Name_lang", "ClientFileString", "Alliance", "PlayableRaceBit"),
    CHAR_BASE_INFO("CharBaseInfo", "RaceID", "ClassID"),
    SKILL_LINE("SkillLine", "ID", "DisplayName_lang", "CategoryID"),
    SKILL_LINE_ABILITY("SkillLineAbility", "SkillLine", "Spell", "?RaceMasks_0", "?RaceMasks_1", "ClassMask",
            "AcquireMethod"),
    CLASSIC_TALENT("Talent", "TabID", "TierID", "ColumnIndex", "SpellRank_0", "SpellRank_1", "SpellRank_2",
            "SpellRank_3", "SpellRank_4", "SpellRank_5", "SpellRank_6", "SpellRank_7", "SpellRank_8"),
    ACHIEVEMENT("Achievement", "ID", "Title_lang", "Description_lang", "Category", "Flags", "Ui_order"),
    ACHIEVEMENT_CATEGORY("Achievement_Category", "ID", "Name_lang", "Parent", "Ui_order");

    private static final String DIFFICULTY = "DifficultyID";

    private final String tableName;
    private final List<String> columns;
    private final boolean normalDifficultyOnly;

    ClientTable(String tableName, String... columns) {
        this(false, tableName, columns);
    }

    ClientTable(boolean normalDifficultyOnly, String tableName, String... columns) {
        this.tableName = tableName;
        this.normalDifficultyOnly = normalDifficultyOnly;
        this.columns = normalDifficultyOnly
                ? Stream.concat(Stream.of(columns), Stream.of(DIFFICULTY)).toList()
                : List.of(columns);
    }

    /** Name of the table in the client and on wago.tools. */
    String tableName() {
        return tableName;
    }

    /** Columns read. */
    List<String> columns() {
        return columns;
    }

    /** Rows kept. */
    Predicate<Db2Table.Row> rows() {
        return this::keeps;
    }

    boolean keeps(Db2Table.Row row) {
        return !normalDifficultyOnly || row.integer(DIFFICULTY) == 0;
    }
}
