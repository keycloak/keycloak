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

import java.nio.file.Path;

import org.keycloak.it.junit5.extension.CLIResult;
import org.keycloak.it.junit5.extension.DistributionTest;
import org.keycloak.it.junit5.extension.RawDistOnly;
import org.keycloak.it.utils.KeycloakDistribution;
import org.keycloak.it.utils.RawKeycloakDistribution;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

@RawDistOnly(reason = "Containers are immutable")
@DistributionTest(defaultOptions = "--db=dev-file")
@Tag(DistributionTest.SMOKE)
public class ExportDistTest {

    @Test
    void testExport(KeycloakDistribution dist) {
        CLIResult cliResult = dist.run("build");

        cliResult = dist.run("export", "--realm=master", "--dir=.");
        cliResult.assertMessage("Export of realm 'master' requested.");
        cliResult.assertMessage("Export finished successfully");
        cliResult.assertNoMessage("Changes detected in configuration");
        cliResult.assertNoMessage("Listening on: http");

        cliResult = dist.run("export", "--realm=master");
        cliResult.assertError("Must specify either --dir or --file options.");

        cliResult = dist.run("export", "--file=master", "--users=skip");
        cliResult.assertError("Property '--users' can be used only when exporting to a directory, or value set to 'same_file' when exporting to a file.");

        cliResult = dist.run("export", "--file=some-file", "--users=same_file");
        cliResult.assertNoError("Property '--users' can be used only when exporting to a directory, or value set to 'same_file' when exporting to a file.");
        cliResult.assertMessage("Exporting model into file");

        cliResult = dist.run("export", "--dir=some-dir", "--users=skip");
        cliResult.assertMessage("Realm 'master' - data exported");

    }

    @Test
    void testExportRealmFGAPEnabled(KeycloakDistribution dist) {
        RawKeycloakDistribution rawDist = dist.unwrap(RawKeycloakDistribution.class);
        Path importDir = rawDist.getDistPath().resolve("data").resolve("import");
        assertTrue(importDir.toFile().mkdirs());
        dist.copyOrReplaceFileFromClasspath("/fgap-realm.json", importDir.resolve("fgap-realm.json"));
        rawDist.run("start-dev","-v", "--import-realm", "--features=admin-fine-grained-authz:v2");
        rawDist.stop();
        CLIResult cliResult = rawDist.run("export", "--realm=fgap", "--dir=" + importDir.toAbsolutePath(), "--features=admin-fine-grained-authz:v2");
        cliResult.assertMessage("Export of realm 'fgap' requested.");
        cliResult.assertMessage("Export finished successfully");
    }

    @Test
    void testExportNonExistent(KeycloakDistribution dist) {
        CLIResult cliResult = dist.run("build");

        cliResult = dist.run("export", "--realm=non-existent-realm", "--dir=.");
        cliResult.assertMessage("realm not found by realm name 'non-existent-realm'");
    }

}
