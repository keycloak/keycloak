package org.keycloak.ssf.receiver.transmitter;

import org.keycloak.ssf.metadata.TransmitterMetadata;
import org.keycloak.ssf.receiver.SsfReceiver;

/**
 * Client to access metadata from a remote SSF Transmitter.
 */
public interface SsfTransmitterClient {

    TransmitterMetadata loadTransmitterMetadata(SsfReceiver receiver);

    TransmitterMetadata fetchTransmitterMetadata(SsfReceiver receiver);

    boolean clearTransmitterMetadata(SsfReceiver receiver);
}
