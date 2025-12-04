package org.keycloak.protocol.ssf.receiver.verification;

import org.keycloak.protocol.ssf.receiver.SsfReceiver;
import org.keycloak.protocol.ssf.receiver.transmitter.SsfTransmitterMetadata;

/**
 * Client to perform SSF Receiver stream verification with a remote SSF Transmitter.
 *
 * See: https://openid.net/specs/openid-sharedsignals-framework-1_0.html#section-8.1.4
 */
public interface SsfVerificationClient {

    void requestVerification(SsfReceiver receiver, SsfTransmitterMetadata transmitterMetadata, String state);
}
