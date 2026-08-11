package org.keycloak.admin.api;

/**
 * A field that a resource's list query can sort by.
 * <p>
 * Implemented by resource-specific field enums (e.g. a {@code ClientField}) so that
 * sort parsing in {@link ListOptions} stays resource-agnostic.
 */
public interface SortField {

    String getApiName();

    default String toQueryValue() {
        return getApiName();
    }
}
