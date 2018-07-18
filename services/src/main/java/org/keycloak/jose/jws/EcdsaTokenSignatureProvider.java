package org.keycloak.jose.jws;

import java.security.Key;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;

import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;

// KEYCLOAK-7560 Refactoring Token Signing and Verifying by Token Signature SPI

public class EcdsaTokenSignatureProvider extends AbstractTokenSignatureProvider {

    public EcdsaTokenSignatureProvider(KeycloakSession session, ComponentModel model) {
        super(session, model);
    }

    @Override
    public void close() {}

    @Override
    public byte[] sign(byte[] data, String sigAlgName, Key key) {
        try {
            PrivateKey privateKey = (PrivateKey)key;
            Signature signature = getSignature(sigAlgName);
            signature.initSign(privateKey);
            signature.update(data);
            return signature.sign();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean verify(JWSInput jws, Key verifyKey) {
        try {
            PublicKey publicKey = (PublicKey)verifyKey;
            Signature verifier = getSignature(jws.getHeader().getAlgorithm().name());
            verifier.initVerify(publicKey);
            verifier.update(jws.getEncodedSignatureInput().getBytes("UTF-8"));
            return verifier.verify(jws.getSignature());
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    protected Signature getSignature(String sigAlgName) {
        try {
            return Signature.getInstance(getJavaAlgorithm(sigAlgName));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String getJavaAlgorithm(String sigAlgName) {
        switch (sigAlgName) {
        case "ES256":
            return "SHA256withECDSA";
        case "ES384":
            return "SHA384withECDSA";
        case "ES512":
            return "SHA512withECDSA";
        default:
            throw new IllegalArgumentException("Not an ECDSA Algorithm");
        }
    }
}
