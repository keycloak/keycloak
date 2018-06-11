package org.keycloak.jose.jws;

import java.security.Key;

public interface JWSSignatureProvider {
    // KEYCLOAK-7560 Refactoring Token Signing and Verifying by Token Signature SPI
    byte[] sign(byte[] data, String sigAlgName, Key key);
    boolean verify(JWSInput input, Key key);
}
