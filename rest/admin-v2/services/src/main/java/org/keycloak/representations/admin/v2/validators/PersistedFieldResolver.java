package org.keycloak.representations.admin.v2.validators;

import org.keycloak.validation.jakarta.ValidationContext;

/**
 * Resolves provided and persisted values for server-managed fields during validation.
 *
 * @author Vaclav Muzikar <vmuzikar@ibm.com>
 */
public interface PersistedFieldResolver<T> {

    boolean supports(Class<? extends T> representationType);

    Object getValue(T representation, String fieldName);

    T getPersisted(ValidationContext context, T representation);

}
