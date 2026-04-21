package org.keycloak.rest.admin.api;

import org.keycloak.admin.api.AdminApi;
import org.keycloak.admin.api.client.ClientsApi;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.oidc.TokenManager;
import org.keycloak.rest.admin.api.client.DefaultClientsApi;
import org.keycloak.services.resources.admin.AdminRoot;
import org.keycloak.services.resources.admin.RealmAdminResource;
import org.keycloak.services.resources.admin.RealmsAdminResource;
import org.keycloak.services.resources.admin.fgap.AdminPermissionEvaluator;
import org.keycloak.services.resources.admin.fgap.AdminPermissions;

public class DefaultAdminApi implements AdminApi {
    private final KeycloakSession session;
    private final RealmModel realm;
    private final AdminPermissionEvaluator permissions;

    // v1 resources
    private final RealmAdminResource realmAdminResource;

    public DefaultAdminApi(KeycloakSession session, String realmName) {
        this.session = session;
        var authInfo = AdminRoot.authenticateRealmAdminRequest(session);
        this.permissions = AdminPermissions.evaluator(session, authInfo.getRealm(), authInfo);
        this.realm = session.realms().getRealmByName(realmName);
        // remove v1 resource once we are not attached to API v1
        this.realmAdminResource = new RealmsAdminResource(session, authInfo, new TokenManager()).getRealmAdmin(realmName);
    }

    @Override
    public ClientsApi clientsV2() {
        return new DefaultClientsApi(session, realm, permissions, realmAdminResource);
    }
}
