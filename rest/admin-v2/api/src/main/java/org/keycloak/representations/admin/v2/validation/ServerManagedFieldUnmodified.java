package org.keycloak.representations.admin.v2.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * Constraint annotation for validating that a server-managed field was not modified by the client.
 *
 * @author Vaclav Muzikar <vmuzikar@ibm.com>
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Repeatable(ServerManagedFieldUnmodified.List.class)
@Constraint(validatedBy = {})
public @interface ServerManagedFieldUnmodified {
    String message() default "Field is server-managed and must not be user-specified";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
    String field();
    String[] affectedFieldNames() default {};
    boolean rejectExistingValueOnCreate() default false;

    @Target({ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    @interface List {
        ServerManagedFieldUnmodified[] value();
    }
}
