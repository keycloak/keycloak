/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.it.cli.dist;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.regex.Pattern;

import org.keycloak.it.junit5.extension.CLIResult;
import org.keycloak.it.junit5.extension.DistributionTest;
import org.keycloak.it.junit5.extension.KeycloakRunner;
import org.keycloak.it.junit5.extension.RawDistOnly;
import org.keycloak.it.utils.RawDistRootPath;
import org.keycloak.it.utils.RawKeycloakDistribution;

import io.quarkus.deployment.util.FileUtil;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.number.OrderingComparison.lessThan;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DistributionTest
@RawDistOnly(reason = "Containers are immutable")
@Tag(DistributionTest.SLOW)
public class ManualMigrationEmptyDatabaseDistTest {

    private static final String CREATE_CHANGELOG_TABLE = "CREATE TABLE PUBLIC.DATABASECHANGELOG (";
    private static final String INSERT_INTO_CHANGELOG = "INSERT INTO PUBLIC.DATABASECHANGELOG (";
    private static final String CREATE_LOCK_TABLE = "CREATE TABLE PUBLIC.DATABASECHANGELOGLOCK (";

    /**
     * The manual migration strategy exists for database users without DDL privileges. On an empty
     * database the generated script announces itself as a creation script to be applied to an empty
     * database, so it has to contain the DDL of the Liquibase changelog table it inserts into.
     */
    @Test
    void testEmptyDatabaseExportContainsChangelogTableDdl(KeycloakRunner runner, RawDistRootPath rawDistRootPath) throws IOException {
        // force a completely empty database
        RawKeycloakDistribution rawDist = runner.getDistribution(RawKeycloakDistribution.class);
        FileUtil.deleteDirectory(rawDist.getDistPath().resolve("data").resolve("h2").toAbsolutePath());

        CLIResult result = runner.run("start-dev",
                "--spi-connections-jpa-quarkus-migration-strategy=manual",
                "--spi-connections-jpa-quarkus-initialize-empty=false");

        result.assertMessage("Database not initialized, please initialize database with");

        File script = rawDistRootPath.getDistRootPath().resolve("bin").resolve("keycloak-database-update.sql").toFile();
        assertTrue(script.isFile(), "Export file must exist when migration-strategy=manual");

        String output = FileUtils.readFileToString(script, Charset.defaultCharset());

        assertThat(output, containsString("Keycloak database creation script - apply this script to empty DB"));

        // The script inserts into the changelog table, so it has to create it as well. Exactly once:
        // emitting it twice makes the script fail with "table already exists" instead.
        assertThat("changelog table must be created exactly once",
                countOccurrences(output, CREATE_CHANGELOG_TABLE), is(1));

        // ... and it has to be created before the first row is inserted into it.
        assertThat("changelog table must be created before it is inserted into",
                output.indexOf(CREATE_CHANGELOG_TABLE), lessThan(output.indexOf(INSERT_INTO_CHANGELOG)));

        // The server creates Liquibase's lock table itself on startup, which a database user without DDL privileges
        // is not allowed to do. A database populated only by this script must therefore get it from the script.
        assertThat("lock table must be created exactly once",
                countOccurrences(output, CREATE_LOCK_TABLE), is(1));

        // changeSetExecuted preconditions are resolved against the changelog table, so it must still exist while
        // the script is generated. Were it missing, they would all resolve to "not executed" and changesets that
        // should be marked as ran would be emitted instead - this one carries MySQL-only index syntax.
        assertThat(output, not(containsString("VALUE(255)")));
    }

    /**
     * An administrator may create both Liquibase bookkeeping tables up front, so that a database user without DDL
     * privileges can produce the script at all. The script must then not create them again: it is meant to be
     * applied to that same database, where the DDL would fail with "already exists". Producing the script once
     * leaves exactly that state behind, as the server creates both tables on the live connection while exporting.
     */
    @Test
    void testEmptyDatabaseExportSkipsPreCreatedBookkeepingTables(KeycloakRunner runner, RawDistRootPath rawDistRootPath) throws IOException {
        RawKeycloakDistribution rawDist = runner.getDistribution(RawKeycloakDistribution.class);
        FileUtil.deleteDirectory(rawDist.getDistPath().resolve("data").resolve("h2").toAbsolutePath());

        // First export on an empty database: creates the changelog and lock tables on the live connection
        runner.run("start-dev",
                "--spi-connections-jpa-quarkus-migration-strategy=manual",
                "--spi-connections-jpa-quarkus-initialize-empty=false")
                .assertMessage("Database not initialized, please initialize database with");

        // Second export against the same, still empty database, where both tables now pre-exist
        CLIResult result = runner.run("start-dev",
                "--spi-connections-jpa-quarkus-migration-strategy=manual",
                "--spi-connections-jpa-quarkus-initialize-empty=false");

        result.assertMessage("Database not initialized, please initialize database with");

        File script = rawDistRootPath.getDistRootPath().resolve("bin").resolve("keycloak-database-update.sql").toFile();
        String output = FileUtils.readFileToString(script, Charset.defaultCharset());

        assertThat(output, containsString(INSERT_INTO_CHANGELOG));
        assertThat("changelog table must not be created when it already existed",
                countOccurrences(output, CREATE_CHANGELOG_TABLE), is(0));
        assertThat("lock table must not be created when it already existed",
                countOccurrences(output, CREATE_LOCK_TABLE), is(0));
    }

    private static int countOccurrences(String haystack, String needle) {
        return haystack.split(Pattern.quote(needle), -1).length - 1;
    }
}
