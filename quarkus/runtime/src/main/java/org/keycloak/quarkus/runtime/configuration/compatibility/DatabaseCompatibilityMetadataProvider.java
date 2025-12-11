package org.keycloak.quarkus.runtime.configuration.compatibility;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.keycloak.compatibility.CompatibilityMetadataProvider;
import org.keycloak.config.DatabaseOptions;
import org.keycloak.config.Option;

import io.smallrye.config.ConfigValue;

import static org.keycloak.quarkus.runtime.configuration.Configuration.getConfigValue;
import static org.keycloak.quarkus.runtime.configuration.Configuration.getOptionalKcValue;

public class DatabaseCompatibilityMetadataProvider implements CompatibilityMetadataProvider {

    public static final String ID = "database";

    @Override
    public Map<String, String> metadata() {
        Map<String, String> metadata = new HashMap<>();
        metadata.put(DatabaseOptions.DB.getKey(), getConfigValue(DatabaseOptions.DB).getValue());
        addOptional(DatabaseOptions.DB_SCHEMA, metadata);

        // Only track DB_URL_* properties if the user has not explicitly configured a DB_URL
        ConfigValue dbUrl = getConfigValue(DatabaseOptions.DB_URL);
        if (!dbUrl.getValue().equals(dbUrl.getRawValue())) {
            addOptional(DatabaseOptions.DB_URL_HOST, metadata);
            addOptional(DatabaseOptions.DB_URL_PORT, metadata);
            addOptional(DatabaseOptions.DB_URL_DATABASE, metadata);
        }
        return metadata;
    }

    void addOptional(Option<?> option, Map<String, String> metadata) {
        Optional<String> optional = getOptionalKcValue(option.getKey());
        optional.ifPresent(opt -> metadata.put(option.getKey(), opt));
    }

    @Override
    public String getId() {
        return ID;
    }
}
