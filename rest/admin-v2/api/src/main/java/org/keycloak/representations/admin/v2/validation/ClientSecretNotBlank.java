package org.keycloak.representations.admin.v2.validation;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import org.keycloak.representations.admin.v2.OIDCClientRepresentation;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Validation constraint that requires the {@link OIDCClientRepresentation.Auth#getSecret()} is not blank
 * when {@link OIDCClientRepresentation.Auth#getMethod()} is the (JWT) client secret.
 *
 * @see ClientSecretNotBlankValidator#isClientSecret(String)
 */
@Target(TYPE)
@Retention(RUNTIME)
@Constraint(validatedBy = ClientSecretNotBlankValidator.class)
public @interface ClientSecretNotBlank {

    String message() default "Client secret must not be blank";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

}
