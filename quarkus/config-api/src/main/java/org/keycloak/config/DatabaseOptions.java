package org.keycloak.config;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import org.keycloak.config.database.Database;

import static org.keycloak.config.OptionsUtil.DURATION_DESCRIPTION;
import static org.keycloak.config.WildcardOptionsUtil.getWildcardNamedKey;

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

    public static final Option<String> DB_POOL_MAX_LIFETIME = new OptionBuilder<>("db-pool-max-lifetime", String.class)
            .category(OptionCategory.DATABASE)
            .description("The maximum time a connection remains in the pool, after which it will be closed upon return and replaced as necessary. " + DURATION_DESCRIPTION)
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

    public static final Option<Boolean> DB_ENABLED_DATASOURCE = new OptionBuilder<>("db-enabled-<datasource>", Boolean.class)
            .category(OptionCategory.DATABASE_DATASOURCES)
            .defaultValue(true)
            .description("If the named datasource <datasource> should be enabled at runtime.")
            .build();

    public static final Option<String> DB_POSTGRESQL_TARGET_SERVER_TYPE = new OptionBuilder<>("db-postgres-target-server-type", String.class)
            .category(OptionCategory.DATABASE)
            .hidden()
            .build();

    public static final class Datasources {
        /**
         * Options that have their sibling for a named datasource
         * Example: for `db-dialect`, `db-dialect-<datasource>` is created
         */
        public static final List<Option<?>> OPTIONS_DATASOURCES = List.of(
                DB_DIALECT,
                DB_DRIVER,
                DB,
                DB_URL,
                DB_URL_HOST,
                DB_URL_DATABASE,
                DB_URL_PORT,
                DB_URL_PROPERTIES,
                DB_USERNAME,
                DB_PASSWORD,
                DB_SCHEMA,
                DB_POOL_INITIAL_SIZE,
                DB_POOL_MIN_SIZE,
                DB_POOL_MAX_SIZE,
                DB_SQL_JPA_DEBUG,
                DB_SQL_LOG_SLOW_QUERIES
        );

        /**
         * In order to avoid ambiguity, we need to have unique option names for wildcard options.
         * This map controls overriding option name to be unique for wildcard option.
         */
        private static final Map<String, String> DATASOURCES_OVERRIDES_SUFFIX = Map.of(
                DatabaseOptions.DB.getKey(), "-kind", // db-kind
                DatabaseOptions.DB_URL.getKey(), "-full"  // db-url-full
        );

        /**
         * You can override some {@link OptionBuilder} methods for additional datasources in this map
         */
        private static final Map<Option<?>, Consumer<OptionBuilder<?>>> DATASOURCES_OVERRIDES_OPTIONS = Map.of(
                DatabaseOptions.DB, builder -> builder
                        .defaultValue(Optional.empty()) // no default value for DB kind for datasources
                        .connectedOptions(
                                getDatasourceOption(DatabaseOptions.DB_URL).orElseThrow(),
                                TransactionOptions.TRANSACTION_XA_ENABLED_DATASOURCE
                        )
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

                // override some settings for options
                var override = DATASOURCES_OVERRIDES_OPTIONS.get(parentOption);
                if (override != null) {
                    override.accept(builder);
                }

                option = builder.build();
                parentOption.setWildcardKey(option.getKey());
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
        public static Optional<String> getNamedKey(Option<?> option, String namedProperty) {
            return getKeyForDatasource(option).map(key -> getWildcardNamedKey(key, namedProperty));
        }
    }
}
