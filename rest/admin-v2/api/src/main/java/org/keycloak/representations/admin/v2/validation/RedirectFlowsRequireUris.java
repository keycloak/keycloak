package org.keycloak.representations.admin.v2.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = {})
@Documented
public @interface RedirectFlowsRequireUris {
    String message() default "STANDARD and IMPLICIT flows require at least one redirect URI";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
    // TODO: add "redirectUris" once OASModelFilter supports class-level descriptions for inherited properties
    String[] affectedFieldNames() default {"loginFlows"};
}
