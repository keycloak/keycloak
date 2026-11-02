package org.keycloak.migration.migrators;


import org.keycloak.authorization.fgap.AdminPermissionsSchema;
import org.keycloak.migration.ModelVersion;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;

public class MigrateTo27_0_0 extends RealmMigration {

    public static final ModelVersion VERSION = new ModelVersion("27.0.0");

    @Override
    public ModelVersion getVersion() {
        return VERSION;
    }


    @Override
    public void migrateRealm(KeycloakSession session, RealmModel realm) {
        AdminPermissionsSchema.SCHEMA.addResourceTypeScope(session, realm, AdminPermissionsSchema.ORGANIZATIONS_RESOURCE_TYPE, AdminPermissionsSchema.VIEW_MEMBERS);
        AdminPermissionsSchema.SCHEMA.addResourceTypeScope(session, realm, AdminPermissionsSchema.ORGANIZATIONS_RESOURCE_TYPE, AdminPermissionsSchema.MANAGE_MEMBERS);
    }
}
