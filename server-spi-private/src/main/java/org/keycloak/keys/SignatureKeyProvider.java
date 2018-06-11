package org.keycloak.keys;

import java.security.Key;

// KEYCLOAK-7560 Refactoring Token Signing and Verifying by Token Signature SPI

public interface SignatureKeyProvider {
    Key getSignKey();
    Key getVerifyKey(String kid);
}
