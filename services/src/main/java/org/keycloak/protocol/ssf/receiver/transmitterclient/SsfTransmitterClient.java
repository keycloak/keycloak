package org.keycloak.protocol.ssf.receiver.transmitterclient;


import org.keycloak.protocol.ssf.receiver.ReceiverModel;
import org.keycloak.protocol.ssf.transmitter.SsfTransmitterMetadata;

public interface SsfTransmitterClient {

    SsfTransmitterMetadata loadTransmitterMetadata(ReceiverModel receiverModel);

    SsfTransmitterMetadata fetchTransmitterMetadata(ReceiverModel receiverModel);

    boolean clearTransmitterMetadata(ReceiverModel receiverModel);
}
