package org.keycloak.scim.model.resourcetype.definition;

import org.keycloak.Config;
import org.keycloak.common.Profile;
import org.keycloak.component.ComponentFactory;
import org.keycloak.provider.EnvironmentDependentProviderFactory;

/**
 * Factory for {@link ScimResourceTypeDefinitionProvider}. Extends {@link ComponentFactory} so that each custom
 * resource type definition is stored, cached and validated as a realm component.
 */
public interface ScimResourceTypeDefinitionProviderFactory
        extends ComponentFactory<ScimResourceTypeDefinitionProvider, ScimResourceTypeDefinitionProvider>,
        EnvironmentDependentProviderFactory {

    @Override
    default boolean isSupported(Config.Scope config) {
        return Profile.isFeatureEnabled(Profile.Feature.SCIM_API);
    }

    @Override
    default String getHelpText() {
        return "Defines a custom SCIM resource type for a realm.";
    }
}
