
package org.keycloak.protocol.oid4vc;

import org.keycloak.Config;
import org.keycloak.common.Profile;
import org.keycloak.provider.EnvironmentDependentProviderFactory;

public interface OID4VCEnvironmentProviderFactory extends EnvironmentDependentProviderFactory {

    @Override
    default boolean isSupported(Config.Scope config) {
        return Profile.isFeatureEnabled(Profile.Feature.OID4VC_VCI);
    }

    @Override
    default boolean isSupported() {
        return Profile.isFeatureEnabled(Profile.Feature.OID4VC_VCI);
    }
}