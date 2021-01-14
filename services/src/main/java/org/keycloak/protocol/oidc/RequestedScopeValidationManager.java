package org.keycloak.protocol.oidc;

import java.util.Set;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;

public class RequestedScopeValidationManager {

	private final KeycloakSession session;

	public RequestedScopeValidationManager(KeycloakSession session) {
		this.session = session;
	}

	public boolean isValid(String scopes, ClientModel client) {
		Set<RequestedScopeValidationProvider> providers = session.getAllProviders(RequestedScopeValidationProvider.class);
		for (RequestedScopeValidationProvider provider : providers) {
			if (provider.isValid(scopes, client)) {
				return true;
			}
		}
		return false;
	}
}
