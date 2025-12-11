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

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.keycloak.common.Profile;
import org.keycloak.common.Version;
import org.keycloak.compatibility.CompatibilityResult;
import org.keycloak.compatibility.FeatureCompatibilityMetadataProvider;
import org.keycloak.compatibility.KeycloakCompatibilityMetadataProvider;
import org.keycloak.config.DatabaseOptions;
import org.keycloak.it.junit5.extension.CLIResult;
import org.keycloak.it.junit5.extension.DistributionTest;
import org.keycloak.it.junit5.extension.RawDistOnly;
import org.keycloak.it.utils.KeycloakDistribution;
import org.keycloak.it.utils.RawKeycloakDistribution;
import org.keycloak.jgroups.certificates.DefaultJGroupsCertificateProviderFactory;
import org.keycloak.quarkus.runtime.cli.command.UpdateCompatibility;
import org.keycloak.quarkus.runtime.cli.command.UpdateCompatibilityCheck;
import org.keycloak.quarkus.runtime.cli.command.UpdateCompatibilityMetadata;
import org.keycloak.quarkus.runtime.configuration.compatibility.DatabaseCompatibilityMetadataProvider;
import org.keycloak.spi.infinispan.CacheEmbeddedConfigProviderSpi;
import org.keycloak.spi.infinispan.CacheRemoteConfigProviderSpi;
import org.keycloak.spi.infinispan.JGroupsCertificateProviderSpi;
import org.keycloak.spi.infinispan.impl.embedded.DefaultCacheEmbeddedConfigProviderFactory;
import org.keycloak.spi.infinispan.impl.remote.DefaultCacheRemoteConfigProviderFactory;
import org.keycloak.util.JsonSerialization;

import io.quarkus.test.junit.main.Launch;
import org.junit.jupiter.api.Test;

import static org.keycloak.it.cli.dist.Util.createTempFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DistributionTest
@RawDistOnly(reason = "Requires creating JSON file to be available between containers")
public class UpdateCommandDistTest {

    @Test
    @Launch({UpdateCompatibility.NAME, UpdateCompatibilityMetadata.NAME, "--features-disabled=rolling-updates"})
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
        var result = distribution.run(UpdateCompatibility.NAME, UpdateCompatibilityMetadata.NAME, UpdateCompatibilityMetadata.OUTPUT_OPTION_NAME, jsonFile.getAbsolutePath(), "--cache-embedded-mtls-enabled", "true");
        result.assertMessage("Metadata:");
        assertEquals(0, result.exitCode());

        var info = JsonSerialization.mapper.readValue(jsonFile, UpdateCompatibilityCheck.METADATA_TYPE_REF);
        assertEquals(Version.VERSION, info.get(KeycloakCompatibilityMetadataProvider.ID).get("version"));
        assertEquals(org.infinispan.commons.util.Version.getVersion(), info.get(CacheEmbeddedConfigProviderSpi.SPI_NAME).get("version"));
        assertEquals(org.jgroups.Version.printVersion(), info.get(CacheEmbeddedConfigProviderSpi.SPI_NAME).get("jgroupsVersion"));

        var cacheMeta = info.get(CacheEmbeddedConfigProviderSpi.SPI_NAME);
        assertTrue(cacheMeta.get(DefaultCacheEmbeddedConfigProviderFactory.CONFIG).endsWith("conf/cache-ispn.xml"));
        assertNull(cacheMeta.get(DefaultCacheEmbeddedConfigProviderFactory.STACK));

        var jgroupsMeta = info.get(JGroupsCertificateProviderSpi.SPI_NAME);
        assertEquals("true", jgroupsMeta.get(DefaultJGroupsCertificateProviderFactory.ENABLED));

        result = distribution.run(UpdateCompatibility.NAME, UpdateCompatibilityCheck.NAME, UpdateCompatibilityCheck.INPUT_OPTION_NAME, jsonFile.getAbsolutePath());
        result.assertExitCode(CompatibilityResult.ExitCode.ROLLING.value());
        result.assertMessage("[OK] Rolling Update is available.");
        result.assertNoError("Rolling Update is not available.");
    }

    @Test
    public void testWrongVersions(KeycloakDistribution distribution) throws IOException {
        var jsonFile = createTempFile("wrong-versions", ".json");

        // incompatible keycloak version
        var info = defaultMeta(distribution);
        info.get(KeycloakCompatibilityMetadataProvider.ID).put("version", "0.0.0.Final");

        Profile.configure();
        info.put(FeatureCompatibilityMetadataProvider.ID, new FeatureCompatibilityMetadataProvider().metadata());
        JsonSerialization.mapper.writeValue(jsonFile, info);

        var result = distribution.run(UpdateCompatibility.NAME, UpdateCompatibilityCheck.NAME, UpdateCompatibilityCheck.INPUT_OPTION_NAME, jsonFile.getAbsolutePath());
        result.assertExitCode(CompatibilityResult.ExitCode.RECREATE.value());
        result.assertError("[%s] Rolling Update is not available. '%s.version' is incompatible: 0.0.0.Final -> %s.".formatted(KeycloakCompatibilityMetadataProvider.ID, KeycloakCompatibilityMetadataProvider.ID, Version.VERSION));

        // incompatible infinispan version
        info = defaultMeta(distribution);
        info.get(CacheEmbeddedConfigProviderSpi.SPI_NAME).put("version", "0.0.0.Final");
        JsonSerialization.mapper.writeValue(jsonFile, info);

        // incompatible jgroups version
        result = distribution.run(UpdateCompatibility.NAME, UpdateCompatibilityCheck.NAME, UpdateCompatibilityCheck.INPUT_OPTION_NAME, jsonFile.getAbsolutePath());
        result.assertExitCode(CompatibilityResult.ExitCode.RECREATE.value());
        result.assertError("[%s] Rolling Update is not available. '%s.version' is incompatible: 0.0.0.Final -> %s.".formatted(CacheEmbeddedConfigProviderSpi.SPI_NAME, CacheEmbeddedConfigProviderSpi.SPI_NAME, org.infinispan.commons.util.Version.getVersion())); // incompatible infinispan version

        info = defaultMeta(distribution);
        info.get(CacheEmbeddedConfigProviderSpi.SPI_NAME).put("jgroupsVersion", "0.0.0.Final");
        JsonSerialization.mapper.writeValue(jsonFile, info);

        result = distribution.run(UpdateCompatibility.NAME, UpdateCompatibilityCheck.NAME, UpdateCompatibilityCheck.INPUT_OPTION_NAME, jsonFile.getAbsolutePath());
        result.assertExitCode(CompatibilityResult.ExitCode.RECREATE.value());
        result.assertError("[%s] Rolling Update is not available. '%s.jgroupsVersion' is incompatible: 0.0.0.Final -> %s.".formatted(CacheEmbeddedConfigProviderSpi.SPI_NAME, CacheEmbeddedConfigProviderSpi.SPI_NAME, org.jgroups.Version.printVersion()));
    }

    private String resolveConfigFile(KeycloakDistribution distribution, String... paths) {
        Path dist = distribution.unwrap(RawKeycloakDistribution.class).getDistPath();
        return Paths.get(dist.toString(), paths).toString();
    }

    @Test
    public void testCacheLocalChange(KeycloakDistribution distribution) throws IOException {
        var jsonFile = createTempFile("compatible", ".json");
        var result = distribution.run(UpdateCompatibility.NAME, UpdateCompatibilityMetadata.NAME, UpdateCompatibilityMetadata.OUTPUT_OPTION_NAME, jsonFile.getAbsolutePath(), "--cache", "local");
        result.assertMessage("Metadata:");
        assertEquals(0, result.exitCode());

        var info = JsonSerialization.mapper.readValue(jsonFile, UpdateCompatibilityCheck.METADATA_TYPE_REF);
        assertTrue(info.get(CacheEmbeddedConfigProviderSpi.SPI_NAME).get("configFile").endsWith("cache-local.xml"));

        result = distribution.run(UpdateCompatibility.NAME, UpdateCompatibilityCheck.NAME, UpdateCompatibilityCheck.INPUT_OPTION_NAME, jsonFile.getAbsolutePath(), "--cache", "ispn");
        result.assertExitCode(CompatibilityResult.ExitCode.RECREATE.value());
        result.assertError("[%s] Rolling Update is not available. '%s.configFile' is incompatible: cache-local.xml -> %s.".formatted(CacheEmbeddedConfigProviderSpi.SPI_NAME, CacheEmbeddedConfigProviderSpi.SPI_NAME, resolveConfigFile(distribution, "conf", "cache-ispn.xml")));
    }

    @Test
    public void testChangeCacheRemoteToEmbedded(KeycloakDistribution distribution) throws IOException {
        var jsonFile = createTempFile("compatible", ".json");
        var result = distribution.run(UpdateCompatibility.NAME, UpdateCompatibilityMetadata.NAME, UpdateCompatibilityMetadata.OUTPUT_OPTION_NAME, jsonFile.getAbsolutePath(), "--features", "clusterless", "--cache-remote-host", "127.0.0.1");
        result.assertMessage("Metadata:");
        assertEquals(0, result.exitCode());

        var info = JsonSerialization.mapper.readValue(jsonFile, UpdateCompatibilityCheck.METADATA_TYPE_REF);
        assertEquals(Version.VERSION, info.get(KeycloakCompatibilityMetadataProvider.ID).get("version"));
        assertNull(info.get(CacheEmbeddedConfigProviderSpi.SPI_NAME));
        assertNull(info.get(JGroupsCertificateProviderSpi.SPI_NAME));

        var cacheMeta = info.get(CacheRemoteConfigProviderSpi.SPI_NAME);
        assertEquals("127.0.0.1", cacheMeta.get(DefaultCacheRemoteConfigProviderFactory.HOSTNAME));
        assertEquals("11222", cacheMeta.get(DefaultCacheRemoteConfigProviderFactory.PORT));

        result = distribution.run(UpdateCompatibility.NAME, UpdateCompatibilityCheck.NAME, UpdateCompatibilityCheck.INPUT_OPTION_NAME, jsonFile.getAbsolutePath());
        result.assertExitCode(CompatibilityResult.ExitCode.RECREATE.value());
        result.assertError("[%1$s] Rolling Update is not available. '%1$s.configFile' is incompatible: null".formatted(CacheEmbeddedConfigProviderSpi.SPI_NAME));
        result.assertError("[%1$s] Rolling Update is not available. '%1$s.jgroupsVersion' is incompatible: null".formatted(CacheEmbeddedConfigProviderSpi.SPI_NAME));
        result.assertError("[%1$s] Rolling Update is not available. '%1$s.version' is incompatible: null".formatted(CacheEmbeddedConfigProviderSpi.SPI_NAME));
    }

    @Test
    public void testChangeCacheEmbeddedToRemote(KeycloakDistribution distribution) throws IOException {
        var jsonFile = createTempFile("compatible", ".json");
        var result = distribution.run(UpdateCompatibility.NAME, UpdateCompatibilityMetadata.NAME, UpdateCompatibilityMetadata.OUTPUT_OPTION_NAME, jsonFile.getAbsolutePath());
        result.assertMessage("Metadata:");
        assertEquals(0, result.exitCode());

        var info = JsonSerialization.mapper.readValue(jsonFile, UpdateCompatibilityCheck.METADATA_TYPE_REF);
        info.remove(FeatureCompatibilityMetadataProvider.ID);
        assertEquals(defaultMeta(distribution), info);

        result = distribution.run(UpdateCompatibility.NAME, UpdateCompatibilityCheck.NAME, UpdateCompatibilityCheck.INPUT_OPTION_NAME, jsonFile.getAbsolutePath(), "--features", "clusterless", "--cache-remote-host", "127.0.0.1");
        result.assertExitCode(CompatibilityResult.ExitCode.RECREATE.value());
        result.assertError("[%1$s] Rolling Update is not available. '%1$s.configFile' is incompatible:".formatted(CacheEmbeddedConfigProviderSpi.SPI_NAME));
        result.assertError("[%1$s] Rolling Update is not available. '%1$s.jgroupsVersion' is incompatible:".formatted(CacheEmbeddedConfigProviderSpi.SPI_NAME));
        result.assertError("[%1$s] Rolling Update is not available. '%1$s.version' is incompatible:".formatted(CacheEmbeddedConfigProviderSpi.SPI_NAME));
    }

    @Test
    public void testChangeCacheStack(KeycloakDistribution distribution) throws IOException {
        var jsonFile = createTempFile("compatible", ".json");
        var result = distribution.run(UpdateCompatibility.NAME, UpdateCompatibilityMetadata.NAME, UpdateCompatibilityMetadata.OUTPUT_OPTION_NAME, jsonFile.getAbsolutePath());
        result.assertMessage("Metadata:");
        assertEquals(0, result.exitCode());

        var info = JsonSerialization.mapper.readValue(jsonFile, UpdateCompatibilityCheck.METADATA_TYPE_REF);
        info.remove(FeatureCompatibilityMetadataProvider.ID);
        assertEquals(defaultMeta(distribution), info);

        result = distribution.run(UpdateCompatibility.NAME, UpdateCompatibilityCheck.NAME, UpdateCompatibilityCheck.INPUT_OPTION_NAME, jsonFile.getAbsolutePath(), "--cache-stack", "jdbc-ping-udp");
        result.assertExitCode(CompatibilityResult.ExitCode.RECREATE.value());
        result.assertError("[%1$s] Rolling Update is not available. '%1$s.stack' is incompatible: null -> jdbc-ping-udp".formatted(CacheEmbeddedConfigProviderSpi.SPI_NAME));
    }

    @Test
    public void testDatabaseTypeChanged(KeycloakDistribution distribution) throws IOException {
        var jsonFile = createTempFile("compatible", ".json");
        var result = distribution.run(UpdateCompatibility.NAME, UpdateCompatibilityMetadata.NAME, UpdateCompatibilityMetadata.OUTPUT_OPTION_NAME, jsonFile.getAbsolutePath());
        result.assertMessage("Metadata:");
        assertEquals(0, result.exitCode());

        var info = JsonSerialization.mapper.readValue(jsonFile, UpdateCompatibilityCheck.METADATA_TYPE_REF);
        info.remove(FeatureCompatibilityMetadataProvider.ID);
        assertEquals(defaultMeta(distribution), info);

        result = distribution.run(UpdateCompatibility.NAME, UpdateCompatibilityCheck.NAME, UpdateCompatibilityCheck.INPUT_OPTION_NAME, jsonFile.getAbsolutePath(), "--db", "postgres");
        result.assertExitCode(CompatibilityResult.ExitCode.RECREATE.value());
        result.assertError("[%1$s] Rolling Update is not available. '%1$s.db' is incompatible: dev-file -> postgres".formatted(DatabaseCompatibilityMetadataProvider.ID));
    }

    @Test
    public void testDatabaseUrlChanged(KeycloakDistribution distribution) throws IOException {
        var jsonFile = createTempFile("compatible", ".json");
        var result = distribution.run(UpdateCompatibility.NAME, UpdateCompatibilityMetadata.NAME, UpdateCompatibilityMetadata.OUTPUT_OPTION_NAME, jsonFile.getAbsolutePath(), "--db", "postgres", "--db-url", "jdbc:postgresql://mypostgres/mydatabase");
        result.assertMessage("Metadata:");
        assertEquals(0, result.exitCode());

        var info = JsonSerialization.mapper.readValue(jsonFile, UpdateCompatibilityCheck.METADATA_TYPE_REF);
        var expectedMeta = defaultMeta(distribution);
        expectedMeta.put(DatabaseCompatibilityMetadataProvider.ID, Map.of(
              DatabaseOptions.DB.getKey(), "postgres"
        ));
        info.remove(FeatureCompatibilityMetadataProvider.ID);
        assertEquals(expectedMeta, info);

        result = distribution.run(UpdateCompatibility.NAME, UpdateCompatibilityCheck.NAME, UpdateCompatibilityCheck.INPUT_OPTION_NAME, jsonFile.getAbsolutePath(), "--db", "postgres", "--db-url", "jdbc:postgresql://mypostgres/mydatabase?ssl=false");
        result.assertExitCode(CompatibilityResult.ExitCode.ROLLING.value());
    }

    @Test
    public void testDatabaseUrlOptions(KeycloakDistribution distribution) throws IOException {
        var jsonFile = createTempFile("compatible", ".json");
        var result = distribution.run(UpdateCompatibility.NAME, UpdateCompatibilityMetadata.NAME, UpdateCompatibilityMetadata.OUTPUT_OPTION_NAME, jsonFile.getAbsolutePath(), "--db-url-host", "localhost", "--db-url-port", "9999", "--db-url-database", "keycloak");
        result.assertMessage("Metadata:");
        assertEquals(0, result.exitCode());

        // Assert that expected db-url-* options are written to the metadata when --db-url is not present
        var info = JsonSerialization.mapper.readValue(jsonFile, UpdateCompatibilityCheck.METADATA_TYPE_REF);
        var expectedMeta = defaultMeta(distribution);
        expectedMeta.put(DatabaseCompatibilityMetadataProvider.ID, Map.of(
              DatabaseOptions.DB.getKey(), DatabaseOptions.DB.getDefaultValue().get(),
              DatabaseOptions.DB_URL_DATABASE.getKey(), "keycloak",
              DatabaseOptions.DB_URL_HOST.getKey(), "localhost",
              DatabaseOptions.DB_URL_PORT.getKey(), "9999"
        ));
        info.remove(FeatureCompatibilityMetadataProvider.ID);
        assertEquals(expectedMeta, info);

        // Assert that changing to --db-url requires a recreate
        result = distribution.run(UpdateCompatibility.NAME, UpdateCompatibilityCheck.NAME, UpdateCompatibilityCheck.INPUT_OPTION_NAME, jsonFile.getAbsolutePath(), "--db-url", "jdbc:h2:mem:keycloakdb");
        result.assertExitCode(CompatibilityResult.ExitCode.RECREATE.value());

        // Assert that db-url-* options are not written to metadata when db-url is present
        result = distribution.run(UpdateCompatibility.NAME, UpdateCompatibilityMetadata.NAME, UpdateCompatibilityMetadata.OUTPUT_OPTION_NAME, jsonFile.getAbsolutePath(), "--db-url", "jdbc:h2:mem:keycloakdb", "--db-url-host", "localhost", "--db-url-port", "9999", "--db-url-database", "keycloak");
        result.assertMessage("Metadata:");
        assertEquals(0, result.exitCode());

        info = JsonSerialization.mapper.readValue(jsonFile, UpdateCompatibilityCheck.METADATA_TYPE_REF);
        expectedMeta.put(DatabaseCompatibilityMetadataProvider.ID, Map.of(
              DatabaseOptions.DB.getKey(), DatabaseOptions.DB.getDefaultValue().get()
        ));
        info.remove(FeatureCompatibilityMetadataProvider.ID);
        assertEquals(expectedMeta, info);

        // Assert that changes to the db-url does not trigger a recreate
        result = distribution.run(UpdateCompatibility.NAME, UpdateCompatibilityCheck.NAME, UpdateCompatibilityCheck.INPUT_OPTION_NAME, jsonFile.getAbsolutePath(), "--db-url", "jdbc:h2:mem:keycloak");
        result.assertExitCode(CompatibilityResult.ExitCode.ROLLING.value());
    }

    private Map<String, Map<String, String>> defaultMeta(KeycloakDistribution distribution) {
        Map<String, String> keycloak = new HashMap<>(1);
        keycloak.put("version", Version.VERSION);

        Map<String, Map<String, String>> m = new HashMap<>();
        m.put(KeycloakCompatibilityMetadataProvider.ID, keycloak);
        m.put(DatabaseCompatibilityMetadataProvider.ID, Map.of(
              DatabaseOptions.DB.getKey(), DatabaseOptions.DB.getDefaultValue().get()
        ));
        m.put(CacheEmbeddedConfigProviderSpi.SPI_NAME, embeddedCachingMeta(distribution));
        m.put(JGroupsCertificateProviderSpi.SPI_NAME, Map.of(
              "enabled", "true"
        ));
        return m;
    }

    private Map<String, String> embeddedCachingMeta(KeycloakDistribution distribution) {
        Map<String, String> m = new HashMap<>();
        m.put("version", org.infinispan.commons.util.Version.getVersion());
        m.put("jgroupsVersion", org.jgroups.Version.printVersion());
        m.put("configFile", resolveConfigFile(distribution, "conf", "cache-ispn.xml"));
        return m;
    }
}
