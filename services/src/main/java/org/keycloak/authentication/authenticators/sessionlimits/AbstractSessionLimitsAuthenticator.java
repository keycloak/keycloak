package org.keycloak.authentication.authenticators.sessionlimits;

import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.Authenticator;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.saml.common.util.StringUtil;

import java.util.Map;

public abstract class AbstractSessionLimitsAuthenticator implements Authenticator {
    protected KeycloakSession session;

    protected boolean exceedsLimit(long count, long limit) {
        if (limit < 0) { // if limit is negative, no valid limit configuration is found
            return false;
        }
        return count > limit - 1;
    }

    protected int getIntConfigProperty(String key, Map<String, String> config) {
        String value = config.get(key);
        if (StringUtil.isNullOrEmpty(value)) {
            return -1;
        }
        return Integer.parseInt(value);
    }

    @Override
    public void action(AuthenticationFlowContext context) {

    }

    @Override
    public boolean requiresUser() {
        return false;
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        return true;
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
    }

    @Override
    public void close() {

    }
}
