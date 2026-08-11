package org.keycloak.ssf.transmitter.metadata;

import org.keycloak.models.KeycloakSession;
import org.keycloak.ssf.metadata.TransmitterMetadata;
import org.keycloak.ssf.transmitter.SsfTransmitterProvider;
import org.keycloak.wellknown.WellKnownProvider;

/**
 * Well-Known Provider implementation for SSF (Shared Signals and Events) protocol metadata.
 * This provider is responsible for exposing SSF-related metadata through Keycloak's Well-Known Provider infrastructure.
 */
public class SsfTransmitterMetadataWellKnownProvider implements WellKnownProvider {

    protected final KeycloakSession session;

    public SsfTransmitterMetadataWellKnownProvider(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public TransmitterMetadata getConfig() {
        SsfTransmitterProvider ssfProvider = session.getProvider(SsfTransmitterProvider.class);
        TransmitterMetadata transmitterMetadata = ssfProvider.metadataService().getTransmitterMetadata();
        return transmitterMetadata;
    }

    @Override
    public void close() {
        // NOOP
    }

}
