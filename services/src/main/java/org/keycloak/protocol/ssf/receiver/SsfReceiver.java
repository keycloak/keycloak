package org.keycloak.protocol.ssf.receiver;

import org.keycloak.protocol.ssf.receiver.transmitter.SsfTransmitterMetadata;
import org.keycloak.provider.Provider;

public interface SsfReceiver extends Provider {

    @Override
    default void close() {
    }

    SsfReceiverProviderConfig getReceiverProviderConfig();

    SsfTransmitterMetadata getTransmitterMetadata();

    SsfTransmitterMetadata refreshTransmitterMetadata();

    void requestVerification();

    String getTransmitterConfigUrl();
}
