package org.keycloak.rest.admin.api;

import jakarta.ws.rs.NotFoundException;

import org.keycloak.admin.api.AdminApi;
import org.keycloak.admin.api.client.ClientsApi;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.rest.admin.api.client.DefaultClientsApi;
import org.keycloak.services.resources.admin.AdminRoot;

public class DefaultAdminApi implements AdminApi {
    private final KeycloakSession session;
    private final RealmModel realm;

    public DefaultAdminApi(KeycloakSession session, String realmName) {
        this.session = session;
        // TODO: This will be consolidated with context permissions logic later
        AdminRoot.authenticateRealmAdminRequest(session);
        RealmModel realm = session.realms().getRealmByName(realmName);
        if (realm == null) throw new NotFoundException("Realm not found.");
        session.getContext().setRealm(realm);
        this.realm = realm;
    }

    @Override
    public ClientsApi clientsV2() {
        return new DefaultClientsApi(session, realm);
    }
}
