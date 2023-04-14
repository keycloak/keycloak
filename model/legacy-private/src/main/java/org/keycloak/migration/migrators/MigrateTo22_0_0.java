package org.keycloak.migration.migrators;

import org.keycloak.migration.ModelVersion;
import org.keycloak.models.AccountRoles;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.representations.idm.RealmRepresentation;

public class MigrateTo22_0_0 implements Migration {

    public static final ModelVersion VERSION = new ModelVersion("22.0.0");

    @Override
    public void migrate(KeycloakSession session) {
        session.realms().getRealmsStream().forEach(this::updateAccountTheme);
    }

    @Override
    public void migrateImport(KeycloakSession session, RealmModel realm, RealmRepresentation rep, boolean skipUserDependent) {
        updateAccountTheme(realm);
    }

    private void updateAccountTheme(RealmModel realm) {
        String accountTheme = realm.getAccountTheme();
        if ("keycloak".equals(accountTheme) || "rh-sso".equals(accountTheme)) {
            realm.setAccountTheme("keycloak.v2");
        }
    }

    @Override
    public ModelVersion getVersion() {
        return VERSION;
    }
}
