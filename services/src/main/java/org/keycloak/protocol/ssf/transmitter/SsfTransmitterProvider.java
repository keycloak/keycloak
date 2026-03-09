package org.keycloak.protocol.ssf.transmitter;

import org.keycloak.protocol.ssf.transmitter.delivery.SecurityEventTokenDispatcher;
import org.keycloak.protocol.ssf.transmitter.event.SecurityEventTokenMapper;
import org.keycloak.protocol.ssf.transmitter.metadata.SsfTransmitterMetadataService;
import org.keycloak.protocol.ssf.transmitter.resources.StreamManagementResource;
import org.keycloak.protocol.ssf.transmitter.stream.StreamService;
import org.keycloak.protocol.ssf.transmitter.resources.StreamStatusResource;
import org.keycloak.protocol.ssf.transmitter.resources.StreamVerificationResource;
import org.keycloak.protocol.ssf.transmitter.stream.StreamVerificationService;
import org.keycloak.provider.Provider;

public interface SsfTransmitterProvider extends Provider {

    default void close() {
    }

    StreamVerificationService verificationService();

    SsfTransmitterMetadataService transmitterService();

    SecurityEventTokenMapper securityEventTokenMapper();

    SecurityEventTokenDispatcher securityEventTokenDispatcher();

    StreamManagementResource streamManagementEndpoint();

    StreamService streamService();

    StreamStatusResource streamStatusEndpoint();

    StreamVerificationResource verificationEndpoint();
}
