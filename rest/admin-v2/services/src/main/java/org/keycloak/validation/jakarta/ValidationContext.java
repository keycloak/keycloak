package org.keycloak.validation.jakarta;

import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.constraints.NotNull;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;

import org.hibernate.validator.constraintvalidation.HibernateConstraintValidatorContext;

/**
 * Context object passed to constraint validators via Hibernate Validator's
 * constraint validator payload mechanism.
 * <p>
 * Record components are annotated with Jakarta Validation so documentation tooling
 * (for example the Admin API v2 OpenAPI {@code ValidationAnnotationScanner}) can surface
 * the same constraints in schema descriptions as for representation types.
 * <p>
 * Custom constraint validators can access this context by unwrapping the
 * {@link jakarta.validation.ConstraintValidatorContext} to
 * {@link org.hibernate.validator.constraintvalidation.HibernateConstraintValidatorContext}
 * and retrieving the payload:
 * 
 * <pre>{@code
 * @Override
 * public boolean isValid(String value, ConstraintValidatorContext context) {
 *     ValidationContext validationContext = ValidationContext.unwrap(context);
 *
 *     KeycloakSession session = validationContext.getSession();
 *     RealmModel realm = validationContext.getRealm();
 *     // ... use session and realm for validation logic
 * }
 * }</pre>
 *
 * @param session the Keycloak session (required)
 * @param realm the realm model being validated against (required)
 */
public record ValidationContext(@NotNull KeycloakSession session, @NotNull RealmModel realm) {

    public ValidationContext {
        if (session == null) {
            throw new IllegalArgumentException("session cannot be null");
        }
        if (realm == null) {
            throw new IllegalArgumentException("realm cannot be null");
        }
    }

    public static ValidationContext unwrap(ConstraintValidatorContext context) {
        HibernateConstraintValidatorContext hibernateContext = context
                .unwrap(HibernateConstraintValidatorContext.class);
        return hibernateContext.getConstraintValidatorPayload(ValidationContext.class);
    }

}
