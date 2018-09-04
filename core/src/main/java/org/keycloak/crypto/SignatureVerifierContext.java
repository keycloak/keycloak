package org.keycloak.crypto;

import org.keycloak.common.VerificationException;

public interface SignatureVerifierContext {

    String getKid();

    String getAlgorithm();

    boolean verify(byte[] data, byte[] signature) throws VerificationException;

}
