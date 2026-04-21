package org.keycloak.representations.admin.v2.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * Constraint annotation for validating redirect URIs according to Keycloak's redirect URI rules.
 * <p>
 * Validation rules:
 * <ul>
 *   <li>Wildcards (*) are only allowed at the end of the path</li>
 *   <li>Wildcards must be preceded by a slash (/)</li>
 *   <li>Wildcards cannot be followed by query parameters or fragments</li>
 *   <li>Only one wildcard is allowed</li>
 *   <li>Without a root URL, redirect URIs must be absolute (include scheme)</li>
 *   <li>With a root URL set, relative paths are allowed</li>
 *   <li>Special values: "*" (full wildcard), "+" and "-" (for post-logout) are always valid</li>
 * </ul>
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = {})
@Documented
public @interface ValidRedirectUris {
    String message() default "Invalid redirect URI";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
