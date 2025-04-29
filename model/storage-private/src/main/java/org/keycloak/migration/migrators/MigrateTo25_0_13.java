package org.keycloak.migration.migrators;

import org.jboss.logging.Logger;
import org.keycloak.migration.MigrationProvider;
import org.keycloak.migration.ModelVersion;
import org.keycloak.models.*;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.idm.RealmRepresentation;

import java.util.List;

public class MigrateTo25_0_13 implements Migration{
    public static final ModelVersion VERSION = new ModelVersion("25.0.13");
    private static final Logger LOG = Logger.getLogger(MigrateTo25_0_13.class);

    @Override
    public ModelVersion getVersion() {
        return VERSION;
    }

    @Override
    public void migrate(KeycloakSession session) {
        if (session.sessions() != null) {
            session.sessions().migrate(VERSION.toString());
        }
        session.realms().getRealmsStream().forEach(realm -> migrateRealm(session, realm));
    }

    @Override
    public void migrateImport(KeycloakSession session, RealmModel realm, RealmRepresentation rep, boolean skipUserDependent) {
        migrateRealm(session, realm);
    }

    protected void migrateRealm(KeycloakSession session, RealmModel realm) {
        if (realm.getName().equals("master")) {
            LOG.infof("Skipping realm: %s", realm.getName());
            return;
        }

        // 🔁 Update 'forms' flow
        AuthenticationFlowModel formsFlow = realm.getFlowByAlias("forms");
        if (formsFlow != null) {
            List<AuthenticationExecutionModel> executions = realm.getAuthenticationExecutionsStream(formsFlow.getId()).toList();

            for (AuthenticationExecutionModel execution : executions) {
                if ("auth-username-password-form".equals(execution.getAuthenticator())) {
                    execution.setAuthenticator("custom-username-password-form");
                    realm.updateAuthenticatorExecution(execution);
                }
            }
        } else {
            LOG.warnf("forms doesn't exists for the flow", realm.getName());
        }
    }
}
