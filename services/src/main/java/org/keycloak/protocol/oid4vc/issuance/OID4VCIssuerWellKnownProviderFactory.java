package org.keycloak.protocol.oid4vc.issuance;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.protocol.oid4vc.OID4VCEnvironmentProviderFactory;
import org.keycloak.wellknown.WellKnownProvider;
import org.keycloak.wellknown.WellKnownProviderFactory;

/**
 * {@link  WellKnownProviderFactory} implementation for the OID4VCI metadata
 * <p>
 * {@see https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html#section-10.2.2}
 *
 * @author <a href="https://github.com/wistefan">Stefan Wiedemann</a>
 */
public class OID4VCIssuerWellKnownProviderFactory implements WellKnownProviderFactory, OID4VCEnvironmentProviderFactory {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static final String PROVIDER_ID = "openid-credential-issuer";

    @Override
    public WellKnownProvider create(KeycloakSession session) {
        return new OID4VCIssuerWellKnownProvider(session, OBJECT_MAPPER);
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
    public String getId() {
        return PROVIDER_ID;
    }
}