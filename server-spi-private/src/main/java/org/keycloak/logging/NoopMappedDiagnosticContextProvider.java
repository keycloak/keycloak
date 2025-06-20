package org.keycloak.logging;

import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.OrganizationModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.sessions.AuthenticationSessionModel;

public class NoopMappedDiagnosticContextProvider implements MappedDiagnosticContextProvider {

    @Override
    public void update(KeycloakContext keycloakContext, AuthenticationSessionModel session) {
        // no-op
    }

    @Override
    public void update(KeycloakContext keycloakContext, RealmModel realm) {
        // no-op
    }

    @Override
    public void update(KeycloakContext keycloakContext, ClientModel client) {
        // no-op
    }

    @Override
    public void update(KeycloakContext keycloakContext, OrganizationModel organization) {
        // no-op
    }

    @Override
    public void update(KeycloakContext keycloakContext, UserSessionModel userSession) {
        // no-op
    }

    @Override
    public void close() {
        // no-op
    }
}
