package org.keycloak.crypto;

import org.keycloak.models.KeycloakSession;

public class ServerECDSASignatureSignerContext extends ECDSASignatureSignerContext {

    public ServerECDSASignatureSignerContext(KeycloakSession session, String algorithm) throws SignatureException {
        super(ServerAsymmetricSignatureSignerContext.getKey(session, algorithm));
    }

    public ServerECDSASignatureSignerContext(KeyWrapper key) {
        super(key);
    }
}
