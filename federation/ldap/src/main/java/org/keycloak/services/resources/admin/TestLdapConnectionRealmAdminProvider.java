package org.keycloak.services.resources.admin;

import org.keycloak.Config.Scope;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.services.resources.admin.ext.AdminRealmResourceProvider;
import org.keycloak.services.resources.admin.ext.AdminRealmResourceProviderFactory;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;

public class TestLdapConnectionRealmAdminProvider implements AdminRealmResourceProviderFactory, AdminRealmResourceProvider {

    @Override
    public AdminRealmResourceProvider create(KeycloakSession session) {
        return this;
    }

    @Override
    public void init(Scope config) {
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public void close() {
    }

    @Override
    public String getId() {
        return "testLDAPConnection";
    }

    @Override
    public Object getResource(KeycloakSession session, RealmModel realm, AdminPermissionEvaluator auth, AdminEventBuilder adminEvent) {
        return new TestLdapConnectionResource(realm, auth, adminEvent);
    }

}
