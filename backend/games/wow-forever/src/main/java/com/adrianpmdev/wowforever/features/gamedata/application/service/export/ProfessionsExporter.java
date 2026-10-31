package com.adrianpmdev.wowforever.features.gamedata.application.service.export;

import com.adrianpmdev.wowforever.features.gamedata.application.service.GameDataConfig;
import com.adrianpmdev.wowforever.features.gamedata.infrastructure.wago.Db2Table.Row;
import com.adrianpmdev.wowforever.features.gamedata.domain.SpellInfo;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Writes the {@code professions} section: for each profession of config.json, by its skill line id, its name and icon
 * and its first camp feature (the camp item with its icon, the skill it needs, the values of the buff it gives and the
 * description of the item), as the client writes them. The values are the effects of the buff spell ({@code $s1},
 * {@code $s2}...), resolved like any game text for a character of the top level; the short text that uses them is in
 * config.json and is filled in when the file is served, so it can change without reading the build again. The
 * description is the one of the spell the item casts, without the notes every camp feature shares (it needs a campfire
 * nearby, the shared cooldown).
 *
 * @author adrianpmdev
 */
final class ProfessionsExporter {

    private static final JsonNodeFactory JSON = JsonNodeFactory.instance;
    private static final String SHARED_NOTES = "Requires a ";

    private final ClientTables tables;
    private final ClientSpells spells;

    ProfessionsExporter(ClientTables tables, ClientSpells spells) {
        this.tables = tables;
        this.spells = spells;
    }

    /**
     * The {@code professions} section.
     *
     * @param professions the professions of config.json
     * @return professions by skill line id; one the client does not have is left out, and so is a missing camp item
     */
    ObjectNode export(List<GameDataConfig.Profession> professions) {
        Map<Integer, Row> skillLines = tables.get(ClientTable.SKILL_LINE).indexBy("ID");
        Map<Integer, Row> itemNames = tables.get(ClientTable.ITEM_NAMES).indexBy("ID");
        Map<Integer, Row> items = tables.get(ClientTable.ITEMS).indexBy("ID");
        Map<Integer, Integer> spellOfEffect = tables.get(ClientTable.ITEM_EFFECT).rows().stream()
                .collect(Collectors.toMap(row -> row.integer("ID"), row -> row.integer("SpellID"), (a, b) -> a));
        Map<Integer, List<Row>> effectsByItem = tables.get(ClientTable.ITEM_X_ITEM_EFFECT).groupBy("ItemID");
        ObjectNode section = JSON.objectNode();

        for (GameDataConfig.Profession profession : professions) {
            Row skillLine = skillLines.get(profession.skillLine());

            if (skillLine == null) {
                continue;
            }

            ObjectNode written = section.putObject(String.valueOf(profession.skillLine()));
            written.put("name", skillLine.text("DisplayName_lang"));
            written.put("icon", spells.iconFile(skillLine.integer("SpellIconFileID")));

            GameDataConfig.Camp camp = profession.camp();
            Row name = camp == null ? null : itemNames.get(camp.item());

            if (name != null) {
                int itemSpell = effectsByItem.getOrDefault(camp.item(), List.of()).stream()
                        .map(link -> spellOfEffect.getOrDefault(link.integer("ItemEffectID"), 0))
                        .filter(id -> id > 0)
                        .findFirst()
                        .orElse(0);
                Row item = items.get(camp.item());
                int valuesSpell = camp.spell() == null ? itemSpell : camp.spell();

                ObjectNode feature = written.putObject("camp")
                        .put("item", camp.item())
                        .put("name", name.text("Display_lang"))
                        .put("icon", item == null ? "" : spells.iconFile(item.integer("IconFileDataID")))
                        .put("skill", name.integer("RequiredSkillRank"))
                        .put("description", withoutSharedNotes(spells.description(itemSpell)));
                feature.set("values", values(valuesSpell));
            }
        }

        return section;
    }

    /** The value of every effect of a spell ({@code $s1}, {@code $s2}...) as the game writes it. */
    private ArrayNode values(int spellId) {
        ArrayNode values = JSON.arrayNode();
        int effects = spells.spell(spellId).map(SpellInfo::effects).map(List::size).orElse(0);

        for (int index = 1; index <= effects; index++) {
            values.add(spells.description(spellId, Map.of(), "$s" + index));
        }

        return values;
    }

    private static String withoutSharedNotes(String description) {
        int notes = description.indexOf(SHARED_NOTES);

        return (notes <= 0 ? description : description.substring(0, notes)).strip();
    }
}
