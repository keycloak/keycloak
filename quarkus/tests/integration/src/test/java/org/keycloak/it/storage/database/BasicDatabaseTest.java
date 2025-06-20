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

import io.quarkus.logging.Log;
import io.quarkus.test.junit.main.Launch;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.keycloak.it.junit5.extension.CLIResult;
import org.keycloak.it.utils.RawDistRootPath;
import org.keycloak.quarkus.runtime.cli.command.AbstractStartCommand;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.apache.commons.lang3.StringUtils.countMatches;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public abstract class BasicDatabaseTest {

    @Test
    @Launch({ "start", AbstractStartCommand.OPTIMIZED_BUILD_OPTION_LONG, "--http-enabled=true", "--hostname-strict=false" })
    protected void testSuccessful(CLIResult cliResult) {
        cliResult.assertStarted();
    }

    @Test
    @Launch({ "start", AbstractStartCommand.OPTIMIZED_BUILD_OPTION_LONG, "--http-enabled=true", "--hostname-strict=false", "--db-username=wrong" })
    protected void testWrongUsername(CLIResult cliResult) {
        cliResult.assertMessage("ERROR: Failed to obtain JDBC connection");
        assertWrongUsername(cliResult);
    }

    protected abstract void assertWrongUsername(CLIResult cliResult);

    @Test
    @Launch({ "start", AbstractStartCommand.OPTIMIZED_BUILD_OPTION_LONG, "--http-enabled=true", "--hostname-strict=false", "--db-password=wrong" })
    protected void testWrongPassword(CLIResult cliResult) {
        cliResult.assertMessage("ERROR: Failed to obtain JDBC connection");
        assertWrongPassword(cliResult);
    }

    protected abstract void assertWrongPassword(CLIResult cliResult);

    @Order(1)
    @Test
    @Launch({ "export", AbstractStartCommand.OPTIMIZED_BUILD_OPTION_LONG, "--dir=./target/export"})
    public void testExportSucceeds(CLIResult cliResult) {
        cliResult.assertMessage("Full model export requested");
        cliResult.assertMessage("Export finished successfully");
    }

    @Order(2)
    @Test
    @Launch({ "import", AbstractStartCommand.OPTIMIZED_BUILD_OPTION_LONG, "--dir=./target/export" })
    void testImportSucceeds(CLIResult cliResult) {
        cliResult.assertMessage("target/export");
        cliResult.assertMessage("Realm 'master' imported");
        cliResult.assertMessage("Import finished successfully");
    }

    public void assertManualDbInitialization(CLIResult cliResult, RawDistRootPath rawDistRootPath) {
        cliResult.assertMessage("Database not initialized, please initialize database with");

        var output = readKeycloakDbUpdateScript(rawDistRootPath);

        assertThat(output, notNullValue());
        assertThat(output, containsString("Create Database Change Log Table"));

        var outputLowerCase = output.toLowerCase();
        var count = countMatches(outputLowerCase, "create table public.databasechangelog") + countMatches(outputLowerCase, "create table keycloak.databasechangelog");
        assertThat(count, is(1));
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
