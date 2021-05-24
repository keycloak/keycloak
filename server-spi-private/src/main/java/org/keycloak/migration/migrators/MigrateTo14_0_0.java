package org.keycloak.migration.migrators;

import org.keycloak.migration.ModelVersion;
import org.keycloak.models.*;
import org.keycloak.representations.idm.RealmRepresentation;

import java.util.Objects;

public class MigrateTo14_0_0 implements Migration {

    public static final ModelVersion VERSION = new ModelVersion("14.0.0");

    @Override
    public void migrate(KeycloakSession session) {
        session.realms().getRealmsStream().forEach(this::addViewGroupsRole);
    }

    @Override
    public void migrateImport(KeycloakSession session, RealmModel realm, RealmRepresentation rep, boolean skipUserDependent) {
        addViewGroupsRole(realm);
    }

    private void addViewGroupsRole(RealmModel realm) {
        ClientModel accountClient = realm.getClientByClientId(Constants.ACCOUNT_MANAGEMENT_CLIENT_ID);
        if (accountClient != null && accountClient.getRole(AccountRoles.VIEW_GROUPS) == null) {
            RoleModel viewAppRole = accountClient.addRole(AccountRoles.VIEW_GROUPS);
            viewAppRole.setDescription("${role_" + AccountRoles.VIEW_GROUPS + "}");
        }
    }

    @Override
    public ModelVersion getVersion() {
        return VERSION;
    }
}