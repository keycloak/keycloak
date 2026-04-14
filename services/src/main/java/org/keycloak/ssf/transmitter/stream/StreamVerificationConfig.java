package org.keycloak.ssf.transmitter.stream;

/**
 * @param verificationTrigger Indicates how the verification is triggered.
 * @param verificationDelayMillis The verification delay in milliseconds, in case the transmitter triggers the verification.
 */
public record StreamVerificationConfig(
        SsfVerificationTrigger verificationTrigger,
        int verificationDelayMillis) {
}
