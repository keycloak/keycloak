package org.keycloak.admin.ui.rest;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.services.resources.admin.fgap.AdminPermissionEvaluator;

public abstract class RoleMappingResource {
    protected final KeycloakSession session;
    protected final RealmModel realm;
    protected final AdminPermissionEvaluator auth;

    public RoleMappingResource(KeycloakSession session, RealmModel realm, AdminPermissionEvaluator auth) {
        this.session = session;
        this.realm = realm;
        this.auth = auth;
    }
}
