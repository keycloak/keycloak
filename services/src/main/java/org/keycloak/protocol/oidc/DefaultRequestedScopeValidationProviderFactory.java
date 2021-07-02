package org.keycloak.protocol.oidc;

import org.keycloak.models.KeycloakSession;

public class DefaultRequestedScopeValidationProviderFactory implements RequestedScopeValidationProviderFactory {
	@Override
	public RequestedScopeValidationProvider create(KeycloakSession session) {
		return new DefaultRequestedScopeValidationProvider();
	}

	@Override
	public String getId() {
		return "default";
	}
}
