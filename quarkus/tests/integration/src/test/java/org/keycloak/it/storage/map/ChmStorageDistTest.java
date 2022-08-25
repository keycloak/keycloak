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

package org.keycloak.it.storage.map;

import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.keycloak.it.junit5.extension.CLIResult;
import org.keycloak.it.junit5.extension.DistributionTest;
import org.keycloak.it.junit5.extension.RawDistOnly;
import org.keycloak.it.utils.RawDistRootPath;

import io.quarkus.test.junit.main.Launch;
import io.quarkus.test.junit.main.LaunchResult;

@RawDistOnly(reason = "Need to check dist path")
@DistributionTest(reInstall = DistributionTest.ReInstall.BEFORE_TEST)
public class ChmStorageDistTest {

    @Test
    @Launch({ "start", "--http-enabled=true", "--hostname-strict=false", "--storage=chm" })
    void testStartUsingChmsStorage(LaunchResult result, RawDistRootPath distPath) {
        CLIResult cliResult = (CLIResult) result;
        assertExpectedMessages(cliResult, distPath);
        cliResult.assertStarted();
    }

    @Test
    @Launch({ "start-dev", "--storage=chm" })
    void testStartDevUsingChmsStorage(LaunchResult result, RawDistRootPath distPath) {
        CLIResult cliResult = (CLIResult) result;
        assertExpectedMessages(cliResult, distPath);
        cliResult.assertStartedDevMode();
    }

    private void assertExpectedMessages(CLIResult cliResult, RawDistRootPath distPath) {
        cliResult.assertMessage("Experimental feature enabled: map_storage");
        cliResult.assertMessage("Hibernate ORM is disabled because no JPA entities were found");
        Assert.assertFalse(distPath.getDistRootPath().resolve("data").resolve("h2").toFile().exists());
    }
}
