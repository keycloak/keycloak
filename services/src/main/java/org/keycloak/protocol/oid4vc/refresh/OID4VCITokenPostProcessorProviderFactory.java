package org.keycloak.protocol.oid4vc.refresh;

import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oid4vc.OID4VCEnvironmentProviderFactory;
import org.keycloak.protocol.oidc.token.TokenPostProcessor;
import org.keycloak.protocol.oidc.token.TokenPostProcessorFactory;

public class OID4VCITokenPostProcessorProviderFactory implements TokenPostProcessorFactory, OID4VCEnvironmentProviderFactory {

    @Override
    public TokenPostProcessor create(KeycloakSession session) {
        return new OID4VCITokenPostProcessor(session);
    }

    @Override
    public String getId() {
        return "oid4vci";
    }
}
