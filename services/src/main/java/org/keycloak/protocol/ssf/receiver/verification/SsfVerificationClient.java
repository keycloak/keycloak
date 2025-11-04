package org.keycloak.protocol.ssf.receiver.verification;

import org.keycloak.protocol.ssf.receiver.SsfReceiverModel;
import org.keycloak.protocol.ssf.transmitter.SsfTransmitterMetadata;

/**
 * See: https://openid.net/specs/openid-sharedsignals-framework-1_0.html#section-8.1.4
 */
public interface SsfVerificationClient {

    void requestVerification(SsfReceiverModel receiverModel, SsfTransmitterMetadata transmitterMetadata, String state);
}
