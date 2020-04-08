package org.keycloak.protocol.ciba.resolvers;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;

public class DefaultCIBALoginUserResolver implements CIBALoginUserResolver {

    private KeycloakSession session;

    public DefaultCIBALoginUserResolver(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public UserModel getUserFromLoginHint(String loginHint) {
        return KeycloakModelUtils.findUserByNameOrEmail(session, session.getContext().getRealm(), loginHint);
    }

    @Override
    public UserModel getUserFromLoginHintToken(String loginHintToken) {
        // not yet supported
        return null;
    }

    @Override
    public UserModel getUserFromIdTokenHint(String idToken) {
        // not yet supported
        return null;
    }

    @Override
    public String getInfoUsedByAuthentication(UserModel user) {
        return user.getUsername();
    }

    @Override
    public UserModel getUserFromInfoUsedByAuthentication(String info) {
        return KeycloakModelUtils.findUserByNameOrEmail(session, session.getContext().getRealm(), info);
    }

    @Override
    public void close() {
    }

}
