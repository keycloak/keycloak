package org.keycloak.testsuite.broker.oidc;

import java.util.Arrays;
import java.util.List;

import org.keycloak.broker.oidc.KeycloakOIDCIdentityProvider;
import org.keycloak.broker.oidc.KeycloakOIDCIdentityProviderFactory;
import org.keycloak.broker.oidc.OIDCIdentityProviderConfig;
import org.keycloak.broker.provider.IdentityProviderMapper;
import org.keycloak.models.KeycloakSession;

/**
 * @author Daniel Fesenmeyer <daniel.fesenmeyer@bosch.com>
 */
public class OverwrittenMappersTestIdentityProvider extends KeycloakOIDCIdentityProvider {

    public OverwrittenMappersTestIdentityProvider(KeycloakSession session, OIDCIdentityProviderConfig config) {
        super(session, config);
    }

    @Override
    public boolean isMapperSupported(IdentityProviderMapper mapper) {
        List<String> compatibleIdps = Arrays.asList(mapper.getCompatibleProviders());

        // provide the same mappers as are available for the parent provider (Keycloak-OIDC)
        return compatibleIdps.contains(IdentityProviderMapper.ANY_PROVIDER)
                || compatibleIdps.contains(KeycloakOIDCIdentityProviderFactory.PROVIDER_ID);
    }

}
