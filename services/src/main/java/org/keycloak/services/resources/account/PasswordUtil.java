package org.keycloak.services.resources.account;

import org.keycloak.credential.CredentialModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

public class PasswordUtil {

    private KeycloakSession session;
    private UserModel user;

    public PasswordUtil(KeycloakSession session, UserModel user) {
        this.session = session;
        this.user = user;
    }

    public boolean isConfigured(KeycloakSession session, RealmModel realm, UserModel user) {
        return session.userCredentialManager().isConfiguredFor(realm, user, CredentialModel.PASSWORD);
    }

    public void update() {

    }

}
