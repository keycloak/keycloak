package org.keycloak.validation.jakarta;

import java.util.Set;
import java.util.function.Function;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;

/**
 * Provider interface for Jakarta Bean Validation with optional Keycloak context support.
 * <p>
 * This interface provides two validation approaches:
 * <ul>
 *   <li><b>Simple validation</b> - Standard Jakarta validation without Keycloak context.
 *       Use for validating objects with only standard constraints (@NotNull, @Size, etc.).</li>
 *   <li><b>Context-aware validation</b> - Validation with Keycloak context (session, realm, existing models).
 *       Use when custom validators need to access Keycloak APIs for cross-entity validation.</li>
 * </ul>
 * <p>
 * Example usage:
 * <pre>{@code
 * // Simple validation
 * validator.validate(client, CreateClient.class);
 *
 * // Context-aware validation (for validators that need session/realm access)
 * var context = ValidationContextData.forCreate(session, realm);
 * validator.validate(client, context, CreateClient.class);
 * }</pre>
 *
 * @see ValidationContextData
 * @see ValidationContextHolder
 */
public interface JakartaValidatorProvider {

    /**
     * Validates an object using the specified validation groups without Keycloak context.
     * <p>
     * Use this method when validating objects with only standard Jakarta constraints
     * that don't need access to Keycloak session, realm, or models.
     *
     * @param object the object to validate
     * @param groups the validation groups to apply (empty means Default group)
     * @param <T> the type of object to validate
     * @throws ConstraintViolationException if validation fails
     */
    <T> void validate(T object, Class<?>... groups) throws ConstraintViolationException;

    /**
     * Validates an object using the specified validation groups with Keycloak context.
     * <p>
     * The context is made available to custom {@link jakarta.validation.ConstraintValidator}
     * implementations via the {@link ValidationContextHolder} CDI bean.
     * <p>
     * Use this method when custom validators need to:
     * <ul>
     *   <li>Check uniqueness against existing entities in the realm</li>
     *   <li>Access realm or client configuration</li>
     *   <li>Perform cross-entity validation</li>
     * </ul>
     *
     * @param object the object to validate
     * @param context the validation context containing session, realm, and optional existing model
     * @param groups the validation groups to apply (empty means Default group)
     * @param <T> the type of object to validate
     * @throws ConstraintViolationException if validation fails
     */
    <T> void validate(T object, ValidationContextData context, Class<?>... groups) throws ConstraintViolationException;

    /**
     * Executes a custom validation function with access to the underlying Validator.
     * <p>
     * This is useful for advanced validation scenarios where you need direct access
     * to the Validator API.
     *
     * @param validation a function that receives the Validator and returns constraint violations
     * @throws ConstraintViolationException if the function returns any violations
     */
    void validate(Function<Validator, Set<ConstraintViolation<?>>> validation) throws ConstraintViolationException;

    /**
     * Returns the underlying Jakarta Validator instance.
     *
     * @return the Validator instance
     */
    Validator getValidator();
}
