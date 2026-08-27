package org.keycloak.migration.migrators;

import org.keycloak.authorization.fgap.AdminPermissionsSchema;
import org.keycloak.common.Profile;
import org.keycloak.migration.MigrationProvider;
import org.keycloak.migration.ModelVersion;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;

public class MigrateTo26_8_0 extends RealmMigration {

    public static final ModelVersion VERSION = new ModelVersion("26.8.0");

    @Override
    public ModelVersion getVersion() {
        return VERSION;
    }

    @Override
    public void migrateRealm(KeycloakSession session, RealmModel realm) {
        AdminPermissionsSchema.SCHEMA.addResourceTypeScope(session, realm, AdminPermissionsSchema.USERS_RESOURCE_TYPE, AdminPermissionsSchema.DELEGATE);
        AdminPermissionsSchema.SCHEMA.addResourceTypeScope(session, realm, AdminPermissionsSchema.GROUPS_RESOURCE_TYPE, AdminPermissionsSchema.DELEGATE_MEMBERS);

        if (Profile.isFeatureEnabled(Profile.Feature.TOKEN_EXCHANGE_DELEGATION)) {
            MigrationProvider migrationProvider = session.getProvider(MigrationProvider.class);
            // FGAP guards delegation permission evaluation, so scopes can safely be optional
            ClientScopeModel userDelegation = migrationProvider.addOIDCUserDelegationClientScope(realm);
            realm.addDefaultClientScope(userDelegation, false);
            ClientScopeModel clientDelegation = migrationProvider.addOIDCClientDelegationClientScope(realm);
            realm.addDefaultClientScope(clientDelegation, false);
        }
    }
}
