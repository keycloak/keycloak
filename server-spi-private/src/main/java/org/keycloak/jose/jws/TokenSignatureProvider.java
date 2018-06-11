package org.keycloak.jose.jws;

import java.security.Key;

import org.keycloak.provider.Provider;

// KEYCLOAK-7560 Refactoring Token Signing and Verifying by Token Signature SPI

public interface TokenSignatureProvider extends Provider {
    byte[] sign(byte[] data, String sigAlgName, Key key);
    boolean verify(JWSInput jws, Key verifyKey);
}
