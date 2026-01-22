package org.keycloak.crypto;

import org.keycloak.common.VerificationException;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.models.ClientModel;

public class NULLClientSignatureVerifierProvider implements ClientSignatureVerifierProvider {

    @Override
    public SignatureVerifierContext verifier(ClientModel client, JWSInput input) throws VerificationException {
        return null;
    }

    @Override
    public String getAlgorithm() {
        return "null";
    }

    @Override
    public boolean isAsymmetricAlgorithm() {
        return true;
    }
}
