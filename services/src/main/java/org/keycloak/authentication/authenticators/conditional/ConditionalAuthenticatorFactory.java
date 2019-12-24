package org.keycloak.authentication.authenticators.conditional;

import org.keycloak.OAuth2Constants;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.authentication.DisplayTypeAuthenticatorFactory;
import org.keycloak.models.KeycloakSession;

public interface ConditionalAuthenticatorFactory extends AuthenticatorFactory, DisplayTypeAuthenticatorFactory {

    @Override
    default Authenticator create(KeycloakSession session) {
        return getSingleton();
    }

    @Override
    default Authenticator createDisplay(KeycloakSession session, String displayType) {
        if (displayType == null) return getSingleton();
        if (!OAuth2Constants.DISPLAY_CONSOLE.equalsIgnoreCase(displayType)) return null;
        return getSingleton();
    }

    ConditionalAuthenticator getSingleton();

}
