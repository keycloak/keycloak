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
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;
import java.util.regex.Pattern;

import org.keycloak.it.junit5.extension.CLIResult;
import org.keycloak.it.junit5.extension.DistributionTest;
import org.keycloak.it.junit5.extension.KeycloakRunner;
import org.keycloak.it.junit5.extension.RawDistOnly;
import org.keycloak.it.storage.database.ManualMigrationParity;
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
import static org.hamcrest.number.OrderingComparison.greaterThan;
import static org.hamcrest.number.OrderingComparison.lessThan;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DistributionTest
@RawDistOnly(reason = "Containers are immutable")
@Tag(DistributionTest.SLOW)
public class ManualMigrationEmptyDatabaseDistTest {

    private static final String CREATE_CHANGELOG_TABLE = "CREATE TABLE PUBLIC.DATABASECHANGELOG (";
    private static final String INSERT_INTO_CHANGELOG = "INSERT INTO PUBLIC.DATABASECHANGELOG (";

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

        // changeSetExecuted preconditions are resolved against the changelog table, so it must still exist while
        // the script is generated. Were it missing, they would all resolve to "not executed" and changesets that
        // should be marked as ran would be emitted instead - this one carries MySQL-only index syntax.
        assertThat(output, not(containsString("VALUE(255)")));
    }


    /**
     * Some changesets replace an index by dropping it and creating one of the same name. The script is generated
     * against an empty database, where the index to drop does not exist yet because an earlier changeset of the
     * same script creates it. The drop must therefore be emitted regardless, otherwise creating the replacement
     * fails with "already exists" when the script is applied.
     */
    @Test
    void testEmptyDatabaseExportDropsReplacedIndexes(KeycloakRunner runner, RawDistRootPath rawDistRootPath) throws IOException {
        RawKeycloakDistribution rawDist = runner.getDistribution(RawKeycloakDistribution.class);
        FileUtil.deleteDirectory(rawDist.getDistPath().resolve("data").resolve("h2").toAbsolutePath());

        runner.run("start-dev",
                "--spi-connections-jpa-quarkus-migration-strategy=manual",
                "--spi-connections-jpa-quarkus-initialize-empty=false")
                .assertMessage("Database not initialized, please initialize database with");

        File script = rawDistRootPath.getDistRootPath().resolve("bin").resolve("keycloak-database-update.sql").toFile();
        String output = FileUtils.readFileToString(script, Charset.defaultCharset());

        for (String index : new String[] { "IDX_USER_SESSION_EXPIRATION_CREATED", "IDX_USER_SESSION_EXPIRATION_LAST_REFRESH", "IDX_ROOT_AUTH_SESSION_REALM" }) {
            int firstCreate = output.indexOf("CREATE INDEX PUBLIC." + index);
            int drop = output.indexOf("DROP INDEX PUBLIC." + index);
            int lastCreate = output.lastIndexOf("CREATE INDEX PUBLIC." + index);

            assertThat(index + " must be created by an earlier changeset", firstCreate, greaterThan(-1));
            assertThat(index + " must be dropped after it is first created", drop, greaterThan(firstCreate));
            assertThat(index + " must be re-created after it is dropped", lastCreate, greaterThan(drop));
        }
    }

    /**
     * Applies the generated script to an empty database and compares the result with what the automatic migration
     * creates, which is what the string assertions above cannot show. The server must also accept the result as an
     * up-to-date database.
     */
    @Test
    void testScriptProducesTheSchemaOfTheAutomaticMigration(KeycloakRunner runner, RawDistRootPath rawDistRootPath) throws Exception {
        RawKeycloakDistribution rawDist = runner.getDistribution(RawKeycloakDistribution.class);
        Path h2Dir = rawDist.getDistPath().resolve("data").resolve("h2").toAbsolutePath();
        String url = "jdbc:h2:file:" + h2Dir.resolve("keycloakdb") + ";NON_KEYWORDS=VALUE";

        ManualMigrationParity.Database database = new ManualMigrationParity.Database() {
            @Override
            public Connection open() throws Exception {
                return DriverManager.getConnection(url, "", "");
            }

            @Override
            public void reset() throws Exception {
                FileUtil.deleteDirectory(h2Dir);
            }
        };

        ManualMigrationParity.verify(runner, rawDistRootPath, database, List.of("start-dev"),
                ManualMigrationParity.KNOWN_MISSING_FROM_SCRIPT, ManualMigrationParity.KNOWN_EXTRA_IN_SCRIPT);
    }

    private static int countOccurrences(String haystack, String needle) {
        return haystack.split(Pattern.quote(needle), -1).length - 1;
    }
}
