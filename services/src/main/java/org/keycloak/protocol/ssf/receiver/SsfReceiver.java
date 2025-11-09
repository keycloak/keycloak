package org.keycloak.protocol.ssf.receiver;

import org.keycloak.protocol.ssf.receiver.transmitter.SsfTransmitterMetadata;
import org.keycloak.provider.Provider;

/**
 * Represents a SSF Receiver.
 */
public interface SsfReceiver extends Provider {

    @Override
    default void close() {
    }

    SsfReceiverProviderConfig getConfig();

    SsfTransmitterMetadata getTransmitterMetadata();

    SsfTransmitterMetadata refreshTransmitterMetadata();

    void requestVerification();

    String getTransmitterConfigUrl();
}
