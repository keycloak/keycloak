package org.keycloak.crypto;

import java.security.PrivateKey;
import java.security.Signature;

public class KeyPairSignatureContext implements SignatureContext {

    private final KeyWrapper key;

    public KeyPairSignatureContext(KeyWrapper key) throws SignatureException {
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
    public byte[] sign(byte[] data) throws SignatureException {
        try {
            Signature signature = Signature.getInstance(JavaAlgorithm.getJavaAlgorithm(key.getAlgorithm()));
            signature.initSign((PrivateKey) key.getSignKey());
            signature.update(data);
            return signature.sign();
        } catch (Exception e) {
            throw new SignatureException("Signing failed", e);
        }
    }

}
