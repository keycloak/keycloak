package org.keycloak.theme.beans;

import org.keycloak.headers.SecurityHeadersProvider;
import org.keycloak.models.KeycloakSession;

import java.util.UUID;

/**
 * @author James Shuriff
 */
public class NonceBean {
    private final String scriptNonce;
    private final String styleNonce;

    public NonceBean(KeycloakSession session) {
        scriptNonce = UUID.randomUUID().toString();
        session.getProvider(SecurityHeadersProvider.class).options().addScriptSrc("'nonce-" + scriptNonce + "'");

        styleNonce = UUID.randomUUID().toString();
        session.getProvider(SecurityHeadersProvider.class).options().addStyleSrc("'nonce-" + styleNonce + "'");
    }

    public String getScript() {
        return scriptNonce;
    }

    public String getStyle() {
        return styleNonce;
    }
}
