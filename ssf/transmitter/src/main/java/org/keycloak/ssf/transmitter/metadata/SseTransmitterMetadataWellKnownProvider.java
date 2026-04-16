package org.keycloak.ssf.transmitter.metadata;

import java.util.stream.Collectors;

import org.keycloak.models.KeycloakSession;
import org.keycloak.ssf.metadata.TransmitterMetadata;

/**
 * Well-Known Provider implementation for the legacy SSE (Shared Signals and Events) protocol metadata.
 * This provider is responsible for exposing SSE-related metadata through Keycloak's Well-Known Provider infrastructure.
 */
public class SseTransmitterMetadataWellKnownProvider extends SsfTransmitterMetadataWellKnownProvider {

    public SseTransmitterMetadataWellKnownProvider(KeycloakSession session) {
        super(session);
    }

    @Override
    public TransmitterMetadata getConfig() {

        TransmitterMetadata sseMetadata = new TransmitterMetadata(super.getConfig());

        // Remove "new" PUSH and POLL delivery methods all urn:... URIs
        sseMetadata.setDeliveryMethodSupported(sseMetadata.getDeliveryMethodSupported()
                .stream()
                .filter(dm -> !dm.startsWith("urn:"))
                .collect(Collectors.toSet()));

        // Remove unsupported fields.
        sseMetadata.setDefaultSubjects(null);
        sseMetadata.setSpecVersion(null);
        sseMetadata.setStatusEndpoint(null);
        sseMetadata.setAuthorizationSchemes(null);

        return sseMetadata;
    }

    @Override
    public void close() {
        // NOOP
    }

}
