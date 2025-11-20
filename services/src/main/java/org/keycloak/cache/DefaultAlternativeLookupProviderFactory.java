package org.keycloak.cache;

import java.util.concurrent.TimeUnit;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.binder.cache.CaffeineStatsCounter;

public class DefaultAlternativeLookupProviderFactory implements AlternativeLookupProviderFactory {

    private Cache<String, String> lookupCache;

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

        CaffeineStatsCounter metrics = new CaffeineStatsCounter(Metrics.globalRegistry, "lookup");

        this.lookupCache = Caffeine.newBuilder()
                .maximumSize(maximumSize)
                .expireAfterAccess(expireAfter, TimeUnit.MINUTES)
                .recordStats(() -> metrics)
                .build();

        metrics.registerSizeMetric(lookupCache);
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public void close() {
        lookupCache.cleanUp();
        lookupCache = null;
    }

}
