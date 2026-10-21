package org.keycloak.representations.admin.v2.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

@Target(ElementType.TYPE_USE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = {})
@Documented
public @interface ValidWebOrigin {
    String message() default "must be a valid web origin (scheme://host[:port]), or '+' to derive from redirect URIs, or '*' to allow all";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
