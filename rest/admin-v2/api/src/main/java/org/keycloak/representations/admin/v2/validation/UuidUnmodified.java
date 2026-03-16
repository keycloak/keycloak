package org.keycloak.representations.admin.v2.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * Constraint annotation for validating that UUID was not modified by the user.
 *
 * @author Vaclav Muzikar <vmuzikar@ibm.com>
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = {UuidUnmodifiedValidator.class})
@Documented
public @interface UuidUnmodified {
    String message() default "UUID is server-managed and must not be user-specified";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
    Class<? extends UuidProvider> aliasProvider();
}
