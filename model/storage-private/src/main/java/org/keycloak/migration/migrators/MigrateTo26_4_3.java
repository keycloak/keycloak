package org.keycloak.migration.migrators;


import org.keycloak.authorization.fgap.AdminPermissionsSchema;
import org.keycloak.migration.ModelVersion;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;

public class MigrateTo26_4_3 extends RealmMigration {

    public static final ModelVersion VERSION = new ModelVersion("26.4.3");

    @Override
    public ModelVersion getVersion() {
        return VERSION;
    }


    @Override
    public void migrateRealm(KeycloakSession session, RealmModel realm) {
        AdminPermissionsSchema.SCHEMA.addResourceTypeScope(session, realm, AdminPermissionsSchema.USERS_RESOURCE_TYPE, AdminPermissionsSchema.RESET_PASSWORD);
    }
}
