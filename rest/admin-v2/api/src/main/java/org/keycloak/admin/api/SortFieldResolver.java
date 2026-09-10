package org.keycloak.admin.api;

import java.util.Optional;

/**
 * Resolves a raw API field name (as used in the {@code sort} query parameter) to a
 * resource-specific {@link SortField}. Allows {@link ListOptions} to parse sort
 * expressions without depending on any concrete field enum.
 */
@FunctionalInterface
public interface SortFieldResolver<F extends SortField> {

    Optional<F> resolve(String apiName);
}
