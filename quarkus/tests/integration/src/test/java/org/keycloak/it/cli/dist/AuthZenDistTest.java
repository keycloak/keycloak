/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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

import org.keycloak.common.Profile;
import org.keycloak.it.junit5.extension.CLIResult;
import org.keycloak.it.junit5.extension.DistributionTest;
import org.keycloak.quarkus.runtime.cli.command.Start;
import org.keycloak.quarkus.runtime.cli.command.StartDev;

import io.quarkus.test.junit.main.Launch;
import org.junit.jupiter.api.Test;

@DistributionTest
public class AuthZenDistTest {

    private static final String AUTHZEN_HTTPS_ERROR = "The AuthZen feature requires HTTPS in order to be compliant with the OpenID AuthZen specification.";

    @Test
    @Launch({Start.NAME, "--db=dev-file", "--features=authzen", "--http-enabled=true", "--hostname-strict=false"})
    public void testHttpEnabledErrorInProductionMode(CLIResult cliResult) {
        cliResult.assertError(AUTHZEN_HTTPS_ERROR);
    }

    @Test
    @Launch({StartDev.NAME, "--features=authzen"})
    public void testNoHttpEnabledWarningInDevMode(CLIResult cliResult) {
        cliResult.assertStartedDevMode();
        cliResult.assertMessage(Profile.Feature.AUTHZEN.getVersionedKey());
        cliResult.assertNoError(AUTHZEN_HTTPS_ERROR);
    }
}
