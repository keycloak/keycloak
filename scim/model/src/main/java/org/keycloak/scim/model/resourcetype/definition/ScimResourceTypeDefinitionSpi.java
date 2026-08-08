package org.keycloak.scim.model.resourcetype.definition;

import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;

/**
 * SPI for custom SCIM resource type definitions. Definitions are stored as realm components, which gives them
 * realm caching, export/import and validation through {@link ScimResourceTypeDefinitionProviderFactory}.
 */
public class ScimResourceTypeDefinitionSpi implements Spi {

    public static final String NAME = "scimResourceTypeDefinition";

    @Override
    public boolean isInternal() {
        return true;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public Class<? extends Provider> getProviderClass() {
        return ScimResourceTypeDefinitionProvider.class;
    }

    @Override
    public Class<? extends ProviderFactory> getProviderFactoryClass() {
        return ScimResourceTypeDefinitionProviderFactory.class;
    }
}
