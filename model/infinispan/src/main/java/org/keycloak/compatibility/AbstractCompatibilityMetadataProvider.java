package org.keycloak.compatibility;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.keycloak.Config;


public abstract class AbstractCompatibilityMetadataProvider implements CompatibilityMetadataProvider {

    final String spi;
    final boolean enabled;
    final Config.Scope config;

    public AbstractCompatibilityMetadataProvider(String spi, boolean enabled) {
        this.spi = spi;
        this.enabled = enabled;
        this.config = enabled ? Config.scope(spi, "default") : null;
    }

    @Override
    public Map<String, String> metadata() {
        if (!enabled)
            return Map.of();

        Map<String, String> metadata = new HashMap<>(meta());
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

    protected Map<String, String> meta() {
        return Map.of();
    }

    protected Stream<String> configKeys() {
        return Stream.of();
    }
}
