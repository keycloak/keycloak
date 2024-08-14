package org.keycloak.forms.login.freemarker.model;

import org.keycloak.headers.SecurityHeadersProvider;
import org.keycloak.models.KeycloakSession;

import java.util.UUID;

public class NonceBean {
    private final KeycloakSession session;

    private final String scriptNonce;
    private final String styleNonce;

    public NonceBean(KeycloakSession session) {
        this.session = session;

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
