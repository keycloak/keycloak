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

package org.keycloak.it.storage.database.dist;

import org.junit.jupiter.api.Test;
import org.keycloak.it.junit5.extension.CLIResult;
import org.keycloak.it.junit5.extension.DistributionTest;

import io.quarkus.test.junit.main.Launch;
import io.quarkus.test.junit.main.LaunchResult;

@DistributionTest
public class CustomTransactionDistTest {

    @Test
    @Launch({ "-Dkc.db-tx-type=enabled", "-Dkc.db-driver=org.postgresql.xa.PGXADataSource", "build", "--db=postgres" })
    void failNoXAUsingXADriver(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;
        cliResult.assertError("Driver org.postgresql.xa.PGXADataSource is an XA datasource, but XA transactions have not been enabled on the default datasource");
    }

    @Test
    @Launch({ "-Dkc.db-driver=com.microsoft.sqlserver.jdbc.SQLServerDriver", "build", "--db=mssql" })
    void failXAUsingNonXADriver(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;
        cliResult.assertError("Driver is not an XA dataSource, while XA has been enabled in the configuration of the default datasource");
    }

    @Test
    @Launch({ "-Dkc.db-tx-type=enabled", "-Dkc.db-driver=com.microsoft.sqlserver.jdbc.SQLServerDriver", "build", "--db=mssql" })
    void testNoXa(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;
        cliResult.assertBuild();
    }
}
