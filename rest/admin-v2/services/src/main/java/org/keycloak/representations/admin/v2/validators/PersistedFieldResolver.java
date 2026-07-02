package org.keycloak.representations.admin.v2.validators;

import org.keycloak.validation.jakarta.ValidationContext;

/**
 * Resolves provided and persisted values for server-managed fields during validation.
 *
 * @author Vaclav Muzikar <vmuzikar@ibm.com>
 */
public interface PersistedFieldResolver {

    boolean supports(Class<?> representationType);

    String getProvidedValue(Object representation, String fieldName);

    String getPersistedValue(ValidationContext context, Object representation, String fieldName);

    boolean valueExists(ValidationContext context, String fieldName, String value);
}
