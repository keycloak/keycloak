package org.keycloak.tests.db;

import org.keycloak.config.DatabaseOptions;
import org.keycloak.quarkus.runtime.configuration.Configuration;
import org.keycloak.quarkus.runtime.configuration.mappers.DatabasePropertyMappers;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.tests.suites.DatabaseTest;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@KeycloakIntegrationTest
@DatabaseTest
public class DbTest {

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    @Test
    public void ensurePostgreSQLSettingsAreApplied() {
        runOnServer.run(session -> {
            if (Configuration.getConfigValue(DatabaseOptions.DB).getValue().equals("postgres") &&
                Configuration.getConfigValue(DatabaseOptions.DB_DRIVER).getValue().equals("org.postgresql.Driver")) {
                Assertions.assertEquals("primary", Configuration.getConfigValue(DatabasePropertyMappers.PG_TARGET_SERVER_TYPE).getValue());
            } else {
                Assertions.assertNull(Configuration.getConfigValue(DatabasePropertyMappers.PG_TARGET_SERVER_TYPE).getValue());
            }
        });
    }

}
