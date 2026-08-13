package org.keycloak.util;

import org.keycloak.crypto.AsymmetricSignatureSignerContext;
import org.keycloak.crypto.AsymmetricSignatureVerifierContext;
import org.keycloak.crypto.ECDSASignatureSignerContext;
import org.keycloak.crypto.ECDSASignatureVerifierContext;
import org.keycloak.crypto.KeyType;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.crypto.SignatureSignerContext;
import org.keycloak.crypto.SignatureVerifierContext;

public class KeyWrapperUtil {

    public static SignatureSignerContext createSignatureSignerContext(KeyWrapper keyWrapper) {
        switch (keyWrapper.getType()) {
            case KeyType.EC:
                return new ECDSASignatureSignerContext(keyWrapper);
            case KeyType.RSA:
            case KeyType.OKP:
                return new AsymmetricSignatureSignerContext(keyWrapper);
            default:
                throw new IllegalArgumentException("No signer provider for key algorithm type " + keyWrapper.getType());
        }
    }

    public static SignatureVerifierContext createSignatureVerifierContext(KeyWrapper keyWrapper) {
        switch (keyWrapper.getType()) {
            case KeyType.EC:
                return new ECDSASignatureVerifierContext(keyWrapper);
            case KeyType.RSA:
            case KeyType.OKP:
                return new AsymmetricSignatureVerifierContext(keyWrapper);
            default:
                throw new IllegalArgumentException("No signer provider for key algorithm type " + keyWrapper.getType());
        }
    }

    private KeyWrapperUtil() {
    }
}
