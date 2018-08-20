package org.keycloak.crypto;

import org.keycloak.models.KeycloakSession;

public class ServerKeyPairSignatureContext extends KeyPairSignatureContext {

    public ServerKeyPairSignatureContext(KeycloakSession session, String algorithm) throws SignatureException {
        super(getKey(session, algorithm));
    }

    private static KeyWrapper getKey(KeycloakSession session, String algorithm) {
        KeyWrapper key = session.keys().getActiveKey(session.getContext().getRealm(), KeyUse.SIG, algorithm);
        if (key == null) {
            throw new SignatureException("Active key for " + algorithm + " not found");
        }
        return key;
    }

}
