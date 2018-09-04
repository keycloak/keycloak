package org.keycloak.crypto;

import org.keycloak.common.VerificationException;
import org.keycloak.models.KeycloakSession;

public class ServerKeyPairSignatureVerifierContext extends KeyPairSignatureVerifierContext {

    public ServerKeyPairSignatureVerifierContext(KeycloakSession session, String kid, String algorithm) throws VerificationException {
        super(getKey(session, kid, algorithm));
    }

    private static KeyWrapper getKey(KeycloakSession session, String kid, String algorithm) throws VerificationException {
        KeyWrapper key = session.keys().getKey(session.getContext().getRealm(), kid, KeyUse.SIG, algorithm);
        if (key == null) {
            throw new VerificationException("Key not found");
        }
        return key;
    }

}
