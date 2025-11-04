package org.keycloak.protocol.ssf.receiver.transmitterclient;

import org.keycloak.protocol.ssf.receiver.SsfReceiverModel;
import org.keycloak.protocol.ssf.transmitter.SsfTransmitterMetadata;

public interface SsfTransmitterClient {

    SsfTransmitterMetadata loadTransmitterMetadata(SsfReceiverModel receiverModel);

    SsfTransmitterMetadata fetchTransmitterMetadata(SsfReceiverModel receiverModel);

    boolean clearTransmitterMetadata(SsfReceiverModel receiverModel);
}
