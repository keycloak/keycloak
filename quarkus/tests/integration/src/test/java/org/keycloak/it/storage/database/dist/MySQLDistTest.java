package org.keycloak.it.storage.database.dist;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.function.Consumer;

import org.keycloak.it.junit5.extension.BeforeStartDistribution;
import org.keycloak.it.junit5.extension.CLIResult;
import org.keycloak.it.junit5.extension.DistributionTest;
import org.keycloak.it.junit5.extension.WithDatabase;
import org.keycloak.it.storage.database.MySQLTest;
import org.keycloak.it.utils.RawDistRootPath;
import org.keycloak.it.utils.RawKeycloakDistribution;
import org.keycloak.quarkus.runtime.cli.command.AbstractAutoBuildCommand;

import io.quarkus.test.junit.main.Launch;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

@DistributionTest
@WithDatabase(alias = "mysql")
public class MySQLDistTest extends MySQLTest {

    @Override
    @Tag(DistributionTest.STORAGE)
    @Test
    @Launch({ "start", AbstractAutoBuildCommand.OPTIMIZED_BUILD_OPTION_LONG, "--http-enabled=true", "--hostname-strict=false" })
    protected void testSuccessful(CLIResult result) {
        super.testSuccessful(result);
    }

    @Tag(DistributionTest.STORAGE)
    @Test
    @Launch({"start", AbstractAutoBuildCommand.OPTIMIZED_BUILD_OPTION_LONG, "--spi-connections-jpa-quarkus-migration-strategy=manual", "--spi-connections-jpa-quarkus-initialize-empty=false", "--http-enabled=true", "--hostname-strict=false",})
    public void testKeycloakDbUpdateScript(CLIResult cliResult, RawDistRootPath rawDistRootPath) {
        String output = assertManualDbInitialization(cliResult, rawDistRootPath);

        // Verify MySQL primary key is included
        assertThat(output, containsString("Add primary key to DATABASECHANGELOG table for MySQL"));
        assertThat(output, containsString("ALTER TABLE keycloak.DATABASECHANGELOG ADD PRIMARY KEY (ID, AUTHOR, FILENAME);"));
    }

    @Tag(DistributionTest.STORAGE)
    @Test
    @Launch({"start", AbstractAutoBuildCommand.OPTIMIZED_BUILD_OPTION_LONG, "--http-enabled=true", "--hostname-strict=false", "--db-pool-max-lifetime=28800"})
    public void testWarningForTooShortLifetime(CLIResult cliResult) {
        cliResult.assertMessage("set 'db-pool-max-lifetime' to a duration smaller than '28800' seconds.");
    }

    @Tag(DistributionTest.STORAGE)
    @Test
    @BeforeStartDistribution(EnableInvisiblePrimaryKeys.class)
    @Launch({"start", AbstractAutoBuildCommand.OPTIMIZED_BUILD_OPTION_LONG, "--http-enabled=true", "--hostname-strict=false"})
    public void testErrorForInvisiblePrimaryKeys(CLIResult cliResult) {
        cliResult.assertMessage("'sql_generate_invisible_primary_key' is set to 'ON'");
    }

    public static class EnableInvisiblePrimaryKeys implements Consumer<RawKeycloakDistribution> {

        @Override
        public void accept(RawKeycloakDistribution distribution) {
            Properties conf = new Properties();
            try (Reader reader = Files.newBufferedReader(distribution.getDistPath().resolve("conf").resolve("keycloak.conf"))) {
                conf.load(reader);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            // the MySQL test container uses the same password for root as for the Keycloak user
            try (Connection connection = DriverManager.getConnection(conf.getProperty("db-url"), "root", conf.getProperty("db-password"));
                 Statement statement = connection.createStatement()) {
                statement.execute("SET GLOBAL sql_generate_invisible_primary_key = ON");
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
