package org.keycloak.representations.admin.v2.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = {})
@Documented
public @interface ValidCanonicalizationMethod {
    String message() default "must be a valid XML canonicalization method URI (see javax.xml.crypto.dsig.CanonicalizationMethod constants)";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
