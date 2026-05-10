package org.keycloak.quarkus.runtime.cli;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.keycloak.common.Profile;
import org.keycloak.common.Version;
import org.keycloak.compatibility.CompatibilityResult;
import org.keycloak.compatibility.FeatureCompatibilityMetadataProvider;
import org.keycloak.compatibility.KeycloakCompatibilityMetadataProvider;
import org.keycloak.config.DatabaseOptions;
import org.keycloak.jgroups.certificates.DefaultJGroupsCertificateProviderFactory;
import org.keycloak.quarkus.runtime.cli.command.UpdateCompatibility;
import org.keycloak.quarkus.runtime.cli.command.UpdateCompatibilityCheck;
import org.keycloak.quarkus.runtime.cli.command.UpdateCompatibilityMetadata;
import org.keycloak.quarkus.runtime.configuration.AbstractConfigurationTest;
import org.keycloak.quarkus.runtime.configuration.compatibility.DatabaseCompatibilityMetadataProvider;
import org.keycloak.spi.infinispan.CacheEmbeddedConfigProviderSpi;
import org.keycloak.spi.infinispan.CacheRemoteConfigProviderSpi;
import org.keycloak.spi.infinispan.JGroupsCertificateProviderSpi;
import org.keycloak.spi.infinispan.impl.remote.DefaultCacheRemoteConfigProviderFactory;
import org.keycloak.util.JsonSerialization;

import org.junit.After;
import org.junit.Test;

import static org.keycloak.infinispan.compatibility.CachingEmbeddedMetadataProvider.CONFIG_FILE_NOT_FOUND;
import static org.keycloak.infinispan.compatibility.CachingEmbeddedMetadataProvider.majorMinorOf;
import static org.keycloak.quarkus.runtime.configuration.compatibility.DatabaseCompatibilityMetadataProvider.UNSUPPORTED_CHANGES_HASH_KEY;
import static org.keycloak.spi.infinispan.impl.embedded.DefaultCacheEmbeddedConfigProviderFactory.CONFIG;
import static org.keycloak.spi.infinispan.impl.embedded.DefaultCacheEmbeddedConfigProviderFactory.STACK;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class UpdateCompatibilityPicocliTest extends AbstractConfigurationTest {

    private NonRunningPicocli pseudoLaunch(String... args) {
        resetConfiguration();
        return new NonRunningPicocli().launch(args);
    }

    @After
    @Override
    public void onAfter() {
        super.onAfter();
    }

    private static File createTempFile(String prefix, String suffix) throws IOException {
        var file = File.createTempFile(prefix, suffix);
        file.deleteOnExit();
        return file;
    }

    @Test
    public void testMissingSubCommand() {
        var result = pseudoLaunch(UpdateCompatibility.NAME);
        assertThat(result.getErrString(), containsString("Missing required subcommand"));
    }

    @Test
    public void testMissingOptionOnSave() {
        var result = pseudoLaunch(UpdateCompatibility.NAME, UpdateCompatibilityMetadata.NAME);
        assertThat(result.getOutString(), not(containsString("Missing required argument")));
    }

    @Test
    public void testMissingOptionOnCheck() {
        var result = pseudoLaunch(UpdateCompatibility.NAME, UpdateCompatibilityCheck.NAME);
        assertThat(result.getErrString(), containsString("Missing required argument: " + UpdateCompatibilityCheck.INPUT_OPTION_NAME));
    }

    @Test
    public void testFileNotFound() {
        var result = pseudoLaunch(UpdateCompatibility.NAME, UpdateCompatibilityCheck.NAME, "--file=not-found");
        assertThat(result.getErrString(), containsString("Incorrect argument --file."));
    }

    @Test
    public void testCompatible() throws IOException {
        var jsonFile = createTempFile("compatible", ".json");
        var result = pseudoLaunch(UpdateCompatibility.NAME, UpdateCompatibilityMetadata.NAME, UpdateCompatibilityMetadata.OUTPUT_OPTION_NAME, jsonFile.getAbsolutePath(), "--cache-embedded-mtls-enabled", "true");
        assertThat(result.getOutString(), containsString("Metadata:"));
        assertEquals(0, result.exitCode);

        var info = JsonSerialization.mapper.readValue(jsonFile, UpdateCompatibilityCheck.METADATA_TYPE_REF);
        assertEquals(Version.VERSION, info.get(KeycloakCompatibilityMetadataProvider.ID).get("version"));

        // Since rolling-updates:v2 is now default, versions are in Major.Minor format
        assertEquals(majorMinorOf(org.infinispan.commons.util.Version.getVersion()), info.get(CacheEmbeddedConfigProviderSpi.SPI_NAME).get("version"));
        assertEquals(majorMinorOf(org.jgroups.Version.printVersion()), info.get(CacheEmbeddedConfigProviderSpi.SPI_NAME).get("jgroupsVersion"));

        var cacheMeta = info.get(CacheEmbeddedConfigProviderSpi.SPI_NAME);
        assertEquals(CONFIG_FILE_NOT_FOUND, cacheMeta.get(CONFIG));
        assertNull(cacheMeta.get(STACK));

        var jgroupsMeta = info.get(JGroupsCertificateProviderSpi.SPI_NAME);
        assertEquals("true", jgroupsMeta.get(DefaultJGroupsCertificateProviderFactory.ENABLED));

        result = pseudoLaunch(UpdateCompatibility.NAME, UpdateCompatibilityCheck.NAME, UpdateCompatibilityCheck.INPUT_OPTION_NAME, jsonFile.getAbsolutePath());
        assertEquals(CompatibilityResult.ExitCode.ROLLING.value(), result.exitCode);
        assertThat(result.getOutString(), containsString("[OK] Rolling Update is available."));
        assertThat(result.getErrString(), not(containsString("Rolling Update is not available.")));
    }

    @Test
    public void testWrongVersions() throws IOException {
        var jsonFile = createTempFile("wrong-versions", ".json");

        // incompatible keycloak version
        var info = defaultMeta();
        info.get(KeycloakCompatibilityMetadataProvider.ID).put("version", "0.0.0.Final");

        Profile.configure();
        info.put(FeatureCompatibilityMetadataProvider.ID, new FeatureCompatibilityMetadataProvider().metadata());
        JsonSerialization.mapper.writeValue(jsonFile, info);

        var result = pseudoLaunch(UpdateCompatibility.NAME, UpdateCompatibilityCheck.NAME, UpdateCompatibilityCheck.INPUT_OPTION_NAME, jsonFile.getAbsolutePath());
        assertEquals(CompatibilityResult.ExitCode.RECREATE.value(), result.exitCode);
        assertThat(result.getErrString(), containsString("[%s] Rolling Update is not available. '%s.version' is incompatible: 0.0.0.Final -> %s.".formatted(KeycloakCompatibilityMetadataProvider.ID, KeycloakCompatibilityMetadataProvider.ID, Version.VERSION)));

        // Get Major.Minor versions for assertions
        String ispnVersion = majorMinorOf(org.infinispan.commons.util.Version.getVersion());
        String jgroupsVersion = majorMinorOf(org.jgroups.Version.printVersion());

        // incompatible infinispan version
        info = defaultMeta();
        info.get(CacheEmbeddedConfigProviderSpi.SPI_NAME).put("version", "0.0.0.Final");
        JsonSerialization.mapper.writeValue(jsonFile, info);

        result = pseudoLaunch(UpdateCompatibility.NAME, UpdateCompatibilityCheck.NAME, UpdateCompatibilityCheck.INPUT_OPTION_NAME, jsonFile.getAbsolutePath());
        assertEquals(CompatibilityResult.ExitCode.RECREATE.value(), result.exitCode);
        assertThat(result.getErrString(), containsString("[%s] Rolling Update is not available. '%s.version' is incompatible: 0.0.0.Final -> %s.".formatted(CacheEmbeddedConfigProviderSpi.SPI_NAME, CacheEmbeddedConfigProviderSpi.SPI_NAME, ispnVersion)));

        // incompatible jgroups version
        info = defaultMeta();
        info.get(CacheEmbeddedConfigProviderSpi.SPI_NAME).put("jgroupsVersion", "0.0.0.Final");
        JsonSerialization.mapper.writeValue(jsonFile, info);

        result = pseudoLaunch(UpdateCompatibility.NAME, UpdateCompatibilityCheck.NAME, UpdateCompatibilityCheck.INPUT_OPTION_NAME, jsonFile.getAbsolutePath());
        assertEquals(CompatibilityResult.ExitCode.RECREATE.value(), result.exitCode);
        assertThat(result.getErrString(), containsString("[%s] Rolling Update is not available. '%s.jgroupsVersion' is incompatible: 0.0.0.Final -> %s.".formatted(CacheEmbeddedConfigProviderSpi.SPI_NAME, CacheEmbeddedConfigProviderSpi.SPI_NAME, jgroupsVersion)));
    }

    @Test
    public void testCacheLocalChange() throws IOException {
        var jsonFile = createTempFile("compatible", ".json");
        var result = pseudoLaunch(UpdateCompatibility.NAME, UpdateCompatibilityMetadata.NAME, UpdateCompatibilityMetadata.OUTPUT_OPTION_NAME, jsonFile.getAbsolutePath(), "--cache", "local");
        assertThat(result.getOutString(), containsString("Metadata:"));
        assertEquals(0, result.exitCode);

        var info = JsonSerialization.mapper.readValue(jsonFile, UpdateCompatibilityCheck.METADATA_TYPE_REF);
        assertEquals(CONFIG_FILE_NOT_FOUND, info.get(CacheEmbeddedConfigProviderSpi.SPI_NAME).get(CONFIG));

        result = pseudoLaunch(UpdateCompatibility.NAME, UpdateCompatibilityCheck.NAME, UpdateCompatibilityCheck.INPUT_OPTION_NAME, jsonFile.getAbsolutePath(), "--cache", "ispn");
        assertEquals(CompatibilityResult.ExitCode.RECREATE.value(), result.exitCode);
        assertThat(result.getErrString(), containsString("[%s] Rolling Update is not available. '%s.enabled' is incompatible: null -> true.".formatted(JGroupsCertificateProviderSpi.SPI_NAME, JGroupsCertificateProviderSpi.SPI_NAME)));
    }

    @Test
    public void testChangeCacheRemoteToEmbedded() throws IOException {
        var jsonFile = createTempFile("compatible", ".json");
        var result = pseudoLaunch(UpdateCompatibility.NAME, UpdateCompatibilityMetadata.NAME, UpdateCompatibilityMetadata.OUTPUT_OPTION_NAME, jsonFile.getAbsolutePath(), "--features", "clusterless", "--cache-remote-host", "127.0.0.1");
        assertThat(result.getOutString(), containsString("Metadata:"));
        assertEquals(0, result.exitCode);

        var info = JsonSerialization.mapper.readValue(jsonFile, UpdateCompatibilityCheck.METADATA_TYPE_REF);
        assertEquals(Version.VERSION, info.get(KeycloakCompatibilityMetadataProvider.ID).get("version"));
        assertNull(info.get(CacheEmbeddedConfigProviderSpi.SPI_NAME));
        assertNull(info.get(JGroupsCertificateProviderSpi.SPI_NAME));

        var cacheMeta = info.get(CacheRemoteConfigProviderSpi.SPI_NAME);
        assertEquals("127.0.0.1", cacheMeta.get(DefaultCacheRemoteConfigProviderFactory.HOSTNAME));
        assertEquals("11222", cacheMeta.get(DefaultCacheRemoteConfigProviderFactory.PORT));

        result = pseudoLaunch(UpdateCompatibility.NAME, UpdateCompatibilityCheck.NAME, UpdateCompatibilityCheck.INPUT_OPTION_NAME, jsonFile.getAbsolutePath());
        assertEquals(CompatibilityResult.ExitCode.RECREATE.value(), result.exitCode);
        assertThat(result.getErrString(), containsString("[%1$s] Rolling Update is not available. '%1$s.configFile' is incompatible: null".formatted(CacheEmbeddedConfigProviderSpi.SPI_NAME)));
        assertThat(result.getErrString(), containsString("[%1$s] Rolling Update is not available. '%1$s.jgroupsVersion' is incompatible: null".formatted(CacheEmbeddedConfigProviderSpi.SPI_NAME)));
        assertThat(result.getErrString(), containsString("[%1$s] Rolling Update is not available. '%1$s.version' is incompatible: null".formatted(CacheEmbeddedConfigProviderSpi.SPI_NAME)));
    }

    @Test
    public void testRollingUpdatePatchCompatibility() throws IOException {
        var jsonFile = createTempFile("patch-compatible", ".json");
        var result = pseudoLaunch(UpdateCompatibility.NAME, UpdateCompatibilityMetadata.NAME, UpdateCompatibilityMetadata.OUTPUT_OPTION_NAME, jsonFile.getAbsolutePath());
        assertThat(result.getOutString(), containsString("Metadata:"));
        assertEquals(0, result.exitCode);

        var info = JsonSerialization.mapper.readValue(jsonFile, UpdateCompatibilityCheck.METADATA_TYPE_REF);
        var cacheMeta = info.get(CacheEmbeddedConfigProviderSpi.SPI_NAME);
        assertTrue("Infinispan version should be Major.Minor", cacheMeta.get("version").matches("^\\d+\\.\\d+$"));
        assertTrue("JGroups version should be Major.Minor", cacheMeta.get("jgroupsVersion").matches("^\\d+\\.\\d+$"));

        result = pseudoLaunch(UpdateCompatibility.NAME, UpdateCompatibilityCheck.NAME, UpdateCompatibilityCheck.INPUT_OPTION_NAME, jsonFile.getAbsolutePath());
        assertEquals(CompatibilityResult.ExitCode.ROLLING.value(), result.exitCode);
        assertThat(result.getOutString(), containsString("[OK] Rolling Update is available."));
    }

    @Test
    public void testChangeCacheEmbeddedToRemote() throws IOException {
        var jsonFile = createTempFile("compatible", ".json");
        var result = pseudoLaunch(UpdateCompatibility.NAME, UpdateCompatibilityMetadata.NAME, UpdateCompatibilityMetadata.OUTPUT_OPTION_NAME, jsonFile.getAbsolutePath());
        assertThat(result.getOutString(), containsString("Metadata:"));
        assertEquals(0, result.exitCode);

        var info = JsonSerialization.mapper.readValue(jsonFile, UpdateCompatibilityCheck.METADATA_TYPE_REF);
        info.remove(FeatureCompatibilityMetadataProvider.ID);
        assertEquals(defaultMeta(), info);

        result = pseudoLaunch(UpdateCompatibility.NAME, UpdateCompatibilityCheck.NAME, UpdateCompatibilityCheck.INPUT_OPTION_NAME, jsonFile.getAbsolutePath(), "--features", "clusterless", "--cache-remote-host", "127.0.0.1");
        assertEquals(CompatibilityResult.ExitCode.RECREATE.value(), result.exitCode);
        assertThat(result.getErrString(), containsString("[%1$s] Rolling Update is not available. '%1$s.configFile' is incompatible:".formatted(CacheEmbeddedConfigProviderSpi.SPI_NAME)));
        assertThat(result.getErrString(), containsString("[%1$s] Rolling Update is not available. '%1$s.jgroupsVersion' is incompatible:".formatted(CacheEmbeddedConfigProviderSpi.SPI_NAME)));
        assertThat(result.getErrString(), containsString("[%1$s] Rolling Update is not available. '%1$s.version' is incompatible:".formatted(CacheEmbeddedConfigProviderSpi.SPI_NAME)));
    }

    @Test
    public void testChangeCacheStack() throws IOException {
        var jsonFile = createTempFile("compatible", ".json");
        var result = pseudoLaunch(UpdateCompatibility.NAME, UpdateCompatibilityMetadata.NAME, UpdateCompatibilityMetadata.OUTPUT_OPTION_NAME, jsonFile.getAbsolutePath());
        assertThat(result.getOutString(), containsString("Metadata:"));
        assertEquals(0, result.exitCode);

        var info = JsonSerialization.mapper.readValue(jsonFile, UpdateCompatibilityCheck.METADATA_TYPE_REF);
        info.remove(FeatureCompatibilityMetadataProvider.ID);
        assertEquals(defaultMeta(), info);

        result = pseudoLaunch(UpdateCompatibility.NAME, UpdateCompatibilityCheck.NAME, UpdateCompatibilityCheck.INPUT_OPTION_NAME, jsonFile.getAbsolutePath(), "--cache-stack", "jdbc-ping-udp");
        assertEquals(CompatibilityResult.ExitCode.RECREATE.value(), result.exitCode);
        assertThat(result.getErrString(), containsString("[%1$s] Rolling Update is not available. '%1$s.stack' is incompatible: null -> jdbc-ping-udp".formatted(CacheEmbeddedConfigProviderSpi.SPI_NAME)));
    }

    @Test
    public void testDatabaseTypeChanged() throws IOException {
        var jsonFile = createTempFile("compatible", ".json");
        var result = pseudoLaunch(UpdateCompatibility.NAME, UpdateCompatibilityMetadata.NAME, UpdateCompatibilityMetadata.OUTPUT_OPTION_NAME, jsonFile.getAbsolutePath());
        assertThat(result.getOutString(), containsString("Metadata:"));
        assertEquals(0, result.exitCode);

        var info = JsonSerialization.mapper.readValue(jsonFile, UpdateCompatibilityCheck.METADATA_TYPE_REF);
        info.remove(FeatureCompatibilityMetadataProvider.ID);
        assertEquals(defaultMeta(), info);

        result = pseudoLaunch(UpdateCompatibility.NAME, UpdateCompatibilityCheck.NAME, UpdateCompatibilityCheck.INPUT_OPTION_NAME, jsonFile.getAbsolutePath(), "--db", "postgres");
        assertEquals(CompatibilityResult.ExitCode.RECREATE.value(), result.exitCode);
        assertThat(result.getErrString(), containsString("[%1$s] Rolling Update is not available. '%1$s.db' is incompatible: dev-file -> postgres".formatted(DatabaseCompatibilityMetadataProvider.ID)));
    }

    @Test
    public void testDatabaseUrlChanged() throws IOException {
        var jsonFile = createTempFile("compatible", ".json");
        var result = pseudoLaunch(UpdateCompatibility.NAME, UpdateCompatibilityMetadata.NAME, UpdateCompatibilityMetadata.OUTPUT_OPTION_NAME, jsonFile.getAbsolutePath(), "--db", "postgres", "--db-url", "jdbc:postgresql://mypostgres/mydatabase");
        assertThat(result.getOutString(), containsString("Metadata:"));
        assertEquals(0, result.exitCode);

        var info = JsonSerialization.mapper.readValue(jsonFile, UpdateCompatibilityCheck.METADATA_TYPE_REF);
        var expectedMeta = defaultMeta();
        expectedMeta.get(DatabaseCompatibilityMetadataProvider.ID).put(DatabaseOptions.DB.getKey(), "postgres");
        info.remove(FeatureCompatibilityMetadataProvider.ID);
        assertEquals(expectedMeta, info);

        result = pseudoLaunch(UpdateCompatibility.NAME, UpdateCompatibilityCheck.NAME, UpdateCompatibilityCheck.INPUT_OPTION_NAME, jsonFile.getAbsolutePath(), "--db", "postgres", "--db-url", "jdbc:postgresql://mypostgres/mydatabase?ssl=false");
        assertEquals(CompatibilityResult.ExitCode.ROLLING.value(), result.exitCode);
    }

    @Test
    public void testDatabaseUrlOptions() throws IOException {
        var jsonFile = createTempFile("compatible", ".json");
        var result = pseudoLaunch(UpdateCompatibility.NAME, UpdateCompatibilityMetadata.NAME, UpdateCompatibilityMetadata.OUTPUT_OPTION_NAME, jsonFile.getAbsolutePath(), "--db-url-host", "localhost", "--db-url-port", "9999", "--db-url-database", "keycloak");
        assertThat(result.getOutString(), containsString("Metadata:"));
        assertEquals(0, result.exitCode);

        // Assert that expected db-url-* options are written to the metadata when --db-url is not present
        var info = JsonSerialization.mapper.readValue(jsonFile, UpdateCompatibilityCheck.METADATA_TYPE_REF);
        var expectedMeta = defaultMeta();
        var dbMeta = expectedMeta.get(DatabaseCompatibilityMetadataProvider.ID);
        dbMeta.putAll(Map.of(
                DatabaseOptions.DB_URL_DATABASE.getKey(), "keycloak",
                DatabaseOptions.DB_URL_HOST.getKey(), "localhost",
                DatabaseOptions.DB_URL_PORT.getKey(), "9999"
        ));
        info.remove(FeatureCompatibilityMetadataProvider.ID);
        assertEquals(expectedMeta, info);

        // Assert that changing to --db-url requires a recreate
        result = pseudoLaunch(UpdateCompatibility.NAME, UpdateCompatibilityCheck.NAME, UpdateCompatibilityCheck.INPUT_OPTION_NAME, jsonFile.getAbsolutePath(), "--db-url", "jdbc:h2:mem:keycloakdb");
        assertEquals(CompatibilityResult.ExitCode.RECREATE.value(), result.exitCode);

        // Assert that db-url-* options are not written to metadata when db-url is present
        result = pseudoLaunch(UpdateCompatibility.NAME, UpdateCompatibilityMetadata.NAME, UpdateCompatibilityMetadata.OUTPUT_OPTION_NAME, jsonFile.getAbsolutePath(), "--db-url", "jdbc:h2:mem:keycloakdb", "--db-url-host", "localhost", "--db-url-port", "9999", "--db-url-database", "keycloak");
        assertThat(result.getOutString(), containsString("Metadata:"));
        assertEquals(0, result.exitCode);

        info = JsonSerialization.mapper.readValue(jsonFile, UpdateCompatibilityCheck.METADATA_TYPE_REF);
        Map<String, String> expectedDbMeta = new HashMap<>();
        expectedDbMeta.put(DatabaseOptions.DB.getKey(), DatabaseOptions.DB.getDefaultValue().get());
        String expectedHash = dbMeta.get(UNSUPPORTED_CHANGES_HASH_KEY);
        if (expectedHash != null) {
            expectedDbMeta.put(UNSUPPORTED_CHANGES_HASH_KEY, expectedHash);
        }
        expectedMeta.put(DatabaseCompatibilityMetadataProvider.ID, expectedDbMeta);

        info.remove(FeatureCompatibilityMetadataProvider.ID);
        assertEquals(expectedMeta, info);

        // Assert that changes to the db-url does not trigger a recreate
        result = pseudoLaunch(UpdateCompatibility.NAME, UpdateCompatibilityCheck.NAME, UpdateCompatibilityCheck.INPUT_OPTION_NAME, jsonFile.getAbsolutePath(), "--db-url", "jdbc:h2:mem:keycloak");
        assertEquals(CompatibilityResult.ExitCode.ROLLING.value(), result.exitCode);
    }

    private static Map<String, Map<String, String>> defaultMeta() {
        Map<String, String> keycloak = new HashMap<>(1);
        keycloak.put("version", Version.VERSION);

        Map<String, String> dbMeta = new HashMap<>();
        dbMeta.put(DatabaseOptions.DB.getKey(), DatabaseOptions.DB.getDefaultValue().get());
        DatabaseCompatibilityMetadataProvider.addUnsupportedDatabaseChanges(dbMeta);

        Map<String, Map<String, String>> m = new HashMap<>();
        m.put(KeycloakCompatibilityMetadataProvider.ID, keycloak);
        m.put(DatabaseCompatibilityMetadataProvider.ID, dbMeta);
        m.put(CacheEmbeddedConfigProviderSpi.SPI_NAME, embeddedCachingMeta());
        m.put(JGroupsCertificateProviderSpi.SPI_NAME, Map.of(
                "enabled", "true"
        ));
        return m;
    }

    private static Map<String, String> embeddedCachingMeta() {
        Map<String, String> m = new HashMap<>();
        m.put("version", majorMinorOf(org.infinispan.commons.util.Version.getVersion()));
        m.put("jgroupsVersion", majorMinorOf(org.jgroups.Version.printVersion()));
        m.put(CONFIG, CONFIG_FILE_NOT_FOUND);
        m.put("actionTokensOwners", "2");
        m.put("loginFailuresOwners", "2");
        m.put("authenticationSessionsOwners", "2");
        return m;
    }
}
