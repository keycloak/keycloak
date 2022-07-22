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

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.keycloak.it.junit5.extension.CLIResult;
import org.keycloak.quarkus.runtime.cli.command.AbstractStartCommand;

import io.quarkus.test.junit.main.Launch;
import io.quarkus.test.junit.main.LaunchResult;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public abstract class BasicDatabaseTest {

    @Test
    @Launch({ "start", AbstractStartCommand.OPTIMIZED_BUILD_OPTION_LONG, "--http-enabled=true", "--hostname-strict=false" })
    void testSuccessful(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;
        cliResult.assertStarted();
    }

    @Test
    @Launch({ "start", AbstractStartCommand.OPTIMIZED_BUILD_OPTION_LONG,"--http-enabled=true", "--hostname-strict=false", "--db-username=wrong" })
    void testWrongUsername(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;
        cliResult.assertMessage("ERROR: Failed to obtain JDBC connection");
        assertWrongUsername(cliResult);
    }

    protected abstract void assertWrongUsername(CLIResult cliResult);

    @Test
    @Launch({ "start", AbstractStartCommand.OPTIMIZED_BUILD_OPTION_LONG,"--http-enabled=true", "--hostname-strict=false", "--db-password=wrong" })
    void testWrongPassword(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;
        cliResult.assertMessage("ERROR: Failed to obtain JDBC connection");
        assertWrongPassword(cliResult);
    }

    protected abstract void assertWrongPassword(CLIResult cliResult);

    @Order(1)
    @Test
    @Launch({ "export", "--dir=./target/export"})
    public void testExportSucceeds(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;
        cliResult.assertMessage("Full model export requested");
        cliResult.assertMessage("Export finished successfully");
    }

    @Order(2)
    @Test
    @Launch({ "import", "--dir=./target/export" })
    void testImportSucceeds(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;
        cliResult.assertMessage("target/export");
        cliResult.assertMessage("Realm 'master' imported");
        cliResult.assertMessage("Import finished successfully");
    }
}
