package org.keycloak.migration.migrators;

import org.keycloak.migration.ModelVersion;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.idm.RealmRepresentation;

public class MigrateTo21_0_0 implements Migration {

    public static final ModelVersion VERSION = new ModelVersion("21.0.0");

    @Override
    public void migrate(KeycloakSession session) {
        session.realms().getRealmsStream().forEach(this::updateAdminTheme);
    }

    @Override
    public void migrateImport(KeycloakSession session, RealmModel realm, RealmRepresentation rep, boolean skipUserDependent) {
        updateAdminTheme(realm);
    }

    private void updateAdminTheme(RealmModel realm) {
        String adminTheme = realm.getAdminTheme();
        if ("keycloak".equals(adminTheme) || "rh-sso".equals(adminTheme)) {
            realm.setAdminTheme("keycloak.v2");
        }
    }

    @Override
    public ModelVersion getVersion() {
        return VERSION;
    }
}
