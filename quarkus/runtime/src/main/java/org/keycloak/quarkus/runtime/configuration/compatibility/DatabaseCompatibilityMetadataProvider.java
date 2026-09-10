package org.keycloak.quarkus.runtime.configuration.compatibility;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.keycloak.compatibility.CompatibilityMetadataProvider;
import org.keycloak.config.DatabaseOptions;
import org.keycloak.config.Option;
import org.keycloak.jose.jws.crypto.HashUtils;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.smallrye.config.ConfigValue;
import org.jboss.logging.Logger;

import static org.keycloak.quarkus.runtime.configuration.Configuration.getConfigValue;
import static org.keycloak.quarkus.runtime.configuration.Configuration.getOptionalKcValue;

public class DatabaseCompatibilityMetadataProvider implements CompatibilityMetadataProvider {

    private static final Logger log = Logger.getLogger(DatabaseCompatibilityMetadataProvider.class);

    public static final String ID = "database";
    public static final String UNSUPPORTED_CHANGES_HASH_KEY = "unsupported-changeset-hash";

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
        addUnsupportedDatabaseChanges(metadata);
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

    public record JsonParent(Collection<ChangeSet> changeSets, Collection<Migration> migrations) {
    }

    record ChangeSet(String id, String author, String filename) {
    }

    record Migration(@JsonProperty("class") String clazz) {
    }

    public static void addUnsupportedDatabaseChanges(Map<String, String> metadata) {
        try {
            // Load JSON into memory and write to a JSON String in order to avoid whitespace changes impacting the hash
            Enumeration<URL> resources = DatabaseCompatibilityMetadataProvider.class.getClassLoader().getResources("/META-INF/rolling-upgrades-unsupported-changes.json");
            Set<ChangeSet> changeSets = new HashSet<>();
            Set<Migration> migrations = new HashSet<>();

            ObjectMapper objectMapper = new ObjectMapper();
            while(resources.hasMoreElements()) {
                URL url = resources.nextElement();
                try (InputStream inputStream = url.openStream()) {
                    JsonParent parent = objectMapper.readValue(inputStream, new TypeReference<>() {});
                    changeSets.addAll(parent.changeSets);
                    migrations.addAll(parent.migrations);
                }
            }

            if (!changeSets.isEmpty()) {
                List<ChangeSet> sortedChanges = changeSets.stream().sorted(
                      Comparator.comparing(ChangeSet::id)
                            .thenComparing(ChangeSet::author)
                            .thenComparing(ChangeSet::filename)
                ).toList();

                List<Migration> sortedMigrations = migrations.stream()
                      .sorted(Comparator.comparing(Migration::clazz))
                      .toList();

                JsonParent parent = new JsonParent(sortedChanges, sortedMigrations);
                String changeSetJson = objectMapper.writeValueAsString(parent);
                String hash = HashUtils.sha256UrlEncodedHash(changeSetJson, StandardCharsets.UTF_8);
                metadata.put(UNSUPPORTED_CHANGES_HASH_KEY, hash);
            }
        } catch (IOException e) {
            log.error("Unable to close InputStream when creating database unsupported change hash", e);
        }
    }
}
