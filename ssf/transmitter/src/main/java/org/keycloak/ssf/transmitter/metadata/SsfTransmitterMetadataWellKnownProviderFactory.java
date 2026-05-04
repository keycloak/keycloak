package org.keycloak.ssf.transmitter.metadata;

import org.keycloak.Config;
import org.keycloak.common.Profile;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.EnvironmentDependentProviderFactory;
import org.keycloak.ssf.Ssf;
import org.keycloak.wellknown.WellKnownProvider;
import org.keycloak.wellknown.WellKnownProviderFactory;

/**
 * Factory implementation for creating instances of {@code SsfTransmitterMetadataWellKnownProvider}.
 * This factory integrates with Keycloak's Well-Known Provider infrastructure and is enabled only
 * when the SSF feature is activated within the system configuration profile.
 */
public class SsfTransmitterMetadataWellKnownProviderFactory implements WellKnownProviderFactory, EnvironmentDependentProviderFactory {

    public static final String PROVIDER_ID = "ssf-configuration";

    @Override
    public WellKnownProvider create(KeycloakSession session) {
        if (!Ssf.isTransmitterEnabled(session.getContext().getRealm())) {
            return null;
        }
        return new SsfTransmitterMetadataWellKnownProvider(session);
    }

    @Override
    public void init(Config.Scope config) {
        // NOOP
    }

    @Override
    public boolean isAvailableViaServerMetadata() {
        return true;
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        // NOOP
    }

    @Override
    public void close() {
        // NOOP
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
