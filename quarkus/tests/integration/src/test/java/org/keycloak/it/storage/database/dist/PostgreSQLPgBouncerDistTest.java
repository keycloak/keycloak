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

package org.keycloak.it.storage.database.dist;

import org.keycloak.it.junit5.extension.CLIResult;
import org.keycloak.it.junit5.extension.DistributionTest;
import org.keycloak.it.junit5.extension.WithDatabase;
import org.keycloak.quarkus.runtime.cli.command.AbstractAutoBuildCommand;

import io.quarkus.test.junit.main.Launch;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Runs against PostgreSQL behind PgBouncer in transaction mode, which doesn't support prepared statements.
 */
@DistributionTest
@WithDatabase(alias = "postgresql-pgbouncer-transaction-no-prepared-statements", buildOptions = "db=postgres")
@Tag(DistributionTest.STORAGE)
public class PostgreSQLPgBouncerDistTest {

    @Test
    @Launch({ "start", AbstractAutoBuildCommand.OPTIMIZED_BUILD_OPTION_LONG, "--http-enabled=true", "--hostname-strict=false" })
    void testStartupFailsWithoutPreparedStatementSupport(CLIResult cliResult) {
        cliResult.assertMessage("The database connection doesn't support server-side prepared statements");
        cliResult.assertNoMessage("started in");
    }

    @Test
    @Launch({ "start", AbstractAutoBuildCommand.OPTIMIZED_BUILD_OPTION_LONG, "--http-enabled=true", "--hostname-strict=false", "--db-url-properties=?prepareThreshold=0" })
    void testStartupSucceedsWithoutServerSidePreparedStatements(CLIResult cliResult) {
        cliResult.assertStarted();
    }
}
