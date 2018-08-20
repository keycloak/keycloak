package org.keycloak.crypto;

import org.keycloak.models.KeycloakSession;

public class ES384SignatureProviderFactory implements SignatureProviderFactory {

    public static final String ID = Algorithm.ES384;

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public SignatureProvider create(KeycloakSession session) {
        return new KeyPairSignatureProvider(session, Algorithm.ES384);
    }

}
