package org.keycloak.crypto;

import org.keycloak.common.VerificationException;

public interface SignatureVerifierContextAccessor {

    SignatureVerifierContext create(String algorithmName, String kid) throws VerificationException;
}
