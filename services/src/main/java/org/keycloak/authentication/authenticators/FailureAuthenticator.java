package org.keycloak.authentication.authenticators;

import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.Authenticator;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

/**
 * Authenticator that results in failure every time.  Useful for disallowing entire flow types for realms
 */
public class FailureAuthenticator implements Authenticator {

	@Override
	public void authenticate(final AuthenticationFlowContext context) {
		context.failure(AuthenticationFlowError.INVALID_CREDENTIALS);
	}

	@Override
	public void action(final AuthenticationFlowContext context) {
		throw new RuntimeException("Unreachable!");
	}

	@Override
	public boolean requiresUser() {
		return false;
	}

	@Override
	public boolean configuredFor(final KeycloakSession session, final RealmModel realm, final UserModel user) {
		return true;
	}

	@Override
	public void setRequiredActions(final KeycloakSession session, final RealmModel realm, final UserModel user) {
		// no-op
	}

	@Override
	public void close() {
		// no-op
	}
}
