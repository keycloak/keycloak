package org.keycloak.crypto;

import javax.crypto.Mac;

public class SecretSignatureContext implements SignatureContext {

    private final KeyWrapper key;

    public SecretSignatureContext(KeyWrapper key) throws SignatureException {
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
            Mac mac = Mac.getInstance(JavaAlgorithm.getJavaAlgorithm(key.getAlgorithm()));
            mac.init(key.getSecretKey());
            mac.update(data);
            return mac.doFinal();
        } catch (Exception e) {
            throw new SignatureException("Signing failed", e);
        }
    }

}
