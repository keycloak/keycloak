package org.keycloak.infinispan.compatibility;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.keycloak.Config;
import org.keycloak.compatibility.AbstractCompatibilityMetadataProvider;
import org.keycloak.crypto.JavaAlgorithm;
import org.keycloak.infinispan.util.InfinispanUtils;
import org.keycloak.jose.jws.crypto.HashUtils;

import org.infinispan.commons.util.Version;

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
        return Map.of(
                "version", majorMinorOf(Version.getVersion()),
                "jgroupsVersion", majorMinorOf(org.jgroups.Version.printVersion()),
                CONFIG, sha256Of(Paths.get(config.get(CONFIG)))
        );
    }
    @Override
    public Stream<String> configKeys() {
        return Stream.of(STACK);
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
