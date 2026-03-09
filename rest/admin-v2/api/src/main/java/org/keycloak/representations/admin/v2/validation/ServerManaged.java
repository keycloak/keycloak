package org.keycloak.representations.admin.v2.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * Constraint annotation for fields that are managed by the server.
 * These fields cannot be set by the client during create or update operations.
 *
 * @author Vaclav Muzikar <vmuzikar@ibm.com>
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = {ServerManagedValidator.class})
@Documented
public @interface ServerManaged {
    String message() default "This field is server-managed and cannot be set by the user";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
