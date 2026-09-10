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
 * Support for the legacy SSE (Shared Signals and Events) protocol, the predecessor of the SSF protocol.
 *
 * Factory implementation for creating instances of {@code SseTransmitterMetadataWellKnownProvider}.
 * This factory integrates with Keycloak's Well-Known Provider infrastructure and is enabled only
 * when the SSF feature is activated within the system configuration profile and the SSF Transmitter
 * feature is enabled for the current realm.
 */
public class SseTransmitterMetadataWellKnownProviderFactory implements WellKnownProviderFactory, EnvironmentDependentProviderFactory {

    public static final String PROVIDER_ID = "sse-configuration";

    /**
     * SPI property that disables the legacy SSE well-known endpoint.
     * Set {@code spi-wellknown--sse-configuration--enabled=false} to
     * unregister this factory entirely — deployments that only need the
     * SSF 1.0 {@code /.well-known/ssf-configuration} endpoint can opt out
     * of exposing the predecessor path. Defaults to {@code true} to
     * preserve backwards compatibility with receivers that still rely on
     * the legacy discovery document.
     */
    public static final String CONFIG_ENABLED = "enabled";

    @Override
    public WellKnownProvider create(KeycloakSession session) {
        if (!isEnabledForRealm(session)) {
            return null;
        }
        return new SseTransmitterMetadataWellKnownProvider(session);
    }

    protected boolean isEnabledForRealm(KeycloakSession session) {
        return Ssf.isTransmitterEnabled(session.getContext().getRealm());
    }

    @Override
    public void init(Config.Scope config) {
        // NOOP
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
        if (!Profile.isFeatureEnabled(Profile.Feature.SSF)) {
            return false;
        }
        return config.getBoolean(CONFIG_ENABLED, false);
    }
}
