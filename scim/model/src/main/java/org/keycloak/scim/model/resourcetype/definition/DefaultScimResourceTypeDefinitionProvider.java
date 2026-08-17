package org.keycloak.scim.model.resourcetype.definition;

/**
 * Default {@link ScimResourceTypeDefinitionProvider} that wraps a definition parsed from a realm component.
 */
public class DefaultScimResourceTypeDefinitionProvider implements ScimResourceTypeDefinitionProvider {

    private final ScimResourceTypeRepresentation definition;

    public DefaultScimResourceTypeDefinitionProvider(ScimResourceTypeRepresentation definition) {
        this.definition = definition;
    }

    @Override
    public ScimResourceTypeRepresentation getDefinition() {
        return definition;
    }
}
