package org.keycloak.representations.admin.v2.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = {})
public @interface ProtocolUnmodified {
    String message() default "protocol cannot be changed for an existing client";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
    String[] affectedFieldNames() default { "protocol" };
}
