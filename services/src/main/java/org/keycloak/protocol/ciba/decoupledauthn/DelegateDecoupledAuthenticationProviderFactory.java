package org.keycloak.protocol.ciba.decoupledauthn;

import org.keycloak.Config.Scope;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

public class DelegateDecoupledAuthenticationProviderFactory implements DecoupledAuthenticationProviderFactory {

    public static final String PROVIDER_ID = "delegate-decoupled-authn";

    private String decoupledAuthenticationRequestUri;

    @Override
    public DecoupledAuthenticationProvider create(KeycloakSession session) {
        return new DelegateDecoupledAuthenticationProvider(session, decoupledAuthenticationRequestUri);
    }

    @Override
    public void init(Scope config) {
        decoupledAuthenticationRequestUri = config.get("decoupledAuthnRequestUri");
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public void close() {
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

}
