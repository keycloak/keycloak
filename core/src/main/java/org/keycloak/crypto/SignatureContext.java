package org.keycloak.crypto;

public interface SignatureContext {

    String getKid();

    String getAlgorithm();

    byte[] sign(byte[] data) throws SignatureException;

}
