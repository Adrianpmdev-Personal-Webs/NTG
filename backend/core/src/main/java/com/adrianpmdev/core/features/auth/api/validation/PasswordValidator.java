package com.adrianpmdev.core.features.auth.api.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

/**
 * Password validator for the {@link ValidPassword} annotation.
 *
 * @author adrianpmdev
 */
public class PasswordValidator implements ConstraintValidator<ValidPassword, String> {

    private static final int MIN_PASSWORD_LENGTH = 8;
    // Supabase hashes passwords with bcrypt, which only uses the first 72 bytes.
    private static final int MAX_PASSWORD_BYTES = 72;

    private static final Pattern HAS_LETTER = Pattern.compile("\\p{L}");
    private static final Pattern HAS_DIGIT = Pattern.compile("\\d");

    /**
     * Initializes the password validator.
     *
     * @param constraintAnnotation constraint annotation
     */
    @Override
    public void initialize(ValidPassword constraintAnnotation) {
    }

    /**
     * Checks whether the password has 8-72 bytes, at least one letter and one digit (any other characters allowed).
     *
     * @param value   value to validate
     * @param context validation context
     * @return true if the password is valid, false otherwise
     */
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return false;
        }

        return value.length() >= MIN_PASSWORD_LENGTH
                && value.getBytes(StandardCharsets.UTF_8).length <= MAX_PASSWORD_BYTES
                && HAS_LETTER.matcher(value).find()
                && HAS_DIGIT.matcher(value).find();
    }
}