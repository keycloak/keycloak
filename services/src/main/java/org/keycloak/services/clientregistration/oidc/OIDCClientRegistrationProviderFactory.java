package org.keycloak.services.clientregistration.oidc;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.services.clientregistration.ClientRegistrationProvider;
import org.keycloak.services.clientregistration.ClientRegistrationProviderFactory;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class OIDCClientRegistrationProviderFactory implements ClientRegistrationProviderFactory {

    public static final String ID = "openid-connect";

    @Override
    public ClientRegistrationProvider create(KeycloakSession session) {
        return new OIDCClientRegistrationProvider(session);
    }

    @Override
    public void init(Config.Scope config) {
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public void close() {
    }

    @Override
    public String getId() {
        return ID;
    }

}
