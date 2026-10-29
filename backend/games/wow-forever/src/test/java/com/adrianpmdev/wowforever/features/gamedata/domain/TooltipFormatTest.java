package com.adrianpmdev.wowforever.features.gamedata.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Numbers and durations as tooltips write them.
 *
 * @author adrianpmdev
 */
class TooltipFormatTest {

    @Test
    void numbersKeepOnlyTheDecimalsTheyHave() {
        assertThat(TooltipFormat.number(15)).isEqualTo("15");
        assertThat(TooltipFormat.number(0.5)).isEqualTo("0.5");
        assertThat(TooltipFormat.number(1.256)).isEqualTo("1.26");
        assertThat(TooltipFormat.number(2.5, 1)).isEqualTo("2.5");
    }

    @Test
    void numbersInTextsSeparateThousands() {
        assertThat(TooltipFormat.text(1200)).isEqualTo("1,200");
        assertThat(TooltipFormat.text(12000.5)).isEqualTo("12,000.5");
        assertThat(TooltipFormat.whole(1234567.4)).isEqualTo("1,234,567");
        assertThat(TooltipFormat.whole(999)).isEqualTo("999");
    }

    @Test
    void durationsUseTheLargestFittingUnit() {
        assertThat(TooltipFormat.duration(15)).isEqualTo("15 sec");
        assertThat(TooltipFormat.duration(90)).isEqualTo("1.5 min");
        assertThat(TooltipFormat.duration(75)).isEqualTo("75 sec");
        assertThat(TooltipFormat.duration(3600)).isEqualTo("1 hour");
        assertThat(TooltipFormat.duration(7200)).isEqualTo("2 hrs");
        assertThat(TooltipFormat.duration(-1)).isEqualTo("until cancelled");
    }
}
