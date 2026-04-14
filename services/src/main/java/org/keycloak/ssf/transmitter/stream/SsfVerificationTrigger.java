package org.keycloak.ssf.transmitter.stream;

public enum SsfVerificationTrigger {

    /**
     * The transmitter triggers the verification, e.g. after stream creation.
     */
    TRANSMITTER_INITIATED,

    /**
     * The receiver requests the verification with a verification request.
     */
    RECEIVER_INITIATED
}
