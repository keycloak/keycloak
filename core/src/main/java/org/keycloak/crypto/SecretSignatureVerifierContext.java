package org.keycloak.crypto;

import org.keycloak.common.VerificationException;

import javax.crypto.Mac;
import java.security.MessageDigest;

public class SecretSignatureVerifierContext implements SignatureVerifierContext {

    private final KeyWrapper key;

    public SecretSignatureVerifierContext(KeyWrapper key) {
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
            Mac mac = Mac.getInstance(JavaAlgorithm.getJavaAlgorithm(key.getAlgorithm()));
            mac.init(key.getSecretKey());
            mac.update(data);
            byte[] verificationSignature = mac.doFinal();
            return MessageDigest.isEqual(verificationSignature, signature);
        } catch (Exception e) {
            throw new VerificationException("Signing failed", e);
        }
    }

}
