package org.keycloak.protocol.oidc;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.wellknown.WellKnownProvider;
import org.keycloak.wellknown.WellKnownProviderFactory;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class OIDCWellKnownProviderFactory implements WellKnownProviderFactory {

    private WellKnownProvider provider;

    @Override
    public WellKnownProvider create(KeycloakSession session) {
        return provider;
    }

    @Override
    public void init(Config.Scope config) {
        provider = new OIDCWellKnownProvider();
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public void close() {
        provider = null;
    }

    @Override
    public String getId() {
        return "openid-configuration";
    }

}
