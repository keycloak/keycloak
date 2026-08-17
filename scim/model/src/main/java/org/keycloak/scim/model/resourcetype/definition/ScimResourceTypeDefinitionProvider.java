package org.keycloak.scim.model.resourcetype.definition;

import org.keycloak.provider.Provider;

/**
 * A provider that exposes a single custom SCIM resource type definition stored as a
 * {@link org.keycloak.component.ComponentModel} on a realm.
 * <p>
 * In this iteration the provider is a thin, read-only wrapper around the stored definition. Serving resource
 * instances for custom types (dynamic CRUD) is handled by a dedicated SCIM resource type provider added on top
 * of these definitions.
 */
public interface ScimResourceTypeDefinitionProvider extends Provider {

    /**
     * Returns the definition backing this provider.
     *
     * @return the resource type definition
     */
    ScimResourceTypeRepresentation getDefinition();

    @Override
    default void close() {
    }
}
