package org.keycloak.quarkus.runtime.configuration.compatibility;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.keycloak.compatibility.CompatibilityMetadataProvider;
import org.keycloak.config.DatabaseOptions;
import org.keycloak.config.Option;
import org.keycloak.jose.jws.crypto.HashUtils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.smallrye.config.ConfigValue;
import org.jboss.logging.Logger;

import static org.keycloak.quarkus.runtime.configuration.Configuration.getConfigValue;
import static org.keycloak.quarkus.runtime.configuration.Configuration.getOptionalKcValue;

public class DatabaseCompatibilityMetadataProvider implements CompatibilityMetadataProvider {

    private static final Logger log = Logger.getLogger(DatabaseCompatibilityMetadataProvider.class);

    public static final String ID = "database";
    public static final String UNSUPPORTED_CHANGE_SET_HASH_KEY = "unsupported-changeset-hash";

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

        ObjectMapper objectMapper = new ObjectMapper();
        try (InputStream inputStream = DatabaseCompatibilityMetadataProvider.class.getResourceAsStream("/META-INF/rolling-upgrades-unsupported-changes.json")) {
            if (inputStream != null) {
                // Load the ChangeSet JSON into memory and write to a JSON String in order to avoid whitespace changes impacting the hash
                Set<ChangeSet> changeSets = objectMapper.readValue(inputStream, new TypeReference<>() {});
                List<ChangeSet> sortedChanges = changeSets.stream().sorted(
                      Comparator.comparing(ChangeSet::id)
                            .thenComparing(ChangeSet::author)
                            .thenComparing(ChangeSet::filename)
                ).toList();

                String changeSetJson = objectMapper.writeValueAsString(sortedChanges);
                String hash = HashUtils.sha256UrlEncodedHash(changeSetJson, StandardCharsets.UTF_8);
                metadata.put(UNSUPPORTED_CHANGE_SET_HASH_KEY, hash);
            }
        } catch (IOException e) {
            log.error("Unable to close InputStream when creating database unsupported change hash", e);
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

    public record ChangeSet(String id, String author, String filename) {
    }
}
