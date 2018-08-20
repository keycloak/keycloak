package org.keycloak.crypto;

import org.keycloak.models.KeycloakSession;

public class HS256SignatureProviderFactory implements SignatureProviderFactory {

    public static final String ID = Algorithm.HS256;

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public SignatureProvider create(KeycloakSession session) {
        return new SecretSignatureProvider(session, Algorithm.HS256);
    }

}
