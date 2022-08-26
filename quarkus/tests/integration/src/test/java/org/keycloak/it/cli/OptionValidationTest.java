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

package org.keycloak.it.cli;

import org.junit.jupiter.api.Test;
import org.keycloak.it.junit5.extension.CLIResult;
import org.keycloak.it.junit5.extension.CLITest;

import io.quarkus.test.junit.main.Launch;
import io.quarkus.test.junit.main.LaunchResult;
import org.keycloak.it.utils.KeycloakDistribution;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;

@CLITest
public class OptionValidationTest {

    @Test
    @Launch({"build", "--db"})
    public void failMissingOptionValue(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;
        assertThat(cliResult.getErrorOutput(), containsString("Missing required value for option '--db' (vendor). Expected values are: dev-file, dev-mem, mariadb, mssql, mysql, oracle, postgres"));
    }

    @Test
    @Launch({"build", "--db", "foo", "bar"})
    public void failMultipleOptionValue(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;
        assertThat(cliResult.getErrorOutput(), containsString("Option '--db' expects a single value (vendor) Expected values are: dev-file, dev-mem, mariadb, mssql, mysql, oracle, postgres"));
    }

    @Test
    @Launch({"build", "--nosuch"})
    public void failUnknownOption(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;
        assertEquals("Unknown option: '--nosuch'\n" +
                "Try '" + KeycloakDistribution.SCRIPT_CMD + " build --help' for more information on the available options.", cliResult.getErrorOutput());
    }

    @Test
    @Launch({"start", "--db-pasword mytestpw"})
    public void failUnknownOptionWhitespaceSeparatorNotShowingValue(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;
        assertEquals("Unknown option: '--db-pasword'\n" +
                "Possible solutions: --db-url, --db-url-host, --db-url-database, --db-url-port, --db-url-properties, --db-username, --db-password, --db-schema, --db-pool-initial-size, --db-pool-min-size, --db-pool-max-size, --db\n" +
                "Try '" + KeycloakDistribution.SCRIPT_CMD + " start --help' for more information on the available options.", cliResult.getErrorOutput());
    }

    @Test
    @Launch({"start", "--db-pasword=mytestpw"})
    public void failUnknownOptionEqualsSeparatorNotShowingValue(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;
        assertEquals("Unknown option: '--db-pasword'\n" +
                "Possible solutions: --db-url, --db-url-host, --db-url-database, --db-url-port, --db-url-properties, --db-username, --db-password, --db-schema, --db-pool-initial-size, --db-pool-min-size, --db-pool-max-size, --db\n" +
                "Try '" + KeycloakDistribution.SCRIPT_CMD + " start --help' for more information on the available options.", cliResult.getErrorOutput());
    }

    @Test
    @Launch({"start", "--db-username=foobar","--db-pasword=mytestpw", "--foobar=barfoo"})
    public void failWithFirstOptionOnMultipleUnknownOptions(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;
        assertEquals("Unknown option: '--db-pasword'\n" +
                "Possible solutions: --db-url, --db-url-host, --db-url-database, --db-url-port, --db-url-properties, --db-username, --db-password, --db-schema, --db-pool-initial-size, --db-pool-min-size, --db-pool-max-size, --db\n" +
                "Try '" + KeycloakDistribution.SCRIPT_CMD + " start --help' for more information on the available options.", cliResult.getErrorOutput());
    }
}
