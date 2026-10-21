package com.adrianpmdev.core.features.auth.api.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Validation annotation for strong password policy.
 *
 * @author adrianpmdev
 */
@Documented
@Constraint(validatedBy = PasswordValidator.class)
@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidPassword {

    String DEFAULT_MESSAGE = "Invalid password. It must have at least 8 characters "
            + "(72 bytes maximum), including at least 1 letter and 1 number";

    /**
     * Default message when the password does not match the required pattern.
     *
     * @return error message
     */
    String message() default DEFAULT_MESSAGE;

    /**
     * Bean Validation groups.
     *
     * @return validation groups
     */
    Class<?>[] groups() default {};

    /**
     * Bean Validation payload.
     *
     * @return associated payload
     */
    Class<? extends Payload>[] payload() default {};
}