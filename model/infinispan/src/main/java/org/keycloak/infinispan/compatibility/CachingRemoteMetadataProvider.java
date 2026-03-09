package org.keycloak.infinispan.compatibility;

import java.util.stream.Stream;

import org.keycloak.Config;
import org.keycloak.compatibility.AbstractCompatibilityMetadataProvider;
import org.keycloak.compatibility.CompatibilityMetadataProvider;
import org.keycloak.infinispan.util.InfinispanUtils;
import org.keycloak.spi.infinispan.CacheRemoteConfigProviderSpi;
import org.keycloak.spi.infinispan.impl.remote.DefaultCacheRemoteConfigProviderFactory;

import com.google.auto.service.AutoService;

@AutoService(CompatibilityMetadataProvider.class)
public class CachingRemoteMetadataProvider extends AbstractCompatibilityMetadataProvider {

    public CachingRemoteMetadataProvider() {
        super(CacheRemoteConfigProviderSpi.SPI_NAME, DefaultCacheRemoteConfigProviderFactory.PROVIDER_ID);
    }

    @Override
    protected boolean isEnabled(Config.Scope scope) {
        return InfinispanUtils.isRemoteInfinispan();
    }

    @Override
    protected Stream<String> configKeys() {
        return Stream.of(DefaultCacheRemoteConfigProviderFactory.HOSTNAME, DefaultCacheRemoteConfigProviderFactory.PORT);
    }
}
