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

import org.keycloak.it.junit5.extension.CLIResult;
import org.keycloak.it.junit5.extension.DistributionTest;
import org.keycloak.it.junit5.extension.WithDatabase;
import org.keycloak.it.storage.database.MariaDBTest;
import org.keycloak.it.utils.RawDistRootPath;
import org.keycloak.quarkus.runtime.cli.command.AbstractAutoBuildCommand;

import io.quarkus.test.junit.main.Launch;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@DistributionTest(removeBuildOptionsAfterBuild = true)
@WithDatabase(alias = "mariadb")
public class MariaDBDistTest extends MariaDBTest {

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
        assertManualDbInitialization(cliResult, rawDistRootPath);
    }

    @Tag(DistributionTest.STORAGE)
    @Test
    @Launch({"start", AbstractAutoBuildCommand.OPTIMIZED_BUILD_OPTION_LONG, "--http-enabled=true", "--hostname-strict=false", "--db-pool-max-lifetime=28800"})
    public void testWarningForTooShortLifetime(CLIResult cliResult) {
        cliResult.assertMessage("set 'db-pool-max-lifetime' to a duration smaller than '28800' seconds.");
    }

}
