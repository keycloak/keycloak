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
import org.keycloak.quarkus.runtime.cli.command.UpdateCompatibilityMetadata;
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
    @Launch({UpdateCompatibility.NAME, UpdateCompatibilityMetadata.NAME})
    public void testMissingOptionOnSave(CLIResult cliResult) {
        cliResult.assertNoMessage("Missing required argument");
    }

    @Test
    @Launch({UpdateCompatibility.NAME, UpdateCompatibilityCheck.NAME})
    public void testMissingOptionOnCheck(CLIResult cliResult) {
        cliResult.assertError("Missing required argument: " + UpdateCompatibilityCheck.INPUT_OPTION_NAME);
    }

    @Test
    public void testCompatible(KeycloakDistribution distribution) throws IOException {
        var jsonFile = createTempFile("compatible");
        var result = distribution.run(UpdateCompatibility.NAME, UpdateCompatibilityMetadata.NAME, UpdateCompatibilityMetadata.OUTPUT_OPTION_NAME, jsonFile.getAbsolutePath());
        result.assertMessage("Metadata:");
        assertEquals(0, result.exitCode());

        var info = JsonSerialization.mapper.readValue(jsonFile, ServerInfo.class);
        assertEquals(CompatibilityManagerImpl.EPOCH, info.getEpoch());
        assertEquals(Version.VERSION, info.getVersions().get(CompatibilityManagerImpl.KEYCLOAK_VERSION_KEY));
        assertEquals(org.infinispan.commons.util.Version.getVersion(), info.getVersions().get(CompatibilityManagerImpl.INFINISPAN_VERSION_KEY));

        result = distribution.run(UpdateCompatibility.NAME, UpdateCompatibilityCheck.NAME, UpdateCompatibilityCheck.INPUT_OPTION_NAME, jsonFile.getAbsolutePath());
        result.assertMessage("[OK] Rolling Upgrade is available.");
        result.assertNoError("Rolling Upgrade is not available.");
    }

    @Test
    public void testWrongEpoch(KeycloakDistribution distribution) throws IOException {
        var jsonFile = createTempFile("wrong-epoch");
        var info = new ServerInfo();
        info.setEpoch(-1);

        JsonSerialization.mapper.writeValue(jsonFile, info);
        var result = distribution.run(UpdateCompatibility.NAME, UpdateCompatibilityCheck.NAME, UpdateCompatibilityCheck.INPUT_OPTION_NAME, jsonFile.getAbsolutePath());
        result.assertError("[Epoch] Rolling Upgrade is not available. 'Epoch' is incompatible: Old=-1, New=%s".formatted(CompatibilityManagerImpl.EPOCH));
    }

    @Test
    public void testWrongVersions(KeycloakDistribution distribution) throws IOException {
        var jsonFile = createTempFile("wrong-versions");

        // incompatible keycloak version
        var info = new ServerInfo();
        info.setEpoch(CompatibilityManagerImpl.EPOCH);
        info.setVersions(Map.of(CompatibilityManagerImpl.KEYCLOAK_VERSION_KEY, "0.0.0.Final",
                CompatibilityManagerImpl.INFINISPAN_VERSION_KEY, org.infinispan.commons.util.Version.getVersion()));
        JsonSerialization.mapper.writeValue(jsonFile, info);

        var result = distribution.run(UpdateCompatibility.NAME, UpdateCompatibilityCheck.NAME, UpdateCompatibilityCheck.INPUT_OPTION_NAME, jsonFile.getAbsolutePath());
        result.assertError("[Versions] Rolling Upgrade is not available. 'keycloak' is incompatible: Old=0.0.0.Final, New=%s".formatted(Version.VERSION));

        // incompatible infinispan version
        info.setVersions(Map.of(
                CompatibilityManagerImpl.KEYCLOAK_VERSION_KEY, Version.VERSION,
                CompatibilityManagerImpl.INFINISPAN_VERSION_KEY, "0.0.0.Final"));
        JsonSerialization.mapper.writeValue(jsonFile, info);

        result = distribution.run(UpdateCompatibility.NAME, UpdateCompatibilityCheck.NAME, UpdateCompatibilityCheck.INPUT_OPTION_NAME, jsonFile.getAbsolutePath());
        result.assertError("[Versions] Rolling Upgrade is not available. 'infinispan' is incompatible: Old=0.0.0.Final, New=%s".formatted(org.infinispan.commons.util.Version.getVersion()));
    }

    private static File createTempFile(String prefix) throws IOException {
        var file = File.createTempFile(prefix, ".json");
        file.deleteOnExit();
        return file;
    }

}
