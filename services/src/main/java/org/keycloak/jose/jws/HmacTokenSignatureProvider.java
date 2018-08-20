package org.keycloak.jose.jws;

import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Mac;

import org.keycloak.common.util.Base64Url;
import org.keycloak.component.ComponentModel;
import org.keycloak.crypto.JavaAlgorithm;
import org.keycloak.models.KeycloakSession;

// KEYCLOAK-7560 Refactoring Token Signing and Verifying by Token Signature SPI

public class HmacTokenSignatureProvider extends AbstractTokenSignatureProvider {

    public HmacTokenSignatureProvider(KeycloakSession session, ComponentModel model) {
        super(session, model);
    }

    private Mac getMAC(final String sigAlgName) {
        try {
            return Mac.getInstance(JavaAlgorithm.getJavaAlgorithm(sigAlgName));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Unsupported HMAC algorithm: " + e.getMessage(), e);
        }
    }

    @Override
    public byte[] sign(byte[] data, String sigAlgName, Key key) {
        try {
            Mac mac = getMAC(sigAlgName);
            mac.init(key);
            mac.update(data);
            return mac.doFinal();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean verify(JWSInput jws, Key verifyKey) {
        try {
            byte[] signature = sign(jws.getEncodedSignatureInput().getBytes("UTF-8"), jws.getHeader().getAlgorithm().name(), verifyKey);
            return MessageDigest.isEqual(signature, Base64Url.decode(jws.getEncodedSignature()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
