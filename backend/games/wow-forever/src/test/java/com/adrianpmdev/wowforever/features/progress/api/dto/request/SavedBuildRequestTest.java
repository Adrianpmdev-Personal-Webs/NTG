package com.adrianpmdev.wowforever.features.progress.api.dto.request;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The order of the points is optional, because builds kept before it was recorded have none, and when it comes it is
 * the short code of the talent calculator.
 *
 * @author adrianpmdev
 */
class SavedBuildRequestTest {

    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void aBuildWithoutOrderIsAccepted() {
        assertThat(VALIDATOR.validate(request(null))).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"0A1b", "0abcC2-~!", "1_^.579", "0468"})
    void ordersInTheShortCodeAreAccepted(String order) {
        assertThat(VALIDATOR.validate(request(order))).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "0a b", "0a/b", "0a?b", "0añ"})
    void otherOrdersAreRefused(String order) {
        assertThat(VALIDATOR.validate(request(order))).isNotEmpty();
    }

    @Test
    void anOrderLongerThanTheColumnIsRefused() {
        assertThat(VALIDATOR.validate(request("0" + "a".repeat(200)))).isNotEmpty();
    }

    private static SavedBuildRequest request(String order) {
        return new SavedBuildRequest("Raid", 60, "161.5-162.3", order);
    }
}
