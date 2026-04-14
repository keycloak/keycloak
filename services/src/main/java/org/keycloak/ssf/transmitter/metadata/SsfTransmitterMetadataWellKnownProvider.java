package org.keycloak.ssf.transmitter.metadata;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.ssf.Ssf;
import org.keycloak.wellknown.WellKnownProvider;
import org.keycloak.wellknown.WellKnownProviderFactory;


public class SsfTransmitterMetadataWellKnownProvider implements WellKnownProvider {

    @Override
    public Object getConfig() {
        SsfTransmitterMetadata transmitterMetadata = Ssf.transmitter().transmitterService().getTransmitterMetadata();
        return transmitterMetadata;
    }

    @Override
    public void close() {

    }

    public static class Factory implements WellKnownProviderFactory {

        private final static SsfTransmitterMetadataWellKnownProvider INSTANCE = new SsfTransmitterMetadataWellKnownProvider();

        public static final String PROVIDER_ID = "ssf-configuration";

        @Override
        public WellKnownProvider create(KeycloakSession session) {
            return INSTANCE;
        }

        @Override
        public void init(Config.Scope config) {

        }

        @Override
        public boolean isAvailableViaServerMetadata() {
            return true;
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
