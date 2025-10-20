package org.keycloak.protocol.ssf.receiver.verification;

import org.keycloak.protocol.ssf.receiver.ReceiverModel;
import org.keycloak.protocol.ssf.transmitter.SsfTransmitterMetadata;

/**
 * See: https://openid.net/specs/openid-sharedsignals-framework-1_0.html#section-7.1.4
 */
public interface SsfVerificationClient {

    void requestVerification(ReceiverModel receiverModel, SsfTransmitterMetadata transmitterMetadata, String state);
}
