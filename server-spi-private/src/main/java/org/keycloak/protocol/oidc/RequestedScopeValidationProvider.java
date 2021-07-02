package org.keycloak.protocol.oidc;

import org.keycloak.models.ClientModel;
import org.keycloak.provider.Provider;

public interface RequestedScopeValidationProvider extends Provider {

	/**
	 * @param scopes The requested scopes to be validated
	 * @param client The client that requested the scopes
	 * @return True if the requested scopes are valid given the client that originated the request. False otherwise.
	 */
	boolean isValid(String scopes, ClientModel client);

	@Override
	default void close() {
	}

}
