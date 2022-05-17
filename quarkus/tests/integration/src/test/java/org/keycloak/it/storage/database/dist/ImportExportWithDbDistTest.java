package org.keycloak.it.storage.database.dist;

import io.quarkus.test.junit.main.Launch;
import io.quarkus.test.junit.main.LaunchResult;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.keycloak.it.junit5.extension.BeforeStartDistribution;
import org.keycloak.it.junit5.extension.CLIResult;
import org.keycloak.it.junit5.extension.DistributionTest;
import org.keycloak.it.junit5.extension.RawDistOnly;
import org.keycloak.it.utils.KeycloakDistribution;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.function.Consumer;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DistributionTest
@RawDistOnly(reason = "Containers are immutable")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ImportExportWithDbDistTest {

    static final String DB_NAME = "keycloak";
    static final String DB_PW = "keycloak";
    static final String DB_USER = "keycloak";
    static final int DB_EXPOSED_PORT = 5432;
    static final PostgreSQLContainer<?> POSTGRES_CONTAINER;

    static {
        POSTGRES_CONTAINER = new PostgreSQLContainer("postgres:alpine");
            POSTGRES_CONTAINER
                .withDatabaseName(DB_NAME)
                .withUsername(DB_USER)
                .withPassword(DB_PW)
                .withExposedPorts(DB_EXPOSED_PORT);
        POSTGRES_CONTAINER.start();
        assertTrue(POSTGRES_CONTAINER.isRunning());
    }

    @Test
    @Launch({ "build", "--db=postgres" })
    @Order(1)
    void testBuildWithRightDb(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;
        cliResult.assertBuild();
    }

    @Test
    @BeforeStartDistribution(UseContainerJdbcUrl.class)
    @Launch({ "show-config" })
    @Order(2)
    void testDbUsedFromPersistedConfig(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;
        assertThat(cliResult.getOutput(),containsString("postgres (PersistedConfigSource)"));
    }

    @Test
    @BeforeStartDistribution(UseContainerJdbcUrl.class)
    @Launch({ "start", "--http-enabled=true", "--hostname-strict=false", "--db-username=keycloak", "--db-password=keycloak" })
    @Order(2)
    void testStartUp(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;
        cliResult.assertStarted();
    }

    @Test
    @BeforeStartDistribution(SimulateDbCredentialsFromConfOrEnv.class)
    @Launch({ "export", "--dir=."})
    @Order(3)
    void testExportSucceeds(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;
        cliResult.assertMessage("Full model export requested");
        cliResult.assertMessage("Export finished successfully");
    }

    @Test
    @Launch({ "import", "--dir=."})
    @Order(4)
    void testImportSucceeds(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;
        cliResult.assertMessage("Realm 'master' already exists. Removing it before import");
        cliResult.assertMessage("Realm 'master' imported");
        cliResult.assertMessage("Import finished successfully");
    }

    public static class UseContainerJdbcUrl implements Consumer<KeycloakDistribution> {
        @Override
        public void accept(KeycloakDistribution distribution) {
            distribution.setProperty("db-url", POSTGRES_CONTAINER.getJdbcUrl());
        }
    }

    public static class SimulateDbCredentialsFromConfOrEnv implements Consumer<KeycloakDistribution> {
        @Override
        public void accept(KeycloakDistribution distribution) {
            distribution.setProperty("db-username", DB_USER);
            distribution.setProperty("db-password", DB_PW);
        }
    }
}
