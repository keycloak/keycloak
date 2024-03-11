package org.keycloak.protocol.oid4vc.issuance;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.protocol.oid4vc.OID4VCEnvironmentProviderFactory;
import org.keycloak.wellknown.WellKnownProvider;
import org.keycloak.wellknown.WellKnownProviderFactory;

/**
 * Factory to provide the additional grant type for OIDC-Metadata
 *
 * @author <a href="https://github.com/wistefan">Stefan Wiedemann</a>
 */
public class OID4VCWellKnownProviderFactory implements WellKnownProviderFactory, OID4VCEnvironmentProviderFactory {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static final String PROVIDER_ID = "openid-configuration";

    @Override
    public WellKnownProvider create(KeycloakSession session) {
        return new OID4VCWellKnownProvider(session, OBJECT_MAPPER);
    }

    @Override
    public void init(Config.Scope config) {
        // no-op

    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        // no-op
    }

    @Override
    public void close() {
        // no-op

    }

    @Override
    public int getPriority() {
        return 0;
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }
}