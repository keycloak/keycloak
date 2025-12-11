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

import java.util.function.Consumer;

import org.keycloak.it.junit5.extension.BeforeStartDistribution;
import org.keycloak.it.junit5.extension.CLIResult;
import org.keycloak.it.junit5.extension.CLITest;
import org.keycloak.it.junit5.extension.WithDatabase;
import org.keycloak.it.utils.KeycloakDistribution;
import org.keycloak.it.utils.RawKeycloakDistribution;

@CLITest
@WithDatabase(alias = "oracle")
@BeforeStartDistribution(OracleTest.CopyOracleJdbcDriver.class)
public class OracleTest extends BasicDatabaseTest {

    @Override
    protected void assertWrongUsername(CLIResult cliResult) {
        cliResult.assertMessage("ORA-01017: invalid username/password; logon denied");
    }

    @Override
    protected void assertWrongPassword(CLIResult cliResult) {
        cliResult.assertMessage("ORA-01017: invalid username/password; logon denied");
    }

    public static class CopyOracleJdbcDriver implements Consumer<KeycloakDistribution> {

        @Override
        public void accept(KeycloakDistribution distribution) {
            RawKeycloakDistribution rawDist = distribution.unwrap(RawKeycloakDistribution.class);
            rawDist.copyProvider("com.oracle.database.jdbc", "ojdbc17");
            rawDist.copyProvider("com.oracle.database.nls", "orai18n");
        }
    }
}
