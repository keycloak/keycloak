package org.keycloak.infinispan.compatibility;

import java.util.Map;
import java.util.stream.Stream;

import org.keycloak.Config;
import org.keycloak.common.Profile;
import org.keycloak.compatibility.AbstractCompatibilityMetadataProvider;
import org.keycloak.infinispan.util.InfinispanUtils;
import org.keycloak.spi.infinispan.CacheEmbeddedConfigProviderSpi;
import org.keycloak.spi.infinispan.impl.embedded.DefaultCacheEmbeddedConfigProviderFactory;

import org.infinispan.commons.util.Version;

public class CachingEmbeddedMetadataProvider extends AbstractCompatibilityMetadataProvider {

    public CachingEmbeddedMetadataProvider() {
        super(CacheEmbeddedConfigProviderSpi.SPI_NAME, DefaultCacheEmbeddedConfigProviderFactory.PROVIDER_ID);
    }

    @Override
    protected boolean isEnabled(Config.Scope scope) {
        return InfinispanUtils.isEmbeddedInfinispan();
    }

    @Override
    public Map<String, String> customMeta() {
        String rawIspnVersion = Version.getVersion();
        String rawJgroupsVersion = org.jgroups.Version.printVersion();
        if (Profile.isAnyVersionOfFeatureEnabled(Profile.Feature.ROLLING_UPDATES_V2)) {
            rawIspnVersion = majorMinorOf(rawIspnVersion);
            rawJgroupsVersion = majorMinorOf(rawJgroupsVersion);
        }
        return Map.of(
                "version", rawIspnVersion,
                "jgroupsVersion", rawJgroupsVersion
        );
    }
    @Override
    public Stream<String> configKeys() {
        return Stream.of(DefaultCacheEmbeddedConfigProviderFactory.CONFIG, DefaultCacheEmbeddedConfigProviderFactory.STACK);
    }

    private String majorMinorOf(String version) {
        if (version == null || version.isEmpty()) {
            return version;
        }
        // Pattern to grab only the "Major.Minor" (e.g., 16.0)
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("^(\\d+\\.\\d+)").matcher(version);
        return matcher.find() ? matcher.group(1) : version;
    }
}
