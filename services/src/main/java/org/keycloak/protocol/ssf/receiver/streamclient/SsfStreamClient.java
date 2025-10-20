package org.keycloak.protocol.ssf.receiver.streamclient;

import org.keycloak.protocol.ssf.stream.CreateStreamRequest;
import org.keycloak.protocol.ssf.stream.SsfStreamRepresentation;
import org.keycloak.protocol.ssf.transmitter.SsfTransmitterMetadata;

public interface SsfStreamClient {

    SsfStreamRepresentation createStream(SsfTransmitterMetadata transmitterMetadata, String transmitterAccessToken, CreateStreamRequest request);

    void deleteStream(SsfTransmitterMetadata transmitterMetadata, String authorizationToken, String streamId);

    SsfStreamRepresentation getStream(SsfTransmitterMetadata transmitterMetadata, String authorizationToken, String streamId);
}
