package org.keycloak.admin.ui.rest;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.services.resources.admin.AdminEventBuilder;
import org.keycloak.services.resources.admin.ext.AdminRealmResourceProvider;
import org.keycloak.services.resources.admin.ext.AdminRealmResourceProviderFactory;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;

public final class EffectiveRoleMappingProvider implements AdminRealmResourceProviderFactory, AdminRealmResourceProvider {
    public AdminRealmResourceProvider create(KeycloakSession session) {
        return this;
    }

    public void init(Config.Scope config) {

    }

    public void postInit(KeycloakSessionFactory factory) {

    }

    public void close() {
    }

    public String getId() {
        return "admin-ui-effective-roles";
    }

    public Object getResource(KeycloakSession session, RealmModel realm, AdminPermissionEvaluator auth, AdminEventBuilder adminEvent) {
        return new EffectiveRoleMappingResource(session, realm, auth);
    }
}
