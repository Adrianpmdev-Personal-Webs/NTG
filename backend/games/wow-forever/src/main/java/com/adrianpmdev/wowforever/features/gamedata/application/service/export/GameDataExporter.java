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
 * Every text is in English, as the client writes it, for a character of the top level with no gear. The client has no
 * notes on what changed for each class, so {@code class_abilities} is empty. When the tables of Classic Era are
 * given, talents and spells are compared with Classic (see ClassicComparison).
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
     * @param build   tables and version of the WoW Forever build
     * @param classic tables and version of the Classic Era build to compare with, or null to skip the comparison
     * @param date    day of the export
     * @return the data file
     * @throws java.io.UncheckedIOException when a table cannot be read
     */
    public ObjectNode export(Build build, Build classic, LocalDate date) {
        GameDataConfig.Client settings = config.client();
        int playerLevel = config.talents().lastPointLevel();
        ClientTables tables = new ClientTables(build.tables());
        ClientSpells spells = new ClientSpells(tables, playerLevel);
        List<TraitTrees.Tree> trees = TraitTrees.read(tables);
        String sourceLine = "WoW Forever client, build " + build.version();
        RacialsExporter racials = new RacialsExporter(tables, spells, PlayableRaces.read(tables));
        ObjectNode talents = new TalentsExporter(tables, spells).export(trees, settings.talentCurrencyId(), sourceLine);
        SpellbookExporter.Sections spellbooks = new SpellbookExporter(tables, spells, config.spellbook().extraTabs(),
                config.talents()).export(talents, build.version());

        if (classic != null) {
            new ClassicComparison(new ClientTables(classic.tables()), playerLevel, config.spellbook().extraTabs())
                    .annotate(talents, spellbooks.spellbooks(), spellbooks.spellDescriptions());
        }

        ObjectNode data = JSON.objectNode();
        data.put("attribution", settings.attribution());
        data.put("generated", date.toString());
        data.put("build", build.version());
        data.put("classicBuild", classic == null ? null : classic.version());
        data.set("talents", talents);
        data.set("spellbooks", spellbooks.spellbooks());
        data.set("spell_desc", spellbooks.spellDescriptions());
        data.set("racials", racials.racials());
        data.set("class_racials", racials.classRacials());
        data.set("class_abilities", JSON.objectNode());
        data.set("legacy", new LegacyExporter(tables, spells, settings).export(trees, sourceLine));

        return data;
    }

    /**
     * One build of a game client.
     *
     * @param version version, for example {@code 1.60.1.70685}
     * @param tables  where its tables are read from
     */
    public record Build(String version, Db2Source tables) {
    }
}
