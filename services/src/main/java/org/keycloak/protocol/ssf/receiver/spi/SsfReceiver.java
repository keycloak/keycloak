package org.keycloak.protocol.ssf.receiver.spi;

import org.keycloak.crypto.KeyWrapper;
import org.keycloak.protocol.ssf.receiver.SsfReceiverModel;
import org.keycloak.protocol.ssf.stream.StreamStatus;
import org.keycloak.protocol.ssf.transmitter.SsfTransmitterMetadata;
import org.keycloak.provider.Provider;

import java.util.stream.Stream;

public interface SsfReceiver extends Provider {

    @Override
    default void close() {
    }

    Stream<KeyWrapper> getKeys();

    SsfReceiverModel getReceiverModel();

    SsfReceiverModel registerStream();

    SsfReceiverModel importStream();

    void unregisterStream();

    SsfTransmitterMetadata refreshTransmitterMetadata();

    void requestVerification();

    void updateStreamStatus(StreamStatus status);
}
