package org.keycloak.config;

import org.keycloak.config.database.Database;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class DatabaseOptions {

    public static final Option<String> DB_DIALECT = new OptionBuilder<>("db-dialect", String.class)
            .category(OptionCategory.DATABASE)
            .hidden()
            .buildTime(true)
            .build();

    public static final Option<String> DB_DRIVER = new OptionBuilder<>("db-driver", String.class)
            .category(OptionCategory.DATABASE)
            .description("The fully qualified class name of the JDBC driver. If not set, a default driver is set accordingly to the chosen database.")
            .buildTime(true)
            .build();

    public static final Option<String> DB = new OptionBuilder<>("db", String.class)
            .category(OptionCategory.DATABASE)
            .description("The database vendor. In production mode the default value of 'dev-file' is deprecated, you should explicitly specify the db instead.")
            .defaultValue("dev-file")
            .expectedValues(Database.getDatabaseAliases())
            .buildTime(true)
            .build();

    public static final Option<String> DB_URL = new OptionBuilder<>("db-url", String.class)
            .category(OptionCategory.DATABASE)
            .description("The full database JDBC URL. If not provided, a default URL is set based on the selected database vendor. " +
                    "For instance, if using 'postgres', the default JDBC URL would be 'jdbc:postgresql://localhost/keycloak'. ")
            .build();

    public static final Option<String> DB_URL_HOST = new OptionBuilder<>("db-url-host", String.class)
            .category(OptionCategory.DATABASE)
            .description("Sets the hostname of the default JDBC URL of the chosen vendor. If the `db-url` option is set, this option is ignored.")
            .build();

    public static final Option<String> DB_URL_DATABASE = new OptionBuilder<>("db-url-database", String.class)
            .category(OptionCategory.DATABASE)
            .description("Sets the database name of the default JDBC URL of the chosen vendor. If the `db-url` option is set, this option is ignored.")
            .build();

    public static final Option<Integer> DB_URL_PORT = new OptionBuilder<>("db-url-port", Integer.class)
            .category(OptionCategory.DATABASE)
            .description("Sets the port of the default JDBC URL of the chosen vendor. If the `db-url` option is set, this option is ignored.")
            .build();

    public static final Option<String> DB_URL_PROPERTIES = new OptionBuilder<>("db-url-properties", String.class)
            .category(OptionCategory.DATABASE)
            .description("Sets the properties of the default JDBC URL of the chosen vendor. " +
                    "Make sure to set the properties accordingly to the format expected by the database vendor, as well as appending the right character at the beginning of this property value. " +
                    "If the `db-url` option is set, this option is ignored.")
            .build();

    public static final Option<String> DB_USERNAME = new OptionBuilder<>("db-username", String.class)
            .category(OptionCategory.DATABASE)
            .description("The username of the database user.")
            .build();

    public static final Option<String> DB_PASSWORD = new OptionBuilder<>("db-password", String.class)
            .category(OptionCategory.DATABASE)
            .description("The password of the database user.")
            .build();

    public static final Option<String> DB_SCHEMA = new OptionBuilder<>("db-schema", String.class)
            .category(OptionCategory.DATABASE)
            .description("The database schema to be used.")
            .build();

    public static final Option<Integer> DB_POOL_INITIAL_SIZE = new OptionBuilder<>("db-pool-initial-size", Integer.class)
            .category(OptionCategory.DATABASE)
            .description("The initial size of the connection pool.")
            .build();

    public static final Option<Integer> DB_POOL_MIN_SIZE = new OptionBuilder<>("db-pool-min-size", Integer.class)
            .category(OptionCategory.DATABASE)
            .description("The minimal size of the connection pool.")
            .build();

    public static final Option<Integer> DB_POOL_MAX_SIZE = new OptionBuilder<>("db-pool-max-size", Integer.class)
            .category(OptionCategory.DATABASE)
            .defaultValue(100)
            .description("The maximum size of the connection pool.")
            .build();

    public static final Option<Boolean> DB_SQL_JPA_DEBUG = new OptionBuilder<>("db-debug-jpql", Boolean.class)
            .category(OptionCategory.DATABASE)
            .defaultValue(false)
            .description("Add JPQL information as comments to SQL statements to debug JPA SQL statement generation.")
            .build();

    public static final Option<Integer> DB_SQL_LOG_SLOW_QUERIES = new OptionBuilder<>("db-log-slow-queries-threshold", Integer.class)
            .category(OptionCategory.DATABASE)
            .description("Log SQL statements slower than the configured threshold with logger org.hibernate.SQL_SLOW and log-level info.")
            .defaultValue(10000)
            .build();

    public static final Option<Boolean> DB_ACTIVE_DATASOURCE = new OptionBuilder<>("db-active-<datasource>", Boolean.class)
            .category(OptionCategory.DATABASE_DATASOURCES)
            .defaultValue(true)
            .description("Deactivate specific named datasource <datasource>.")
            .build();

    public static final Option<String> DB_POSTGRESQL_TARGET_SERVER_TYPE = new OptionBuilder<>("db-postgres-target-server-type", String.class)
            .category(OptionCategory.DATABASE)
            .hidden()
            .build();

    /**
     * Options that have their sibling for a named datasource
     * Example: for `db-dialect`, `db-dialect-<datasource>` is created
     */
    public static final List<Option<?>> OPTIONS_DATASOURCES = List.of(
            DatabaseOptions.DB_DIALECT,
            DatabaseOptions.DB_DRIVER,
            DatabaseOptions.DB,
            DatabaseOptions.DB_URL,
            DatabaseOptions.DB_URL_HOST,
            DatabaseOptions.DB_URL_DATABASE,
            DatabaseOptions.DB_URL_PORT,
            DatabaseOptions.DB_URL_PROPERTIES,
            DatabaseOptions.DB_USERNAME,
            DatabaseOptions.DB_PASSWORD,
            DatabaseOptions.DB_SCHEMA,
            DatabaseOptions.DB_POOL_INITIAL_SIZE,
            DatabaseOptions.DB_POOL_MIN_SIZE,
            DatabaseOptions.DB_POOL_MAX_SIZE
    );

    /**
     * In order to avoid ambiguity, we need to have unique option names for wildcard options.
     * This map controls overriding option name to be unique for wildcard option.
     */
    private static final Map<String, String> DATASOURCES_OVERRIDES_SUFFIX = Map.of(
            DatabaseOptions.DB.getKey(), "-kind", // db-kind
            DatabaseOptions.DB_URL.getKey(), "-full"  // db-url-full
    );

    private static final Map<String, Option<?>> cachedDatasourceOptions = new HashMap<>();


    /**
     * Get datasource option containing named datasource mapped to parent DB options.
     * <p>
     * We map DB options to named datasource options like:
     * <ul>
     *     <li>{@code db-url-host --> db-url-host-<datasource>}</li>
     *     <li>{@code db-username --> db-username-<datasource>}</li>
     *     <li>{@code db --> db-kind-<datasource>}</li>
     * </ul>
     */
    @SuppressWarnings("unchecked")
    public static <T> Optional<Option<T>> getDatasourceOption(Option<T> parentOption) {
        if (!OPTIONS_DATASOURCES.contains(parentOption)) {
            return Optional.empty();
        }

        var key = getKeyForDatasource(parentOption);
        if (key.isEmpty()) {
            return Optional.empty();
        }

        // check if we already created the same option and return it from the cache
        Option<?> option = cachedDatasourceOptions.get(key.get());

        if (option == null) {
            var builder = parentOption.toBuilder()
                    .key(key.get())
                    .category(OptionCategory.DATABASE_DATASOURCES);

            if (!parentOption.isHidden()) {
                builder.description("Used for named <datasource>. " + parentOption.getDescription());
            }

            option = builder.build();
            cachedDatasourceOptions.put(key.get(), option);
        }
        return Optional.of((Option<T>) option);
    }

    /**
     * Get mapped datasource key based on DB option {@param option}
     */
    public static Optional<String> getKeyForDatasource(Option<?> option) {
        return getKeyForDatasource(option.getKey());
    }

    /**
     * Get mapped datasource key based on DB option {@param option}
     */
    public static Optional<String> getKeyForDatasource(String option) {
        return Optional.of(option)
                .filter(o -> OPTIONS_DATASOURCES.stream().map(Option::getKey).anyMatch(o::equals))
                .map(key -> key.concat(DATASOURCES_OVERRIDES_SUFFIX.getOrDefault(key, "")))
                .map(key -> key.concat("-<datasource>"));
    }

    /**
     * Returns datasource option based on DB option {@code option} with actual wildcard value.
     * It replaces the {@code <datasource>} with actual value in {@code namedProperty}.
     * <p>
     * f.e. Consider {@code option}={@link DatabaseOptions#DB_DRIVER}, and {@code namedProperty}=my-store.
     * <p>
     * Result: {@code db-driver-my-store}
     */
    public static Optional<String> getResultNamedKey(Option<?> option, String namedProperty) {
        return getKeyForDatasource(option)
                .map(key -> key.substring(0, key.indexOf("<")))
                .map(key -> key.concat(namedProperty));
    }
}
