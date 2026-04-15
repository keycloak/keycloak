package org.keycloak.ssf.receiver;

import org.keycloak.provider.Provider;
import org.keycloak.ssf.metadata.TransmitterMetadata;
import org.keycloak.ssf.receiver.registration.SsfReceiverRegistrationProviderConfig;

/**
 * Represents a SSF Receiver.
 */
public interface SsfReceiver extends Provider {

    @Override
    default void close() {
    }

    SsfReceiverRegistrationProviderConfig getConfig();

    TransmitterMetadata getTransmitterMetadata();

    TransmitterMetadata refreshTransmitterMetadata();

    void requestVerification();

    String getTransmitterConfigUrl();
}
