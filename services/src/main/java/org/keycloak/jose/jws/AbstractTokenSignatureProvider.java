package org.keycloak.jose.jws;

import java.security.Key;
import java.security.Signature;

import org.jboss.logging.Logger;
import org.keycloak.component.ComponentModel;
import org.keycloak.crypto.JavaAlgorithm;
import org.keycloak.models.KeycloakSession;
import org.keycloak.jose.jws.JWSSignatureProvider;

// KEYCLOAK-7560 Refactoring Token Signing and Verifying by Token Signature SPI

public abstract class AbstractTokenSignatureProvider implements TokenSignatureProvider, JWSSignatureProvider {
    protected static final Logger logger = Logger.getLogger(AbstractTokenSignatureProvider.class);

    public AbstractTokenSignatureProvider(KeycloakSession session, ComponentModel model) {}

    @Override
    public void close() {}

    @Override
    public abstract byte[] sign(byte[] data, String sigAlgName, Key key);

    @Override
    public abstract boolean verify(JWSInput jws, Key verifyKey);

    protected Signature getSignature(String sigAlgName) {
        try {
            return Signature.getInstance(JavaAlgorithm.getJavaAlgorithm(sigAlgName));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
