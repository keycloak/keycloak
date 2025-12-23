package org.keycloak.crypto;

import org.keycloak.common.VerificationException;

public class NULLSignatureProvider implements SignatureProvider {
    @Override
    public SignatureSignerContext signer() throws SignatureException {
        return null;
    }

    @Override
    public SignatureSignerContext signer(KeyWrapper key) throws SignatureException {
        return null;
    }

    @Override
    public SignatureVerifierContext verifier(String kid) throws VerificationException {
        return null;
    }

    @Override
    public SignatureVerifierContext verifier(KeyWrapper key) throws VerificationException {
        return null;
    }

    @Override
    public boolean isAsymmetricAlgorithm() {
        return true;
    }
}
