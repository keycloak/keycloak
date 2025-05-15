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
import org.keycloak.migration.ModelVersion;
import org.keycloak.quarkus.runtime.cli.command.UpdateCompatibility;
import org.keycloak.quarkus.runtime.cli.command.UpdateCompatibilityCheck;
import org.keycloak.quarkus.runtime.cli.command.UpdateCompatibilityMetadata;
import org.keycloak.util.JsonSerialization;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@DistributionTest
@RawDistOnly(reason = "Requires creating JSON file to be available between containers")
public class UpdateCommandDistTest {

    private static final String DISABLE_FEATURE = "--features-disabled=rolling-updates";
    private static final String ENABLE_V2_FEATURE = "--features=rolling-updates:v2";

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
        var jsonFile = createTempFile("compatible");
        var result = distribution.run(UpdateCompatibility.NAME, UpdateCompatibilityMetadata.NAME, UpdateCompatibilityMetadata.OUTPUT_OPTION_NAME, jsonFile.getAbsolutePath());
        result.assertMessage("Metadata:");
        assertEquals(0, result.exitCode());

        var info = JsonSerialization.mapper.readValue(jsonFile, UpdateCompatibilityCheck.METADATA_TYPE_REF);
        assertEquals(Version.VERSION, info.get(KeycloakCompatibilityMetadataProvider.ID).get("version"));
        assertEquals(org.infinispan.commons.util.Version.getVersion(), info.get(CachingCompatibilityMetadataProvider.ID).get("version"));

        result = distribution.run(UpdateCompatibility.NAME, UpdateCompatibilityCheck.NAME, UpdateCompatibilityCheck.INPUT_OPTION_NAME, jsonFile.getAbsolutePath());
        result.assertExitCode(CompatibilityResult.ExitCode.ROLLING.value());
        result.assertMessage("[OK] Rolling Update is available.");
        result.assertNoError("Rolling Update is not available.");
    }

    @Test
    public void testWrongVersions(KeycloakDistribution distribution) throws IOException {
        var jsonFile = createTempFile("wrong-versions");

        // incompatible keycloak version
        var info = new HashMap<String, Map<String, String>>();
        info.put(KeycloakCompatibilityMetadataProvider.ID, Map.of("version", "0.0.0.Final"));
        info.put(CachingCompatibilityMetadataProvider.ID, Map.of(
                "version", org.infinispan.commons.util.Version.getVersion(),
                "persistence", "true",
                "mode", "embedded"
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
                "mode", "embedded"
        ));
        JsonSerialization.mapper.writeValue(jsonFile, info);

        result = distribution.run(UpdateCompatibility.NAME, UpdateCompatibilityCheck.NAME, UpdateCompatibilityCheck.INPUT_OPTION_NAME, jsonFile.getAbsolutePath());
        assertEquals(3, result.exitCode());
        result.assertError("[%s] Rolling Update is not available. '%s.version' is incompatible: 0.0.0.Final -> %s.".formatted(CachingCompatibilityMetadataProvider.ID, CachingCompatibilityMetadataProvider.ID, org.infinispan.commons.util.Version.getVersion()));
    }

    @Test
    public void testCompatibleForV2Feature(KeycloakDistribution distribution) throws IOException {
        var jsonFile = createTempFile("test-patch-releases-compatible");

        // test the same version
        testCompatibleV2KeycloakVersion(distribution, jsonFile, Version.VERSION);

        // Test previous micro release
        ModelVersion modelVersion = new ModelVersion(Version.VERSION);
        // We are not able to test the following unless we are in a micro version larger than 0
        //  This is tested in unit test class KeycloakCompatibilityMetadataProvider
        assumeTrue(modelVersion.getMicro() != 0);

        testCompatibleV2KeycloakVersion(distribution, jsonFile, String.format("%d.%d.%d-%s", modelVersion.getMajor(), modelVersion.getMinor(), modelVersion.getMicro() - 1, modelVersion.getQualifier()));

        // Test qualifier ignored
        testCompatibleV2KeycloakVersion(distribution, jsonFile, String.format("%d.%d.%d-%s", modelVersion.getMajor(), modelVersion.getMinor(), modelVersion.getMicro() - 1, "SNAPSHOT1"));
    }

    @Test
    public void testIncompatibleForV2Feature(KeycloakDistribution distribution) throws IOException {
        var jsonFile = createTempFile("test-patch-releases-incompatible");
        ModelVersion modelVersion = new ModelVersion(Version.VERSION);

        // Some of the following tests are ignored if we are in a version where we can't subtract from major/minor/micro version value
        //  This is tested in the unit test class KeycloakCompatibilityMetadataProvider

        // test skipping patch release
        if (modelVersion.getMicro() > 1) {
            testIncompatibleV2KeycloakVersion(distribution, jsonFile, String.format("%d.%d.%d-%s", modelVersion.getMajor(), modelVersion.getMinor(), modelVersion.getMicro() - 2, modelVersion.getQualifier()));
        }

        // test rollback to the previous version
        testIncompatibleV2KeycloakVersion(distribution, jsonFile, String.format("%d.%d.%d-%s", modelVersion.getMajor(), modelVersion.getMinor(), modelVersion.getMicro() + 1, modelVersion.getQualifier()));

        // test a different minor version
        if (modelVersion.getMicro() != 0 && modelVersion.getMinor() != 0) {
            testIncompatibleV2KeycloakVersion(distribution, jsonFile, String.format("%d.%d.%d-%s", modelVersion.getMajor(), modelVersion.getMinor() - 1, modelVersion.getMicro() - 1, modelVersion.getQualifier()));
        }

        // test a different major version
        if (modelVersion.getMicro() != 0 && modelVersion.getMajor() != 0) {
            testIncompatibleV2KeycloakVersion(distribution, jsonFile, String.format("%d.%d.%d-%s", modelVersion.getMajor() - 1, modelVersion.getMinor(), modelVersion.getMicro() - 1, modelVersion.getQualifier()));
        }
    }

    private void testIncompatibleV2KeycloakVersion(KeycloakDistribution distribution, File jsonFile, String previousVersion) throws IOException {
        var info = new HashMap<String, Map<String, String>>();
        info.put(KeycloakCompatibilityMetadataProvider.ID, Map.of("version", previousVersion));
        info.put(CachingCompatibilityMetadataProvider.ID, Map.of(
                "version", org.infinispan.commons.util.Version.getVersion(),
                "persistence", "true",
                "mode", "embedded"
        ));
        JsonSerialization.mapper.writeValue(jsonFile, info);

        var result = distribution.run(UpdateCompatibility.NAME, UpdateCompatibilityCheck.NAME, UpdateCompatibilityCheck.INPUT_OPTION_NAME, jsonFile.getAbsolutePath(), ENABLE_V2_FEATURE);
        result.assertExitCode(CompatibilityResult.ExitCode.RECREATE.value());
        result.assertError("[%s] Rolling Update is not available. '%s.version' is incompatible: %s -> %s.".formatted(KeycloakCompatibilityMetadataProvider.ID, KeycloakCompatibilityMetadataProvider.ID, previousVersion, Version.VERSION));
    }

    private void testCompatibleV2KeycloakVersion(KeycloakDistribution distribution, File jsonFile, String previousVersion) throws IOException {
        var info = new HashMap<String, Map<String, String>>();
        info.put(KeycloakCompatibilityMetadataProvider.ID, Map.of("version", previousVersion));
        info.put(CachingCompatibilityMetadataProvider.ID, Map.of(
                "version", org.infinispan.commons.util.Version.getVersion(),
                "persistence", "true",
                "mode", "embedded"
        ));
        JsonSerialization.mapper.writeValue(jsonFile, info);

        var result = distribution.run(UpdateCompatibility.NAME, UpdateCompatibilityCheck.NAME, UpdateCompatibilityCheck.INPUT_OPTION_NAME, jsonFile.getAbsolutePath(), ENABLE_V2_FEATURE);
        result.assertExitCode(CompatibilityResult.ExitCode.ROLLING.value());
        result.assertMessage("[OK] Rolling Update is available.");
        result.assertNoError("Rolling Update is not available.");
    }

    private static File createTempFile(String prefix) throws IOException {
        var file = File.createTempFile(prefix, ".json");
        file.deleteOnExit();
        return file;
    }

}

