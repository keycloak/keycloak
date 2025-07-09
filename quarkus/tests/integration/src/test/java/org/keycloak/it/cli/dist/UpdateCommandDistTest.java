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

import io.quarkus.test.junit.main.Launch;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.keycloak.common.Version;
import org.keycloak.compatibility.CompatibilityResult;
import org.keycloak.compatibility.KeycloakCompatibilityMetadataProvider;
import org.keycloak.infinispan.compatibility.CachingCompatibilityMetadataProvider;
import org.keycloak.it.junit5.extension.CLIResult;
import org.keycloak.it.junit5.extension.DistributionTest;
import org.keycloak.it.junit5.extension.RawDistOnly;
import org.keycloak.it.utils.KeycloakDistribution;
import org.keycloak.quarkus.runtime.cli.command.UpdateCompatibility;
import org.keycloak.quarkus.runtime.cli.command.UpdateCompatibilityCheck;
import org.keycloak.quarkus.runtime.cli.command.UpdateCompatibilityMetadata;
import org.keycloak.util.JsonSerialization;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.keycloak.it.cli.dist.Util.createTempFile;

@DistributionTest
@RawDistOnly(reason = "Requires creating JSON file to be available between containers")
public class UpdateCommandDistTest {

    private static final String DISABLE_FEATURE = "--features-disabled=rolling-updates";

    @Test
    @Launch({UpdateCompatibility.NAME, UpdateCompatibilityMetadata.NAME, DISABLE_FEATURE})
    public void testFeatureNotEnabled(CLIResult cliResult) {
        cliResult.assertError("Unable to use this command. None of the versions of the feature 'rolling-updates' is enabled.");
    }

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
        var jsonFile = createTempFile("compatible", ".json");
        var result = distribution.run(UpdateCompatibility.NAME, UpdateCompatibilityMetadata.NAME, UpdateCompatibilityMetadata.OUTPUT_OPTION_NAME, jsonFile.getAbsolutePath());
        result.assertMessage("Metadata:");
        assertEquals(0, result.exitCode());

        var info = JsonSerialization.mapper.readValue(jsonFile, UpdateCompatibilityCheck.METADATA_TYPE_REF);
        assertEquals(Version.VERSION, info.get(KeycloakCompatibilityMetadataProvider.ID).get("version"));
        assertEquals(org.infinispan.commons.util.Version.getVersion(), info.get(CachingCompatibilityMetadataProvider.ID).get("version"));
        assertEquals(org.jgroups.Version.printVersion(), info.get(CachingCompatibilityMetadataProvider.ID).get("jgroupsVersion"));

        result = distribution.run(UpdateCompatibility.NAME, UpdateCompatibilityCheck.NAME, UpdateCompatibilityCheck.INPUT_OPTION_NAME, jsonFile.getAbsolutePath());
        result.assertExitCode(CompatibilityResult.ExitCode.ROLLING.value());
        result.assertMessage("[OK] Rolling Update is available.");
        result.assertNoError("Rolling Update is not available.");
    }

    @Test
    public void testWrongVersions(KeycloakDistribution distribution) throws IOException {
        var jsonFile = createTempFile("wrong-versions", ".json");

        // incompatible keycloak version
        var info = new HashMap<String, Map<String, String>>();
        info.put(KeycloakCompatibilityMetadataProvider.ID, Map.of("version", "0.0.0.Final"));
        info.put(CachingCompatibilityMetadataProvider.ID, Map.of(
                "version", org.infinispan.commons.util.Version.getVersion(),
                "persistence", "true",
                "mode", "embedded",
                "jgroupsVersion", org.jgroups.Version.printVersion()
        ));
        JsonSerialization.mapper.writeValue(jsonFile, info);

        var result = distribution.run(UpdateCompatibility.NAME, UpdateCompatibilityCheck.NAME, UpdateCompatibilityCheck.INPUT_OPTION_NAME, jsonFile.getAbsolutePath());
        result.assertExitCode(CompatibilityResult.ExitCode.RECREATE.value());
        result.assertError("[%s] Rolling Update is not available. '%s.version' is incompatible: 0.0.0.Final -> %s.".formatted(KeycloakCompatibilityMetadataProvider.ID, KeycloakCompatibilityMetadataProvider.ID, Version.VERSION));

        // incompatible infinispan version
        info.put(KeycloakCompatibilityMetadataProvider.ID, Map.of("version", Version.VERSION));
        info.put(CachingCompatibilityMetadataProvider.ID, Map.of(
                "version", "0.0.0.Final",
                "persistence", "true",
                "mode", "embedded",
                "jgroupsVersion", org.jgroups.Version.printVersion()
        ));
        JsonSerialization.mapper.writeValue(jsonFile, info);

        // incompatible jgroups version
        result = distribution.run(UpdateCompatibility.NAME, UpdateCompatibilityCheck.NAME, UpdateCompatibilityCheck.INPUT_OPTION_NAME, jsonFile.getAbsolutePath());
        result.assertExitCode(CompatibilityResult.ExitCode.RECREATE.value());
        result.assertError("[%s] Rolling Update is not available. '%s.version' is incompatible: 0.0.0.Final -> %s.".formatted(CachingCompatibilityMetadataProvider.ID, CachingCompatibilityMetadataProvider.ID, org.infinispan.commons.util.Version.getVersion())); // incompatible infinispan version

        info.put(KeycloakCompatibilityMetadataProvider.ID, Map.of("version", Version.VERSION));
        info.put(CachingCompatibilityMetadataProvider.ID, Map.of(
                "version", org.infinispan.commons.util.Version.getVersion(),
                "persistence", "true",
                "mode", "embedded",
                "jgroupsVersion", "0.0.0.Final"
        ));
        JsonSerialization.mapper.writeValue(jsonFile, info);

        result = distribution.run(UpdateCompatibility.NAME, UpdateCompatibilityCheck.NAME, UpdateCompatibilityCheck.INPUT_OPTION_NAME, jsonFile.getAbsolutePath());
        result.assertExitCode(CompatibilityResult.ExitCode.RECREATE.value());
        result.assertError("[%s] Rolling Update is not available. '%s.jgroupsVersion' is incompatible: 0.0.0.Final -> %s.".formatted(CachingCompatibilityMetadataProvider.ID, CachingCompatibilityMetadataProvider.ID, org.jgroups.Version.printVersion()));
    }
}

