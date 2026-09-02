package org.keycloak.validation.jakarta;

import java.util.Set;
import java.util.function.Function;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;

/**
 * Provider interface for Jakarta Bean Validation.
 * <p>
 * Implementations can provide a {@link ValidationContext} to custom constraint validators
 * via Hibernate Validator's constraint validator payload mechanism. This allows validators
 * to access Keycloak's runtime context (session, realm) for validation logic.
 *
 * @see ValidationContext
 * @see HibernateValidatorProvider
 */
public interface JakartaValidatorProvider {

    /**
     * Validates the given object using the specified validation groups.
     *
     * @param object the object to validate
     * @param groups the validation groups to apply
     * @param <T> the type of object being validated
     * @throws ConstraintViolationException if validation fails
     */
    <T> void validate(T object, Class<?>... groups) throws ConstraintViolationException;

    /**
     * Validates using a custom validation function that receives the validator.
     *
     * @param validation a function that performs validation and returns constraint violations
     * @throws ConstraintViolationException if validation fails
     */
    void validate(Function<Validator, Set<ConstraintViolation<?>>> validation) throws ConstraintViolationException;

    /**
     * Returns the underlying Jakarta Validator instance.
     *
     * @return the validator
     */
    Validator getValidator();
}
