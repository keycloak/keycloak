/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.it.storage.database;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

import org.keycloak.it.junit5.extension.CLIResult;
import org.keycloak.it.junit5.extension.KeycloakRunner;
import org.keycloak.it.utils.RawDistRootPath;
import org.keycloak.it.utils.RawKeycloakDistribution;
import org.keycloak.quarkus.runtime.cli.command.AbstractAutoBuildCommand;

import io.quarkus.logging.Log;
import io.quarkus.test.junit.main.Launch;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static org.apache.commons.lang3.StringUtils.countMatches;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public abstract class BasicDatabaseTest {

    @Test
    @Launch({ "start", AbstractAutoBuildCommand.OPTIMIZED_BUILD_OPTION_LONG, "--http-enabled=true", "--hostname-strict=false" })
    protected void testSuccessful(CLIResult cliResult) {
        cliResult.assertStarted();
        cliResult.assertNoMessage("charset encoding");
    }

    @Test
    @Launch({ "start", AbstractAutoBuildCommand.OPTIMIZED_BUILD_OPTION_LONG, "--http-enabled=true", "--hostname-strict=false", "--db-username=wrong" })
    protected void testWrongUsername(CLIResult cliResult) {
        cliResult.assertMessage("ERROR: Failed to obtain JDBC connection");
        assertWrongUsername(cliResult);
    }

    protected abstract void assertWrongUsername(CLIResult cliResult);

    @Test
    @Launch({ "start", AbstractAutoBuildCommand.OPTIMIZED_BUILD_OPTION_LONG, "--http-enabled=true", "--hostname-strict=false", "--db-password=wrong" })
    protected void testWrongPassword(CLIResult cliResult) {
        cliResult.assertMessage("ERROR: Failed to obtain JDBC connection");
        assertWrongPassword(cliResult);
    }

    protected abstract void assertWrongPassword(CLIResult cliResult);

    @Order(1)
    @Test
    @Launch({ "export", AbstractAutoBuildCommand.OPTIMIZED_BUILD_OPTION_LONG, "--dir=./target/export"})
    public void testExportSucceeds(CLIResult cliResult) {
        cliResult.assertMessage("Full model export requested");
        cliResult.assertMessage("Export finished successfully");
    }

    @Order(2)
    @Test
    @Launch({ "import", AbstractAutoBuildCommand.OPTIMIZED_BUILD_OPTION_LONG, "--dir=./target/export" })
    void testImportSucceeds(CLIResult cliResult) {
        cliResult.assertMessage("target/export");
        cliResult.assertMessage("Realm 'master' imported");
        cliResult.assertMessage("Import finished successfully");
    }

    public String assertManualDbInitialization(CLIResult cliResult, RawDistRootPath rawDistRootPath) {
        cliResult.assertMessage("Database not initialized, please initialize database with");

        var output = readKeycloakDbUpdateScript(rawDistRootPath);

        assertThat(output, notNullValue());
        assertThat(output, containsString("Keycloak database creation script - apply this script to empty DB"));
        assertThat(output, containsString("Create Database Change Log Table"));

        // The script inserts into the changelog table, so it has to create it - once, or applying it fails
        var outputLowerCase = output.toLowerCase(Locale.ROOT);
        var count = countMatches(outputLowerCase, "create table public.databasechangelog (")
                + countMatches(outputLowerCase, "create table keycloak.databasechangelog (");
        assertThat(count, is(1));
        assertThat(output, containsString("Change Log: META-INF/jpa-changelog-master.xml"));
        assertThat(output, containsString("Changeset META-INF/jpa-changelog-26.2.6.xml"));

        return output;
    }

    /**
     * Applies the generated manual migration script to an empty database and checks that the result is the schema
     * of the automatic migration. Distribution tests call this from a {@code @Test}; it is not one itself because
     * it needs a {@link KeycloakRunner}, which only exists for {@code @DistributionTest} classes.
     */
    protected void assertManualScriptMatchesAutomaticMigration(KeycloakRunner runner, RawDistRootPath rawDistRootPath) throws Exception {
        RawKeycloakDistribution dist = runner.getDistribution(RawKeycloakDistribution.class);

        // the connection details the test framework wrote for the container
        Properties config = new Properties();
        try (Reader reader = Files.newBufferedReader(dist.getDistPath().resolve("conf").resolve("keycloak.conf"))) {
            config.load(reader);
        }

        ManualMigrationParity.Database database = new ManualMigrationParity.Database() {
            @Override
            public Connection open() throws Exception {
                return DriverManager.getConnection(config.getProperty("db-url"), config.getProperty("db-username"),
                        config.getProperty("db-password"));
            }

            @Override
            public void reset() throws Exception {
                try (Connection connection = open()) {
                    resetDatabase(connection);
                }
            }

            @Override
            public boolean supportsSequences() {
                return BasicDatabaseTest.this.supportsSequences();
            }
        };

        ManualMigrationParity.verify(runner, rawDistRootPath, database,
                List.of("start", AbstractAutoBuildCommand.OPTIMIZED_BUILD_OPTION_LONG, "--http-enabled=true", "--hostname-strict=false"),
                knownMissingFromScript(), knownExtraInScript());
    }

    /** Remove every object from the database the connection points at, leaving an empty database. */
    protected void resetDatabase(Connection connection) throws Exception {
        throw new UnsupportedOperationException("Manual migration parity is not set up for this database");
    }

    /** Snapshot entries (prefixes) known to exist after the automatic migration but not after the script. */
    protected List<String> knownMissingFromScript() {
        return ManualMigrationParity.KNOWN_MISSING_FROM_SCRIPT;
    }

    /** Snapshot entries (prefixes) known to exist after the script but not after the automatic migration. */
    protected List<String> knownExtraInScript() {
        return ManualMigrationParity.KNOWN_EXTRA_IN_SCRIPT;
    }

    protected boolean supportsSequences() {
        return true;
    }

    /**
     * Drop every table of the current catalog, for databases where the test user may not drop the database itself.
     */
    protected static void dropAllTables(Connection connection) throws Exception {
        List<String> tables = new ArrayList<>();
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(
                     "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_TYPE = 'BASE TABLE'")) {
            while (rs.next()) {
                tables.add(rs.getString(1));
            }
        }
        try (Statement statement = connection.createStatement()) {
            statement.execute("SET FOREIGN_KEY_CHECKS = 0");
            for (String table : tables) {
                statement.execute("DROP TABLE `" + table + "`");
            }
            statement.execute("SET FOREIGN_KEY_CHECKS = 1");
        }
    }

    protected static String readKeycloakDbUpdateScript(RawDistRootPath path) {
        final String defaultScriptName = "keycloak-database-update.sql";
        Path scriptPath = Paths.get(path.getDistRootPath() + File.separator + "bin" + File.separator + defaultScriptName);
        File script = new File(scriptPath.toString());
        assertThat(String.format("Script '%s' does not exist!", defaultScriptName), script.isFile(), is(true));

        try {
            var result = FileUtils.readFileToString(script, Charset.defaultCharset());
            Log.infof("Deleting Keycloak DB update script '%s'", defaultScriptName);
            Files.delete(scriptPath);
            return result;
        } catch (IOException e) {
            throw new AssertionError(String.format("Cannot read or delete script '%s'", defaultScriptName), e);
        }
    }
}
