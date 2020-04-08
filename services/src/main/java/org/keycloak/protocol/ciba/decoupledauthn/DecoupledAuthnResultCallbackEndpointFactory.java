package org.keycloak.protocol.ciba.decoupledauthn;

import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oidc.ext.OIDCExtProvider;
import org.keycloak.protocol.oidc.ext.OIDCExtProviderFactory;

public class DecoupledAuthnResultCallbackEndpointFactory implements OIDCExtProviderFactory {

    public static final String PROVIDER_ID = "ciba-decoupled-authn-callback";

    @Override
    public OIDCExtProvider create(KeycloakSession session) {
        return new DelegateDecoupledAuthenticationProvider(session, null);
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

}
