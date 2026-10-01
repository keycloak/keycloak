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

package org.keycloak.it.cli.dist;

import org.keycloak.it.junit5.extension.CLIResult;
import org.keycloak.it.junit5.extension.DistributionTest;
import org.keycloak.it.junit5.extension.RawDistOnly;
import org.keycloak.it.junit5.extension.StopServer;
import org.keycloak.it.junit5.extension.StopServer.Mode;
import org.keycloak.it.junit5.extension.WithEnvVars;

import io.quarkus.test.junit.main.Launch;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static org.junit.jupiter.api.Assertions.assertTrue;

@DistributionTest
@WithEnvVars({"KC_AUTO_BUILD", "false"})
@RawDistOnly(reason = "Containers are immutable")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class StartAutoBuildDisabledDistTest {
    
    @StopServer(Mode.BEFORE_QUARKUS)
    @Test
    @Launch({ "start", "--db=dev-file", "--http-enabled=true", "--hostname-strict=false" })
    @Order(-1)
    void initialStartFails(CLIResult cliResult) {
        cliResult.assertError("The 'auto-build' option was disabled for first ever server start. Please don't use this for the first startup or use 'kc.sh build' to build the server first.");
    }
    
    @StopServer(Mode.BEFORE_QUARKUS)
    @Test
    @Launch({ "build", "--db=dev-file" })
    @Order(0)
    void build(CLIResult cliResult) {
        // setting KC_AUTO_BUILD has no effect for the build command
        cliResult.assertBuild();
    }
    
    @StopServer(Mode.BEFORE_QUARKUS)
    @Test
    @Launch({ "start", "--db=dev-file", "--http-enabled=true", "--hostname-strict=false" })
    @Order(1)
    void testShouldNotReAugIfConfigIsSame(CLIResult cliResult) {
        cliResult.assertNoBuild();
        assertTrue(cliResult.getErrorOutput().isBlank());
    }

    @StopServer(Mode.BEFORE_QUARKUS)
    @Test
    @Launch({ "start", "--db=dev-mem", "--http-enabled=true", "--hostname-strict=false" })
    @Order(2)
    void testShouldErrorIfConfigChanged(CLIResult cliResult) {
        cliResult.assertError("The following build time options have values that differ from what is persisted - the new values will NOT be used until another build is run: kc.db");
    }

    @StopServer(Mode.BEFORE_QUARKUS)
    @Test
    @Launch({ "build", "--db=postgres" })
    @Order(3)
    void testBuildForReAugWhenAutoBuildDisabled(CLIResult cliResult) {
        cliResult.assertBuild();
    }

    @StopServer(Mode.BEFORE_QUARKUS)
    @Test
    @Launch({ "start", "--db=postgres", "--http-enabled=true", "--hostname-strict=false" })
    @Order(4)
    void testReuseBuildAfterDatabaseChange(CLIResult cliResult) {
        cliResult.assertNoBuild();
        assertTrue(cliResult.getErrorOutput().isBlank());
    }

    @StopServer(Mode.BEFORE_QUARKUS)
    @Test
    @Launch({ "start-dev" })
    @Order(5)
    void testStartDevAllowsAutoBuild(CLIResult cliResult) {
        cliResult.assertMessage("Updating the configuration and installing your custom providers, if any. Please wait.");
        cliResult.assertStartedDevMode();
    }

    @StopServer(Mode.BEFORE_QUARKUS)
    @Test
    @Launch({ "start-dev" })
    @Order(6)
    void testShouldNotReAugStartDevIfConfigIsSame(CLIResult cliResult) {
        cliResult.assertNoMessage("Updating the configuration and installing your custom providers, if any. Please wait.");
        cliResult.assertNoBuild();
        cliResult.assertStartedDevMode();
    }

}
