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
public @interface ConfidentialFlowsRequireAuth {
    String message() default "SERVICE_ACCOUNT and TOKEN_EXCHANGE flows require a confidential client (auth must be specified)";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
    String[] affectedFieldNames() default {"loginFlows"};
}
