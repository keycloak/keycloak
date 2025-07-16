package org.keycloak.infinispan.compatibility;

import java.util.Map;
import java.util.stream.Stream;

import org.keycloak.common.util.MultiSiteUtils;
import org.keycloak.compatibility.AbstractCompatibilityMetadataProvider;
import org.keycloak.infinispan.util.InfinispanUtils;
import org.keycloak.spi.infinispan.CacheRemoteConfigProviderSpi;
import org.keycloak.spi.infinispan.impl.remote.DefaultCacheRemoteConfigProviderFactory;

public class CachingRemoteMetadataProvider extends AbstractCompatibilityMetadataProvider {

    public CachingRemoteMetadataProvider() {
        super(CacheRemoteConfigProviderSpi.SPI_NAME, DefaultCacheRemoteConfigProviderFactory.PROVIDER_ID, InfinispanUtils.isRemoteInfinispan());
    }

    @Override
    public Map<String, String> meta() {
        return Map.of("persistence", Boolean.toString(MultiSiteUtils.isPersistentSessionsEnabled()));
    }

    @Override
    protected Stream<String> configKeys() {
        return Stream.of(DefaultCacheRemoteConfigProviderFactory.HOSTNAME, DefaultCacheRemoteConfigProviderFactory.PORT);
    }
}
