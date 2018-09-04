package org.keycloak.crypto;

import org.keycloak.common.VerificationException;
import org.keycloak.provider.Provider;

public interface SignatureProvider extends Provider {

    SignatureContext signer() throws SignatureException;

    SignatureVerifierContext verifier(String kid) throws VerificationException;

    @Override
    default void close() {
    }

}
