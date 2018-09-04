package org.keycloak.crypto;

import org.keycloak.models.KeycloakSession;

public class RS512SignatureProviderFactory implements SignatureProviderFactory {

    public static final String ID = Algorithm.RS512;

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public SignatureProvider create(KeycloakSession session) {
        return new KeyPairSignatureProvider(session, Algorithm.RS512);
    }

}
