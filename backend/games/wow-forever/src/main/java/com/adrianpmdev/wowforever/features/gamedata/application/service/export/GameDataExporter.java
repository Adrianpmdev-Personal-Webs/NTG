package com.adrianpmdev.wowforever.features.gamedata.application.service.export;

import com.adrianpmdev.wowforever.features.gamedata.application.service.GameDataConfig;
import com.adrianpmdev.wowforever.features.gamedata.infrastructure.wago.Db2Source;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.time.LocalDate;
import java.util.List;

/**
 * Builds the game data file from the tables of one build of the game client, with the same structure the backend
 * has always read (sections {@code talents}, {@code spellbooks}, {@code spell_desc}, {@code racials},
 * {@code class_racials}, {@code class_abilities} and {@code legacy}), so the rest of the backend does not change.
 * Every text is in English, as the client writes it, for a character of the top level with no gear.
 *
 * @author adrianpmdev
 */
public final class GameDataExporter {

    private static final JsonNodeFactory JSON = JsonNodeFactory.instance;

    private final GameDataConfig config;

    /**
     * Builds the exporter.
     *
     * @param config the settings of the area, with the client section
     */
    public GameDataExporter(GameDataConfig config) {
        this.config = config;
    }

    /**
     * Exports one build.
     *
     * @param source tables of the build
     * @param build  version of the build, for example {@code 1.60.1.70685}
     * @param date   day of the export
     * @return the data file
     * @throws java.io.UncheckedIOException when a table cannot be read
     */
    public ObjectNode export(Db2Source source, String build, LocalDate date) {
        GameDataConfig.Client settings = config.client();
        ClientTables tables = new ClientTables(source);
        ClientSpells spells = new ClientSpells(tables, config.talents().lastPointLevel());
        List<TraitTrees.Tree> trees = TraitTrees.read(tables);
        String sourceLine = "WoW Forever client, build " + build;
        RacialsExporter racials = new RacialsExporter(tables, spells);

        ObjectNode data = JSON.objectNode();
        data.put("attribution", settings.attribution());
        data.put("generated", date.toString());
        data.put("build", build);
        data.set("talents", new TalentsExporter(tables, spells).export(trees, settings.talentCurrencyId(), sourceLine));
        data.set("racials", racials.racials());
        data.set("class_racials", racials.classRacials());
        data.set("legacy", new LegacyExporter(tables, spells, settings).export(trees, sourceLine));

        return data;
    }
}
