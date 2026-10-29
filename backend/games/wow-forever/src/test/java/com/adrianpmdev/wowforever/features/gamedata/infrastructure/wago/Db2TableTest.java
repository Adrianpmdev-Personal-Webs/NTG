package com.adrianpmdev.wowforever.features.gamedata.infrastructure.wago;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Client tables read from the CSV exports of wago.tools, and the newest build of a product.
 *
 * @author adrianpmdev
 */
class Db2TableTest {

    private static final String CSV = "ID,Name_lang,Description_lang,Unused\r\n"
            + "1,\"Heroic Strike\",\"A strong attack, with \"\"rage\"\".\",x\r\n"
            + "2,Charge,\"Line one\nline two\",y\r\n"
            + "3,,,z\n";

    @Test
    void keepsOnlyTheAskedColumnsAndReadsQuotedFields() throws IOException {
        Db2Table table = Db2Table.read("SpellName", new StringReader(CSV), List.of("ID", "Description_lang"));

        assertThat(table.rows()).hasSize(3);
        assertThat(table.indexBy("ID").get(1).text("Description_lang")).isEqualTo("A strong attack, with \"rage\".");
        assertThat(table.indexBy("ID").get(2).text("Description_lang")).isEqualTo("Line one\nline two");
        assertThat(table.indexBy("ID").get(3).text("Description_lang")).isEmpty();
        assertThatThrownBy(() -> table.rows().getFirst().text("Name_lang")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void keepsOnlyTheAskedRows() throws IOException {
        Db2Table table = Db2Table.read("SpellName", new StringReader(CSV), List.of("ID"),
                row -> row.integer("ID") > 1);

        assertThat(table.rows()).extracting(row -> row.integer("ID")).containsExactly(2, 3);
    }

    @Test
    void optionalColumnsMayBeMissing() throws IOException {
        Db2Table table = Db2Table.read("SpellName", new StringReader(CSV), List.of("ID", "?Missing"));

        assertThat(table.rows().getFirst().text("Missing")).isEmpty();
        assertThat(table.rows().getFirst().integer("Missing")).isZero();
    }

    @Test
    void refusesAnExportWithoutAColumn() {
        assertThatThrownBy(() -> Db2Table.read("SpellName", new StringReader(CSV), List.of("ID", "Missing")))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("Missing");
    }

    @Test
    void theNewestBuildIsTheHighestVersion() throws IOException {
        var builds = new ObjectMapper().readTree("""
                {"wowf": [{"version": "1.60.1.70658"}, {"version": "1.60.1.70685"}, {"version": "1.60.1.9999"}],
                 "wow": [{"version": "12.1.5.70575"}]}""");

        assertThat(WagoClient.latestBuild(builds, "wowf")).contains("1.60.1.70685");
        assertThat(WagoClient.latestBuild(builds, "wowx")).isEmpty();
    }
}
