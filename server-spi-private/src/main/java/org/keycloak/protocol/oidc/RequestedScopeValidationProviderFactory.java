package org.keycloak.protocol.oidc;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderFactory;

public interface RequestedScopeValidationProviderFactory extends ProviderFactory<RequestedScopeValidationProvider> {

	@Override
	default void init(Config.Scope config) {
	}

	@Override
	default void postInit(KeycloakSessionFactory factory) {
	}

	@Override
	default void close() {
	}

}
