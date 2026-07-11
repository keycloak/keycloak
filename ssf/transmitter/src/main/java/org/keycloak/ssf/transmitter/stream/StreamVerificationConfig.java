package org.keycloak.ssf.transmitter.stream;

/**
 * @param autoVerifyStream When {@code true}, the transmitter
 * dispatches a stream-verification SET shortly after stream creation.
 * When {@code false} the receiver drives verification on demand via
 * the {@code /verify} endpoint.
 * @param verificationDelayMillis Delay in milliseconds before the
 * transmitter dispatches the post-create verification SET. Only
 * applies when {@code autoVerifyStream} is {@code true}.
 */
public record StreamVerificationConfig(
        boolean autoVerifyStream,
        int verificationDelayMillis) {
}
