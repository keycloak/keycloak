package org.keycloak.infinispan.compatibility;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.keycloak.Config;
import org.keycloak.common.Profile;
import org.keycloak.compatibility.AbstractCompatibilityMetadataProvider;
import org.keycloak.crypto.JavaAlgorithm;
import org.keycloak.infinispan.util.InfinispanUtils;
import org.keycloak.jose.jws.crypto.HashUtils;
import org.keycloak.spi.infinispan.impl.embedded.CacheConfigurator;

import org.infinispan.commons.util.Version;
import org.infinispan.configuration.cache.HashConfiguration;

import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.CLUSTERED_CACHE_NUM_OWNERS;
import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.USER_AND_CLIENT_SESSION_CACHES;
import static org.keycloak.spi.infinispan.CacheEmbeddedConfigProviderSpi.SPI_NAME;
import static org.keycloak.spi.infinispan.impl.embedded.DefaultCacheEmbeddedConfigProviderFactory.CONFIG;
import static org.keycloak.spi.infinispan.impl.embedded.DefaultCacheEmbeddedConfigProviderFactory.PROVIDER_ID;
import static org.keycloak.spi.infinispan.impl.embedded.DefaultCacheEmbeddedConfigProviderFactory.STACK;

public class CachingEmbeddedMetadataProvider extends AbstractCompatibilityMetadataProvider {

    public static final String CONFIG_FILE_NOT_FOUND = "not_found";

    public CachingEmbeddedMetadataProvider() {
        super(SPI_NAME, PROVIDER_ID);
    }

    @Override
    protected boolean isEnabled(Config.Scope scope) {
        return InfinispanUtils.isEmbeddedInfinispan();
    }

    @Override
    public Map<String, String> customMeta() {
        var meta = new HashMap<String, String>(8);
        var defaultNumOwners = HashConfiguration.NUM_OWNERS.getDefaultValue();
        Arrays.stream(CLUSTERED_CACHE_NUM_OWNERS)
                .map(CacheConfigurator::numOwnerConfigKey)
                .forEach(configKey -> addInt(meta, configKey, defaultNumOwners));
        if (Profile.isFeatureEnabled(Profile.Feature.PERSISTENT_USER_SESSIONS)) {
            // persistent user sessions always force num_owners=1.
            // the spi option is ignored
            Arrays.stream(USER_AND_CLIENT_SESSION_CACHES)
                    .map(CacheConfigurator::numOwnerConfigKey)
                    .forEach(meta::remove);
        }
        meta.put("version", majorMinorOf(Version.getVersion()));
        meta.put("jgroupsVersion", majorMinorOf(org.jgroups.Version.printVersion()));
        meta.put(CONFIG, sha256Of(Paths.get(config.get(CONFIG))));
        return meta;
    }
    @Override
    public Stream<String> configKeys() {
        return Stream.of(STACK);
    }

    private void addInt(Map<String, String> meta, String configKey, int defaultValue) {
        Optional.ofNullable(config.getInt(configKey, defaultValue))
                .map(String::valueOf)
                .ifPresent(value -> meta.put(configKey, value));
    }

    public static String sha256Of(Path filePath) {
        try {
            var hash = HashUtils.hash(JavaAlgorithm.SHA256, Files.readAllBytes(filePath));
            return Base64.getEncoder().encodeToString(hash);
        } catch (IOException e) {
            return CONFIG_FILE_NOT_FOUND;
        }
    }

    public static String majorMinorOf(String version) {
        if (version == null || version.isEmpty()) {
            return version;
        }
        // Pattern to grab only the "Major.Minor" (e.g., 16.0)
        Matcher matcher = Pattern.compile("^(\\d+\\.\\d+)").matcher(version);
        return matcher.find() ? matcher.group(1) : version;
    }
}
