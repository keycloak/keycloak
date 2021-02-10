package org.keycloak.crypto;

import org.keycloak.common.VerificationException;
import org.keycloak.jose.jwe.JWEException;

import java.security.PrivateKey;

public interface DecryptionKEKAccessor {

    PrivateKey getKEK(String kid, String algorithm) throws JWEException;
}
