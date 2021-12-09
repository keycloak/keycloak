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

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.condition.DisabledIf;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.keycloak.it.junit5.extension.CLIResult;
import org.keycloak.it.junit5.extension.DistributionTest;
import org.keycloak.it.junit5.extension.RawDistOnly;

import io.quarkus.test.junit.main.Launch;
import io.quarkus.test.junit.main.LaunchResult;

@DistributionTest(reInstall = DistributionTest.ReInstall.NEVER)
@RawDistOnly(reason = "Containers are immutable")
@TestMethodOrder(OrderAnnotation.class)
public class BuildAndStartDistTest {

    @Test
    @Launch({ "build", "--http-enabled=true", "--hostname-strict=false", "--cache=local" })
    @Order(1)
    void firstYouBuild(LaunchResult result) {
    }

    @Test
    @Launch({ "start" })
    @Order(2)
    void thenYouStart(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;
        cliResult.assertStarted();
    }
}
