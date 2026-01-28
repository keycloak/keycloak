package org.keycloak.migration.migrators;

import org.keycloak.migration.ModelVersion;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.DefaultAuthenticationFlows;


public class MigrateTo26_6_0 extends RealmMigration {

    public static final ModelVersion VERSION = new ModelVersion("26.6.0");

    @Override
    public ModelVersion getVersion() {
        return VERSION;
    }


    @Override
    public void migrateRealm(KeycloakSession session, RealmModel realm) {
        DefaultAuthenticationFlows.addOrganizationBrowserFlowStep(realm, realm.getBrowserFlow());
    }
}
