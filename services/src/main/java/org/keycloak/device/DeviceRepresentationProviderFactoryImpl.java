package org.keycloak.device;

import java.util.List;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.binder.cache.CaffeineStatsCounter;
import ua_parser.Client;
import ua_parser.Parser;

public class DeviceRepresentationProviderFactoryImpl implements DeviceRepresentationProviderFactory {

    private static final Parser UA_PARSER = new Parser();
    private static final String CACHE_SIZE = "cacheSize";
    // The max user agent size is 512 bytes and it will take 1024 bytes per cache entry.
    // Using 2MB for caching.
    private static final int DEFAULT_CACHE_SIZE = 2048;

    private volatile LoadingCache<String, Client> cache;

    public static final String PROVIDER_ID = "deviceRepresentation";

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public void init(Config.Scope config) {
        CaffeineStatsCounter metrics = new CaffeineStatsCounter(Metrics.globalRegistry, "userAgent");
        cache = Caffeine.newBuilder()
                .maximumSize(config.getInt(CACHE_SIZE, DEFAULT_CACHE_SIZE))
                .recordStats(() -> metrics)
                .softValues()
                .build(UA_PARSER::parse);
        metrics.registerSizeMetric(cache);
    }

    @Override
    public DeviceRepresentationProvider create(KeycloakSession session) {
        return new DeviceRepresentationProviderImpl(session, cache);
    }

    @Override
    public List<ProviderConfigProperty> getConfigMetadata() {
        return ProviderConfigurationBuilder.create()
                .property()
                .name(CACHE_SIZE)
                .type(ProviderConfigProperty.INTEGER_TYPE)
                .helpText("Sets the maximum number of parsed user-agent values in the local cache.")
                .defaultValue(DEFAULT_CACHE_SIZE)
                .add()
                .build();
    }
}
