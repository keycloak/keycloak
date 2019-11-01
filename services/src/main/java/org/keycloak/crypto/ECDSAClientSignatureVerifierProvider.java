package org.keycloak.crypto;

import org.keycloak.common.VerificationException;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;

public class ECDSAClientSignatureVerifierProvider implements ClientSignatureVerifierProvider {
    private final KeycloakSession session;
    private final String algorithm;

    public ECDSAClientSignatureVerifierProvider(KeycloakSession session, String algorithm) {
        this.session = session;
        this.algorithm = algorithm;
    }

    @Override
    public SignatureVerifierContext verifier(ClientModel client, JWSInput input) throws VerificationException {
        return new ClientECDSASignatureVerifierContext(session, client, input);
    }
}
