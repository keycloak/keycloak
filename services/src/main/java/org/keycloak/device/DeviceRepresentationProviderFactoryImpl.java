package org.keycloak.device;

import java.util.List;

import org.keycloak.Config;
import org.keycloak.cache.LocalCache;
import org.keycloak.cache.LocalCacheConfiguration;
import org.keycloak.cache.LocalCacheProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;

import ua_parser.Client;
import ua_parser.Parser;

public class DeviceRepresentationProviderFactoryImpl implements DeviceRepresentationProviderFactory {

    private static final Parser UA_PARSER = new Parser();
    private static final String CACHE_SIZE = "cacheSize";
    // The max user agent size is 512 bytes and it will take 1024 bytes per cache entry.
    // Using 2MB for caching.
    private static final int DEFAULT_CACHE_SIZE = 2048;
    public static final String PROVIDER_ID = "deviceRepresentation";

    private LocalCacheConfiguration<String, Client> cacheConfig;
    private LocalCache<String, Client> cache;

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public void init(Config.Scope config) {
        cacheConfig = LocalCacheConfiguration.<String, Client>builder()
              .name("userAgent")
              .maxSize(config.getInt(CACHE_SIZE, DEFAULT_CACHE_SIZE))
              .loader(UA_PARSER::parse)
              .build();
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        try (KeycloakSession session = factory.create()) {
            cache = session.getProvider(LocalCacheProvider.class).create(cacheConfig);
            cacheConfig = null;
        }
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

    @Override
    public void close() {
        if (cache != null) {
            cache.close();
            cache = null;
        }
    }
}
