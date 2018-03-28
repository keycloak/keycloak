package org.keycloak.authentication;

import org.keycloak.models.KeycloakSession;

/**
 * Implement this interface when declaring your authenticator factory
 * if your provider has support for multiple oidc display query parameter parameter types
 * if the display query parameter is set and your factory implements this interface, this method
 * will be called.
 *
 */
public interface DisplayTypeAuthenticatorFactory {
    /**
     *
     *
     * @param session
     * @param displayType i.e. "console", "wap", "popup" are examples
     * @return null if display type isn't support.
     */
    Authenticator createDisplay(KeycloakSession session, String displayType);
}
