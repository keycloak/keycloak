package org.keycloak.config;

import java.io.File;
import java.util.Arrays;

import org.keycloak.config.database.Database;

import static org.keycloak.config.OptionsUtil.DURATION_DESCRIPTION;

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
    
    public static final Option<String> DB_KIND = new OptionBuilder<>("db-kind-<datasource>", String.class)
            .category(OptionCategory.DATABASE_DATASOURCES)
            .description("Used for named <datasource>. The database vendor.")
            .expectedValues(Database.getDatabaseAliases())
            .connectedOptions(TransactionOptions.TRANSACTION_XA_ENABLED_DATASOURCE)
            .buildTime(true)
            .build();

    public static final Option<String> DB = new OptionBuilder<>("db", String.class)
            .category(OptionCategory.DATABASE)
            .description("The database vendor. In production mode the default value of 'dev-file' is deprecated, you should explicitly specify the db instead.")
            .defaultValue("dev-file")
            .expectedValues(Database.getDatabaseAliases())
            .wildcardKey(DB_KIND.getKey())
            .buildTime(true)
            .build();

    public static final Option<String> DB_URL = new OptionBuilder<>("db-url", String.class)
            .category(OptionCategory.DATABASE)
            .description("The full database JDBC URL. If not provided, a default URL is set based on the selected database vendor. " +
                    "For instance, if using 'postgres', the default JDBC URL would be 'jdbc:postgresql://localhost/keycloak'. ")
            .wildcardKey("db-url-full-<datasource>")
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

    public static final Option<String> DB_CONNECT_TIMEOUT = new OptionBuilder<>("db-connect-timeout", String.class)
            .category(OptionCategory.DATABASE)
            .description("Sets the JDBC driver connection timeout and login timeout. " + DURATION_DESCRIPTION)
            .defaultValue("10s")
            .build();

    public static final Option<String> DB_POOL_ACQUISITION_TIMEOUT = new OptionBuilder<>("db-pool-acquisition-timeout", String.class)
            .category(OptionCategory.DATABASE)
            .hidden()
            .build();
    public static final Option<String> DB_TLS_MODE = new OptionBuilder<>("db-tls-mode", String.class)
            .category(OptionCategory.DATABASE)
            .expectedValues(Arrays.stream(DatabaseTlsMode.values()).map(DatabaseTlsMode::toCliValue).toList())
            .defaultValue(DatabaseTlsMode.DISABLED.toCliValue())
            .description("Sets the TLS mode for the database connection. If disabled, it uses the driver's default value. When set to verify-server, it enables encryption and server identity verification. The database server certificate or Certificate Authority (CA) certificate is required.")
            .build();

    public static final Option<File> DB_TLS_TRUST_STORE_FILE = new OptionBuilder<>("db-tls-trust-store-file", File.class)
            .category(OptionCategory.DATABASE)
            .description("The path to the truststore file containing the database server certificates or Certificate Authority (CA) certificates used to verify the database server's identity.")
            .build();

    public static final Option<String> DB_TLS_TRUST_STORE_PASSWORD = new OptionBuilder<>("db-tls-trust-store-password", String.class)
            .category(OptionCategory.DATABASE)
            .description("The password to access the truststore file specified in db-tls-trust-store-file (if required and supported by the JDBC driver).")
            .build();
    public static final Option<String> DB_TLS_TRUST_STORE_TYPE = new OptionBuilder<>("db-tls-trust-store-type", String.class)
            .category(OptionCategory.DATABASE)
            .description("The type of the truststore file. Common values include 'JKS' (Java KeyStore) and 'PKCS12'. If not specified, it uses the driver's default.")
            .build();

    // mTLS keystore options
    public static final Option<File> DB_MTLS_KEY_STORE_FILE = new OptionBuilder<>("db-mtls-key-store-file", File.class)
            .category(OptionCategory.DATABASE)
            .description("The path to the keystore file containing the client certificate and private key used for mTLS authentication with the database server.")
            .build();

    public static final Option<String> DB_MTLS_KEY_STORE_PASSWORD = new OptionBuilder<>("db-mtls-key-store-password", String.class)
            .category(OptionCategory.DATABASE)
            .description("The password to access the keystore file specified in db-mtls-key-store-file.")
            .build();

    public static final Option<String> DB_MTLS_KEY_STORE_TYPE = new OptionBuilder<>("db-mtls-key-store-type", String.class)
            .category(OptionCategory.DATABASE)
            .description("The type of the keystore file. Common values include 'JKS' (Java KeyStore) and 'PKCS12'. If not specified, it uses the driver's default.")
            .build();

    public static class Datasources {

        /**
         * Get datasource option containing named datasource mapped to parent DB options.
         * <p>
         * We map DB options to named datasource options like:
         * <ul>
         *     <li>{@code db-url-host --> db-url-host-<datasource>}</li>
         *     <li>{@code db-username --> db-username-<datasource>}</li>
         * </ul>
         */
        @SuppressWarnings("unchecked")
        protected static <T> Option<T> getDatasourceOption(Option<T> parentOption) {
            var key = parentOption.getWildcardKey().orElse(parentOption.getKey().concat("-<datasource>"));
            var builder = parentOption.toBuilder()
                    .key(key)
                    .category(OptionCategory.DATABASE_DATASOURCES);

            if (!parentOption.isHidden()) {
                builder.description("Used for named <datasource>. " + parentOption.getDescription());
            }

            Option<?> option = builder.build();
            parentOption.setWildcardKey(option.getKey());
            return (Option<T>)option;
        }
    }

    public enum DatabaseTlsMode {
        DISABLED,
        VERIFY_SERVER;

        public String toCliValue() {
            return name().toLowerCase().replace('_', '-');
        }

        public static DatabaseTlsMode fromCliValue(String value) {
            return valueOf(value.toUpperCase().replace('-', '_'));
        }
    }
}
