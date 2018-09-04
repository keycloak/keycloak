package org.keycloak.crypto;

import org.keycloak.common.VerificationException;
import org.keycloak.models.KeycloakSession;

public class KeyPairSignatureProvider implements SignatureProvider {

    private final KeycloakSession session;
    private final String algorithm;

    public KeyPairSignatureProvider(KeycloakSession session, String algorithm) {
        this.session = session;
        this.algorithm = algorithm;
    }

    @Override
    public SignatureContext signer() throws SignatureException {
        return new ServerKeyPairSignatureContext(session, algorithm);
    }

    @Override
    public SignatureVerifierContext verifier(String kid) throws VerificationException {
        return new ServerKeyPairSignatureVerifierContext(session, kid, algorithm);
    }

}
