package org.keycloak.jose.jws;

import java.security.Key;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;

import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;

// KEYCLOAK-7560 Refactoring Token Signing and Verifying by Token Signature SPI

public class RsassaTokenSignatureProvider extends AbstractTokenSignatureProvider {

    public RsassaTokenSignatureProvider(KeycloakSession session, ComponentModel model) {
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
}
