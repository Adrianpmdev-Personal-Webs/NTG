package com.adrianpmdev.wowforever.features.gamedata.application.service.export;

import com.adrianpmdev.wowforever.features.gamedata.application.service.GameDataConfig;
import com.adrianpmdev.wowforever.features.gamedata.application.service.GameDataService;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The fingerprint of the export: the same for the same code and settings (a restart never reads the build again), and
 * another one when a setting the export reads changes, but not when only a text of the screens does.
 *
 * @author adrianpmdev
 */
class GameDataExporterTest {

    private final GameDataConfig config = new GameDataService().getConfig();

    @Test
    void theFingerprintIsStable() {
        String fingerprint = GameDataExporter.fingerprint(config);

        assertThat(fingerprint).matches("[0-9a-f]{16}");
        assertThat(GameDataExporter.fingerprint(new GameDataService().getConfig())).isEqualTo(fingerprint);
    }

    @Test
    void aSettingTheExportReadsChangesItButATextDoesNot() {
        GameDataConfig.Profession first = config.professions().list().getFirst();
        GameDataConfig.Profession otherSpell = new GameDataConfig.Profession(first.skillLine(), first.group(),
                new GameDataConfig.Camp(first.camp().item(), 1, first.camp().effect()), first.description(),
                first.pairsWith());
        GameDataConfig.Profession otherTexts = new GameDataConfig.Profession(first.skillLine(), first.group(),
                new GameDataConfig.Camp(first.camp().item(), first.camp().spell(), "$s1 something else"),
                "Another description", "Goes with something else.");

        assertThat(GameDataExporter.fingerprint(withFirstProfession(otherSpell)))
                .isNotEqualTo(GameDataExporter.fingerprint(config));
        assertThat(GameDataExporter.fingerprint(withFirstProfession(otherTexts)))
                .isEqualTo(GameDataExporter.fingerprint(config));
    }

    private GameDataConfig withFirstProfession(GameDataConfig.Profession profession) {
        List<GameDataConfig.Profession> list = new ArrayList<>(config.professions().list());
        list.set(0, profession);

        return new GameDataConfig(config.iconBaseUrl(), config.raceIconBaseUrl(), config.fallbackIcon(),
                config.factions(), config.features(), config.news(), config.bluePosts(), config.talents(),
                config.spellbook(), config.legacy(), new GameDataConfig.Professions(config.professions().groups(),
                list), config.client());
    }
}
