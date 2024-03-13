package org.keycloak.protocol.oidc.grants;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

/**
 * Factory for Pre-Authorized Code Grant
 *
 * @author <a href="https://github.com/wistefan">Stefan Wiedemann</a>
 */
public class PreAuthorizedCodeGrantTypeFactory implements OAuth2GrantTypeFactory {

    public static final String GRANT_TYPE = "urn:ietf:params:oauth:grant-type:pre-authorized_code";

    @Override
    public OAuth2GrantType create(KeycloakSession session) {
        return new PreAuthorizedCodeGrantType();
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
        return GRANT_TYPE;
    }

}
