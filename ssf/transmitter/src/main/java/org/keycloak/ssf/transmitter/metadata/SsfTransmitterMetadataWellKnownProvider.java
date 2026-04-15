package org.keycloak.ssf.transmitter.metadata;

import org.keycloak.Config;
import org.keycloak.common.Profile;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.EnvironmentDependentProviderFactory;
import org.keycloak.ssf.metadata.TransmitterMetadata;
import org.keycloak.ssf.transmitter.SsfTransmitter;
import org.keycloak.wellknown.WellKnownProvider;
import org.keycloak.wellknown.WellKnownProviderFactory;


public class SsfTransmitterMetadataWellKnownProvider implements WellKnownProvider {

    @Override
    public Object getConfig() {
        TransmitterMetadata transmitterMetadata = SsfTransmitter.current().metadataService().getTransmitterMetadata();
        return transmitterMetadata;
    }

    @Override
    public void close() {

    }

    public static class Factory implements WellKnownProviderFactory, EnvironmentDependentProviderFactory {

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

        @Override
        public boolean isSupported(Config.Scope config) {
            return Profile.isFeatureEnabled(Profile.Feature.SSF);
        }
    }
}
