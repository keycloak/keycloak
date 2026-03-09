package org.keycloak.protocol.ssf.transmitter.metadata;

import java.util.stream.Collectors;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.protocol.ssf.Ssf;
import org.keycloak.wellknown.WellKnownProvider;
import org.keycloak.wellknown.WellKnownProviderFactory;

public class SseTransmitterMetadataWellKnownProvider implements WellKnownProvider {

    @Override
    public Object getConfig() {
        SsfTransmitterMetadata transmitterMetadata = Ssf.transmitter().transmitterService().getTransmitterMetadata();
        transmitterMetadata.setDeliveryMethodSupported(transmitterMetadata.getDeliveryMethodSupported()
                .stream()
                .filter(dm -> !dm.startsWith("urn:"))
                .collect(Collectors.toSet()));
        transmitterMetadata.setDefaultSubjects(null);
        transmitterMetadata.setSpecVersion(null);
        transmitterMetadata.setStatusEndpoint(null);
        transmitterMetadata.setAuthorizationSchemes(null);
        return transmitterMetadata;
    }

    @Override
    public void close() {

    }

    public static class Factory implements WellKnownProviderFactory {

        private final static SseTransmitterMetadataWellKnownProvider INSTANCE = new SseTransmitterMetadataWellKnownProvider();

        public static final String PROVIDER_ID = "sse-configuration";

        @Override
        public WellKnownProvider create(KeycloakSession session) {
            return INSTANCE;
        }

        @Override
        public void init(Config.Scope config) {

        }

        @Override
        public void postInit(KeycloakSessionFactory factory) {

        }

        @Override
        public void close() {

        }

        @Override
        public String getId() {
            return PROVIDER_ID;
        }
    }
}
