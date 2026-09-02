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

import org.keycloak.it.junit5.extension.CLIResult;
import org.keycloak.it.junit5.extension.DistributionTest;
import org.keycloak.it.junit5.extension.StopServer.Mode;
import org.keycloak.it.junit5.extension.WithExternalInfinispan;

import io.quarkus.test.junit.main.Launch;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@DistributionTest(stopServer = Mode.MANUAL)
@WithExternalInfinispan
@Tag(DistributionTest.STORAGE)
public class ExternalInfinispanTest {

    @Test
    @Launch({
            "start-dev",
            "--cache=ispn",
            "-Djboss.site.name=ISPN",
            "--verbose"
    })
    void testSiteNameAsSystemProperty(CLIResult cliResult) {
        cliResult.assertMessage("System property jboss.site.name is in use. Use --spi-cache-embedded-default-site-name config option instead");
    }

    @Test
    @Launch({
            "start-dev",
            "--cache=ispn",
            "-Djboss.node.name=ISPN",
            "--verbose"
    })
    void testNodeNameAsSystemProperty(CLIResult cliResult) {
        cliResult.assertMessage("System property jboss.node.name is in use. Use --spi-cache-embedded-default-node-name config option instead");
    }
}
