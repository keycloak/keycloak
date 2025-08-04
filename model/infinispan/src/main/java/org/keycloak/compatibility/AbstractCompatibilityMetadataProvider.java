package org.keycloak.compatibility;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.keycloak.Config;


public abstract class AbstractCompatibilityMetadataProvider implements CompatibilityMetadataProvider {

    final String spi;
    final Config.Scope config;

    public AbstractCompatibilityMetadataProvider(String spi, String providerId) {
        this.spi = spi;
        this.config = Config.scope(spi, providerId);
    }

    abstract protected boolean isEnabled(Config.Scope scope);

    @Override
    public Map<String, String> metadata() {
        if (!isEnabled(config))
            return Map.of();

        Map<String, String> metadata = new HashMap<>(customMeta());
        configKeys().forEach(key -> {
            String value = config.get(key);
            if (value != null)
                metadata.put(key, value);
        });
        return metadata;
    }

    @Override
    public String getId() {
        return spi;
    }

    protected Map<String, String> customMeta() {
        return Map.of();
    }

    protected Stream<String> configKeys() {
        return Stream.of();
    }
}
