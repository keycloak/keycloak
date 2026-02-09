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

import io.quarkus.test.junit.main.Launch;
import io.quarkus.test.junit.main.LaunchResult;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;

@DistributionTest
@RawDistOnly(reason = "Containers are immutable")
public class HostnameV2DistTest {
    @Test
    @Launch({"start", "--db=dev-file", "--http-enabled=true"})
    public void testServerFailsToStartWithoutHostnameSpecified(LaunchResult result) {
        assertThat(result.getErrorOutput(), containsString("ERROR: hostname is not configured; either configure hostname, or set hostname-strict to false"));
    }

    @Test
    @Launch({"start-dev"})
    public void testServerStartsDevtWithoutHostnameSpecified(LaunchResult result) {
        ((CLIResult) result).assertStartedDevMode();
    }
}