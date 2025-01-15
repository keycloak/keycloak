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

import java.io.File;
import java.io.IOException;
import java.util.Map;

import io.quarkus.test.junit.main.Launch;
import org.junit.jupiter.api.Test;
import org.keycloak.common.Version;
import org.keycloak.it.junit5.extension.CLIResult;
import org.keycloak.it.junit5.extension.DistributionTest;
import org.keycloak.it.junit5.extension.RawDistOnly;
import org.keycloak.it.utils.KeycloakDistribution;
import org.keycloak.quarkus.runtime.cli.command.UpdateCompatibility;
import org.keycloak.quarkus.runtime.cli.command.UpdateCompatibilityCheck;
import org.keycloak.quarkus.runtime.cli.command.UpdateCompatibilitySave;
import org.keycloak.quarkus.runtime.compatibility.CompatibilityManagerImpl;
import org.keycloak.quarkus.runtime.compatibility.ServerInfo;
import org.keycloak.util.JsonSerialization;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DistributionTest
@RawDistOnly(reason = "Requires creating JSON file to be available between containers")
public class UpdateCommandDistTest {

    @Test
    @Launch({UpdateCompatibility.NAME})
    public void testMissingSubCommand(CLIResult cliResult) {
        cliResult.assertError("Missing required subcommand");
    }

    @Test
    @Launch({UpdateCompatibility.NAME, UpdateCompatibilitySave.NAME})
    public void testMissingOptionOnSave(CLIResult cliResult) {
        cliResult.assertError("Missing required argument: --output");
    }

    @Test
    @Launch({UpdateCompatibility.NAME, UpdateCompatibilityCheck.NAME})
    public void testMissingOptionOnCheck(CLIResult cliResult) {
        cliResult.assertError("Missing required argument: --input");
    }

    @Test
    public void testCompatible(KeycloakDistribution distribution) throws IOException {
        var jsonFile = createTempFile("compatible");
        var result = distribution.run(UpdateCompatibility.NAME, UpdateCompatibilitySave.NAME, "--output", jsonFile.getAbsolutePath());
        result.assertMessage("Metadata successfully written.");
        assertEquals(0, result.exitCode());

        var info = JsonSerialization.mapper.readValue(jsonFile, ServerInfo.class);
        assertEquals(CompatibilityManagerImpl.EPOCH, info.getEpoch());
        assertEquals(Version.VERSION, info.getVersions().get("keycloak"));
        assertEquals(org.infinispan.commons.util.Version.getVersion(), info.getVersions().get("infinispan"));

        result = distribution.run(UpdateCompatibility.NAME, UpdateCompatibilityCheck.NAME, "--input", jsonFile.getAbsolutePath());
        result.assertMessage("Metadata successfully read.");
        result.assertNoError("incompatible");
    }

    @Test
    public void testWrongEpoch(KeycloakDistribution distribution) throws IOException {
        var jsonFile = createTempFile("wrong-epoch");
        var info = new ServerInfo();
        info.setEpoch(-1);

        JsonSerialization.mapper.writeValue(jsonFile, info);
        var result = distribution.run(UpdateCompatibility.NAME, UpdateCompatibilityCheck.NAME, "--input", jsonFile.getAbsolutePath());
        result.assertMessage("Metadata successfully read.");
        result.assertError("[Epoch] Epoch is incompatible: Old=-1, New=%s".formatted(CompatibilityManagerImpl.EPOCH));
    }

    @Test
    public void testWrongVersions(KeycloakDistribution distribution) throws IOException {
        var jsonFile = createTempFile("wrong-versions");

        // incompatible keycloak version
        var info = new ServerInfo();
        info.setEpoch(CompatibilityManagerImpl.EPOCH);
        info.setVersions(Map.of("keycloak", "0.0.0.Final",
                "infinispan", org.infinispan.commons.util.Version.getVersion()));
        JsonSerialization.mapper.writeValue(jsonFile, info);

        var result = distribution.run(UpdateCompatibility.NAME, UpdateCompatibilityCheck.NAME, "--input", jsonFile.getAbsolutePath());
        result.assertMessage("Metadata successfully read.");
        result.assertError("[Versions] keycloak is incompatible: Old=0.0.0.Final, New=%s".formatted(Version.VERSION));

        // incompatible infinispan version
        info.setVersions(Map.of(
                "keycloak", Version.VERSION,
                "infinispan", "0.0.0.Final"));
        JsonSerialization.mapper.writeValue(jsonFile, info);

        result = distribution.run(UpdateCompatibility.NAME, UpdateCompatibilityCheck.NAME, "--input", jsonFile.getAbsolutePath());
        result.assertMessage("Metadata successfully read.");
        result.assertError("[Versions] infinispan is incompatible: Old=0.0.0.Final, New=%s".formatted(org.infinispan.commons.util.Version.getVersion()));
    }

    private static File createTempFile(String prefix) throws IOException {
        var file = File.createTempFile(prefix, ".json");
        file.deleteOnExit();
        return file;
    }

}
