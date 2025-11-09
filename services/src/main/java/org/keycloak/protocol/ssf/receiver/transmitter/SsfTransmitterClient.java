package org.keycloak.protocol.ssf.receiver.transmitter;

import org.keycloak.protocol.ssf.receiver.SsfReceiver;

/**
 * Client to access metadata from a remote SSF Transmitter.
 */
public interface SsfTransmitterClient {

    SsfTransmitterMetadata loadTransmitterMetadata(SsfReceiver receiver);

    SsfTransmitterMetadata fetchTransmitterMetadata(SsfReceiver receiver);

    boolean clearTransmitterMetadata(SsfReceiver receiver);
}
