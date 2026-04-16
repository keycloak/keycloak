package org.keycloak.ssf.transmitter.metadata;

import java.util.stream.Collectors;

import org.keycloak.Config;
import org.keycloak.common.Profile;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.EnvironmentDependentProviderFactory;
import org.keycloak.ssf.metadata.TransmitterMetadata;
import org.keycloak.ssf.transmitter.SsfTransmitter;
import org.keycloak.ssf.transmitter.SsfTransmitterProvider;
import org.keycloak.wellknown.WellKnownProvider;
import org.keycloak.wellknown.WellKnownProviderFactory;

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
