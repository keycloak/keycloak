package org.keycloak.crypto;

import org.keycloak.common.VerificationException;

public class ECDSASignatureVerifierContext extends AsymmetricSignatureVerifierContext{
    public ECDSASignatureVerifierContext(KeyWrapper key) {
        super(key);
    }

    @Override
    public boolean verify(byte[] data, byte[] signature) throws VerificationException {
        try {
            int expectedSize = ECDSAAlgorithm.getSignatureLength(getAlgorithm());
            byte[] derSignature = ECDSAAlgorithm.concatenatedRSToASN1DER(signature, expectedSize);
            return super.verify(data, derSignature);
        } catch (Exception e) {
            throw new VerificationException("Verification failed", e);
        }
    }
}
