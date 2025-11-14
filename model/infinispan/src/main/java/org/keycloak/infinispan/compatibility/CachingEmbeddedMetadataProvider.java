package org.keycloak.infinispan.compatibility;

import java.util.Map;
import java.util.stream.Stream;

import org.keycloak.Config;
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
        return Map.of(
              "version", Version.getVersion(),
              "jgroupsVersion", org.jgroups.Version.printVersion()
        );
    }

    @Override
    public Stream<String> configKeys() {
        return Stream.of(DefaultCacheEmbeddedConfigProviderFactory.CONFIG, DefaultCacheEmbeddedConfigProviderFactory.STACK);
    }
}
