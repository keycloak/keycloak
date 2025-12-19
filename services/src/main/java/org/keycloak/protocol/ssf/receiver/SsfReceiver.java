package org.keycloak.protocol.ssf.receiver;

import org.keycloak.protocol.ssf.receiver.registration.SsfReceiverRegistrationProviderConfig;
import org.keycloak.protocol.ssf.receiver.transmitter.SsfTransmitterMetadata;
import org.keycloak.provider.Provider;

/**
 * Represents a SSF Receiver.
 */
public interface SsfReceiver extends Provider {

    @Override
    default void close() {
    }

    SsfReceiverRegistrationProviderConfig getConfig();

    SsfTransmitterMetadata getTransmitterMetadata();

    SsfTransmitterMetadata refreshTransmitterMetadata();

    void requestVerification();

    String getTransmitterConfigUrl();
}
