package org.keycloak.protocol.oidc.endpoints;

import org.keycloak.provider.EnvironmentDependentProviderFactory;
import org.keycloak.provider.ProviderFactory;

public interface AuthorizationEndpointCheckProviderFactory extends ProviderFactory<AuthorizationEndpointCheckProvider>, EnvironmentDependentProviderFactory {
}
