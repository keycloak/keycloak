package org.keycloak.cache;

import java.time.Duration;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

public class DefaultAlternativeLookupProviderFactory implements AlternativeLookupProviderFactory {

    private LocalCacheConfiguration<String, String> cacheConfig;
    private LocalCache<String, String> lookupCache;

    @Override
    public String getId() {
        return "default";
    }

    @Override
    public AlternativeLookupProvider create(KeycloakSession session) {
        return new DefaultAlternativeLookupProvider(lookupCache);
    }

    @Override
    public void init(Config.Scope config) {
        Integer maximumSize = config.getInt("maximumSize", 1000);
        Integer expireAfter = config.getInt("expireAfter", 60);

        cacheConfig = LocalCacheConfiguration.<String, String>builder()
              .name("lookup")
              .expiration(Duration.ofMinutes(expireAfter))
              .maxSize(maximumSize)
              .build();
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        try (KeycloakSession session = factory.create()) {
            lookupCache = session.getProvider(LocalCacheProvider.class).create(cacheConfig);
            cacheConfig = null;
        }
    }

    @Override
    public void close() {
        if (lookupCache != null) {
            lookupCache.close();
            lookupCache = null;
        }
    }
}
