package org.keycloak.login.freemarker;

import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorUtil;
import org.keycloak.authentication.authenticators.LoginFormPasswordAuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.services.Urls;

import java.net.URI;
import java.util.List;

/**
 */
public class AuthenticatorConfiguredMethod implements TemplateMethodModelEx {
    private final RealmModel realm;
    private final UserModel user;
    private final KeycloakSession session;

    public AuthenticatorConfiguredMethod(RealmModel realm, UserModel user, KeycloakSession session) {
        this.realm = realm;
        this.user = user;
        this.session = session;
    }

    @Override
    public Object exec(List list) throws TemplateModelException {
        String providerId = list.get(0).toString();
        Authenticator authenticator = session.getProvider(Authenticator.class, providerId);
        return authenticator.configuredFor(session, realm, user);
    }
}
