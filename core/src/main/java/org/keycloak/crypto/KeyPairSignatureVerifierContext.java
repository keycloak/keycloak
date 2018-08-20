package org.keycloak.crypto;

import org.keycloak.common.VerificationException;

import java.security.PublicKey;
import java.security.Signature;

public class KeyPairSignatureVerifierContext implements SignatureVerifierContext {

    private final KeyWrapper key;

    public KeyPairSignatureVerifierContext(KeyWrapper key) {
        this.key = key;
    }

    @Override
    public String getKid() {
        return key.getKid();
    }

    @Override
    public String getAlgorithm() {
        return key.getAlgorithm();
    }

    @Override
    public boolean verify(byte[] data, byte[] signature) throws VerificationException {
        try {
            Signature verifier = Signature.getInstance(JavaAlgorithm.getJavaAlgorithm(key.getAlgorithm()));
            verifier.initVerify((PublicKey) key.getVerifyKey());
            verifier.update(data);
            return verifier.verify(signature);
        } catch (Exception e) {
            throw new VerificationException("Signing failed", e);
        }
    }

}
