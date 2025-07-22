package org.keycloak.protocol.oidc;

import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserModel;
import org.keycloak.provider.Provider;

public interface RequestedScopeValidationProvider extends Provider {

	/**
	 * @param session The Keycloak session
	 * @param scopes The requested scopes to be validated
	 * @param client The client that requested the scopes
	 * @param user The user for whom the scopes are requested (can be null)
	 * @return True if the requested scopes are valid given the client that originated the request. False otherwise.
	 */
	boolean isValid(KeycloakSession session, String scopes, ClientModel client, UserModel user);

	@Override
	default void close() {
	}

} 