package org.keycloak.protocol.oauth2.cimd.provider;

import org.keycloak.Config;
import org.keycloak.common.Profile;
import org.keycloak.provider.EnvironmentDependentProviderFactory;
import org.keycloak.provider.ProviderFactory;

/**
 * The interface is a factory of {@link ClientIdMetadataDocumentProvider}.
 *
 * @author <a href="mailto:takashi.norimatsu.ws@hitachi.com">Takashi Norimatsu</a>
 */
public interface ClientIdMetadataDocumentProviderFactory extends ProviderFactory<ClientIdMetadataDocumentProvider>,
        EnvironmentDependentProviderFactory {

    @Override
    default boolean isSupported(Config.Scope config) {
        return Profile.isFeatureEnabled(Profile.Feature.CIMD);
    }
}
