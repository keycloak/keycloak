package org.keycloak.protocol.ssf.receiver;

import org.keycloak.crypto.KeyWrapper;
import org.keycloak.protocol.ssf.transmitter.SsfTransmitterMetadata;
import org.keycloak.provider.Provider;

import java.util.stream.Stream;

public interface SsfReceiver extends Provider {

    @Override
    default void close() {
    }

    Stream<KeyWrapper> getKeys();

    ReceiverModel getReceiverModel();

    ReceiverModel registerStream();

    ReceiverModel importStream();

    void unregisterStream();

    SsfTransmitterMetadata refreshTransmitterMetadata();

    void requestVerification();
}
