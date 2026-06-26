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
            case KeyType.AKP:
                 return new AsymmetricSignatureSignerContext(keyWrapper) {
                     @Override
                     public byte[] sign(byte[] data) throws org.keycloak.crypto.SignatureException {
                         try {
                             java.security.Signature signature;
                             try {
                                 signature = java.security.Signature.getInstance(org.keycloak.crypto.JavaAlgorithm.getJavaAlgorithm(this.key.getAlgorithmOrDefault(), this.key.getCurve()));
                             } catch (java.security.NoSuchAlgorithmException e) {
                                 signature = org.keycloak.common.crypto.CryptoIntegration.getProvider().getSignature(this.key.getAlgorithmOrDefault());
                             }
                             signature.initSign((java.security.PrivateKey) this.key.getPrivateKey());
                             signature.update(data);
                             return signature.sign();
                         } catch (Exception e) {
                             throw new org.keycloak.crypto.SignatureException("Signing failed", e);
                         }
                     }
                 };
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
            case KeyType.AKP:
                return new AsymmetricSignatureVerifierContext(keyWrapper);
            default:
                throw new IllegalArgumentException("No signer provider for key algorithm type " + keyWrapper.getType());
        }
    }

    private KeyWrapperUtil() {
    }
}
