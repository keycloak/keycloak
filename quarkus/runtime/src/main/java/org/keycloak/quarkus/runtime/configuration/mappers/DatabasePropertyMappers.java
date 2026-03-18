package org.keycloak.quarkus.runtime.configuration.mappers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.keycloak.config.CachingOptions;
import org.keycloak.config.CachingOptions.Stack;
import org.keycloak.config.DatabaseOptions;
import org.keycloak.config.Option;
import org.keycloak.config.OptionBuilder;
import org.keycloak.config.TransactionOptions;
import org.keycloak.config.database.Database;
import org.keycloak.quarkus.runtime.cli.Picocli;
import org.keycloak.quarkus.runtime.cli.PropertyException;
import org.keycloak.quarkus.runtime.configuration.Configuration;
import org.keycloak.utils.StringUtil;

import io.quarkus.datasource.common.runtime.DatabaseKind;
import io.smallrye.config.ConfigSourceInterceptorContext;
import io.smallrye.config.ConfigValue;
import org.jboss.logging.Logger;

import static org.keycloak.config.DatabaseOptions.DB;
import static org.keycloak.config.DatabaseOptions.DB_ORACLE_TLS_TRANSPORT;
import static org.keycloak.config.DatabaseOptions.DB_POOL_INITIAL_SIZE;
import static org.keycloak.config.DatabaseOptions.DB_POOL_MAX_SIZE;
import static org.keycloak.config.DatabaseOptions.DB_PROPERTY_ENCRYPT;
import static org.keycloak.config.DatabaseOptions.DB_PROPERTY_ORACLE_TRUST_STORE;
import static org.keycloak.config.DatabaseOptions.DB_PROPERTY_ORACLE_TRUST_STORE_PASSWORD;
import static org.keycloak.config.DatabaseOptions.DB_PROPERTY_ORACLE_TRUST_STORE_TYPE;
import static org.keycloak.config.DatabaseOptions.DB_PROPERTY_SERVER_SSL_CERT;
import static org.keycloak.config.DatabaseOptions.DB_PROPERTY_SSLFACTORY;
import static org.keycloak.config.DatabaseOptions.DB_PROPERTY_SSLROOTCERT;
import static org.keycloak.config.DatabaseOptions.DB_PROPERTY_SSL_SERVER_DN_MATCH;
import static org.keycloak.config.DatabaseOptions.DB_PROPERTY_TRUST_CERTIFICATE_KEY_STORE_PASSWORD;
import static org.keycloak.config.DatabaseOptions.DB_PROPERTY_TRUST_CERTIFICATE_KEY_STORE_URL;
import static org.keycloak.config.DatabaseOptions.DB_PROPERTY_TRUST_SERVER_CERTIFICATE;
import static org.keycloak.config.DatabaseOptions.DB_PROPERTY_TRUST_STORE;
import static org.keycloak.config.DatabaseOptions.DB_PROPERTY_TRUST_STORE_PASSWORD;
import static org.keycloak.config.DatabaseOptions.DB_TLS_MODE;
import static org.keycloak.config.DatabaseOptions.DB_TLS_TRUST_STORE_FILE;
import static org.keycloak.config.DatabaseOptions.DB_TLS_TRUST_STORE_PASSWORD;
import static org.keycloak.config.DatabaseOptions.DB_TLS_TRUST_STORE_TYPE;
import static org.keycloak.config.DatabaseOptions.DB_URL;
import static org.keycloak.config.DatabaseOptions.Datasources.OPTIONS_DATASOURCES;
import static org.keycloak.config.DatabaseOptions.Datasources.getDatasourceOption;
import static org.keycloak.config.DatabaseOptions.Datasources.getKeyForDatasource;
import static org.keycloak.config.DatabaseOptions.Datasources.getNamedKey;
import static org.keycloak.quarkus.runtime.configuration.Configuration.getOptionalKcValue;
import static org.keycloak.quarkus.runtime.configuration.MicroProfileConfigProvider.NS_KEYCLOAK_PREFIX;
import static org.keycloak.quarkus.runtime.configuration.mappers.DatabasePropertyMappers.Datasources.appendDatasourceMappers;
import static org.keycloak.quarkus.runtime.configuration.mappers.PropertyMapper.fromOption;

public final class DatabasePropertyMappers implements PropertyMapperGrouping {
    public static final String PG_TARGET_SERVER_TYPE = "quarkus.datasource.jdbc.additional-jdbc-properties.targetServerType";
    public static final String MSSQL_SEND_STRING_PARAMETER_AS_UNICODE = "quarkus.datasource.jdbc.additional-jdbc-properties.sendStringParametersAsUnicode";
    public static final String MYSQL_CONNECT_TIMEOUT = "quarkus.datasource.jdbc.additional-jdbc-properties.connectTimeout";
    public static final String MARIADB_CONNECT_TIMEOUT = "quarkus.datasource.jdbc.additional-jdbc-properties.connectTimeout";
    public static final String ORACLEDB_CONNECT_TIMEOUT = "quarkus.datasource.jdbc.additional-jdbc-properties.oracle.net.CONNECT_TIMEOUT";
    public static final String MSSQL_CONNECT_TIMEOUT = "quarkus.datasource.jdbc.additional-jdbc-properties.loginTimeout";
    private static final String POSTGRES_CONNECT_TIMEOUT = "quarkus.datasource.jdbc.additional-jdbc-properties.connectTimeout";
    private static final String TIDB_CONNECT_TIMEOUT = "quarkus.datasource.jdbc.additional-jdbc-properties.connectTimeout";

    private static final Logger log = Logger.getLogger(DatabasePropertyMappers.class);

    /**
     * Minimum {@code db-pool-max-size} required for {@link Stack#jdbc_ping} and {@link Stack#jdbc_ping_udp}.
     * Determined experimentally — lower values cause startup failures due to connection pool exhaustion.
     * Verified by {@code KeycloakDeploymentTest#testDocumentedMinimalPoolMaxSizeWorks}.
     */
    private static final int JDBC_PING_MIN_POOL_MAX_SIZE = 4;

    @Override
    public List<PropertyMapper<?>> getPropertyMappers() {
        List<PropertyMapper<?>> mappers = List.of(
                fromOption(DatabaseOptions.DB_DIALECT)
                        .mapFrom(DB, DatabasePropertyMappers::transformDialect)
                        .build(),
                fromOption(DatabaseOptions.DB_DRIVER)
                        .mapFrom(DB, DatabasePropertyMappers::getXaOrNonXaDriver)
                        .to("quarkus.datasource.jdbc.driver")
                        .paramLabel("driver")
                        .build(),
                fromOption(DB)
                        .to("quarkus.datasource.db-kind")
                        .transformer(DatabasePropertyMappers::toDatabaseKind)
                        .paramLabel("vendor")
                        .build(),
                fromOption(DatabaseOptions.DB_URL)
                        .to("quarkus.datasource.jdbc.url")
                        .mapFrom(DB, DatabasePropertyMappers::getDatabaseUrl)
                        .paramLabel("jdbc-url")
                        .build(),
                fromOption(DatabaseOptions.DB_POSTGRESQL_TARGET_SERVER_TYPE)
                        .to(PG_TARGET_SERVER_TYPE)
                        .isEnabled(DatabasePropertyMappers::isPostgresqlTargetServerTypeEnabled)
                        .build(),
                fromOption(DatabaseOptions.DB_MSSQL_SEND_STRING_PARAMETER_AS_UNICODE)
                        .to(MSSQL_SEND_STRING_PARAMETER_AS_UNICODE)
                        .isEnabled(DatabasePropertyMappers::isMssqlSendStringParametersAsUnicode)
                        .build(),
                fromOption(DatabaseOptions.DB_MYSQL_CONNECT_TIMEOUT)
                        .to(MYSQL_CONNECT_TIMEOUT)
                        .isEnabled(DatabasePropertyMappers::isMysqlConnectTimeoutEnabled)
                        .build(),
                fromOption(DatabaseOptions.DB_MARIADB_CONNECT_TIMEOUT)
                        .to(MARIADB_CONNECT_TIMEOUT)
                        .isEnabled(DatabasePropertyMappers::isMariadbConnectTimeoutEnabled)
                        .build(),
                fromOption(DatabaseOptions.DB_ORACLE_CONNECT_TIMEOUT)
                        .to(ORACLEDB_CONNECT_TIMEOUT)
                        .isEnabled(DatabasePropertyMappers::isOracleConnectTimeoutEnabled)
                        .build(),
                fromOption(DatabaseOptions.DB_MSSQL_CONNECT_TIMEOUT)
                        .to(MSSQL_CONNECT_TIMEOUT)
                        .isEnabled(DatabasePropertyMappers::isMssqlLoginTimeoutEnabled)
                        .build(),
                fromOption(DatabaseOptions.DB_POSTGRES_CONNECT_TIMEOUT)
                        .to(POSTGRES_CONNECT_TIMEOUT)
                        .isEnabled(DatabasePropertyMappers::isPostgresConnectTimeoutEnabled)
                        .build(),
                fromOption(DatabaseOptions.DB_TIDB_CONNECT_TIMEOUT)
                        .to(TIDB_CONNECT_TIMEOUT)
                        .isEnabled(DatabasePropertyMappers::isTidbConnectTimeoutEnabled)
                        .build(),
                fromOption(DatabaseOptions.DB_URL_HOST)
                        .paramLabel("hostname")
                        .build(),
                fromOption(DatabaseOptions.DB_URL_DATABASE)
                        .paramLabel("dbname")
                        .build(),
                fromOption(DatabaseOptions.DB_URL_PORT)
                        .paramLabel("port")
                        .build(),
                fromOption(DatabaseOptions.DB_URL_PROPERTIES)
                        .paramLabel("properties")
                        .build(),
                fromOption(DatabaseOptions.DB_USERNAME)
                        .to("quarkus.datasource.username")
                        .paramLabel("username")
                        .build(),
                fromOption(DatabaseOptions.DB_PASSWORD)
                        .to("quarkus.datasource.password")
                        .paramLabel("password")
                        .isMasked(true)
                        .build(),
                fromOption(DatabaseOptions.DB_SCHEMA)
                        .paramLabel("schema")
                        .build(),
                fromOption(DB_POOL_INITIAL_SIZE)
                        .to("quarkus.datasource.jdbc.initial-size")
                        .paramLabel("size")
                        .build(),
                fromOption(DatabaseOptions.DB_POOL_MIN_SIZE)
                        .mapFrom(DB, DatabasePropertyMappers::transformMinPoolSize)
                        .to("quarkus.datasource.jdbc.min-size")
                        .paramLabel("size")
                        .build(),
                fromOption(DB_POOL_MAX_SIZE)
                        .to("quarkus.datasource.jdbc.max-size")
                        .paramLabel("size")
                        .build(),
                fromOption(DatabaseOptions.DB_POOL_MAX_LIFETIME)
                        .to("quarkus.datasource.jdbc.max-lifetime")
                        .mapFrom(DB, DatabasePropertyMappers::transformPoolMaxLifetime)
                        .paramLabel("duration")
                        .build(),
                fromOption(DatabaseOptions.DB_SQL_JPA_DEBUG)
                        .build(),
                fromOption(DatabaseOptions.DB_SQL_LOG_SLOW_QUERIES)
                        .paramLabel("milliseconds")
                        .build(),
                fromOption(DatabaseOptions.DB_ENABLED_DATASOURCE)
                        .to("quarkus.datasource.\"<datasource>\".active")
                        .build(),
                fromOption(DB_URL_PATH)
                        .build(),
                // Database TLS configuration
                fromOption(DB_TLS_MODE)
                        .paramLabel("mode")
                        .build(),
                fromOption(DB_TLS_TRUST_STORE_FILE)
                        .paramLabel("path")
                        .build(),
                fromOption(DB_TLS_TRUST_STORE_TYPE)
                        .paramLabel("type")
                        .build(),
                fromOption(DB_TLS_TRUST_STORE_PASSWORD)
                        .paramLabel("password")
                        .isMasked(true)
                        .build(),
                fromOption(DB_ORACLE_TLS_TRANSPORT)
                        .mapFrom(DB_TLS_MODE, DatabasePropertyMappers::transformOracleProtocol)
                        .build(),
                // Oracle
                setTlsJdbcProperty(DB_PROPERTY_SSL_SERVER_DN_MATCH, "ssl_server_dn_match", Map.of(Database.Vendor.ORACLE, "true")),
                setInputTlsJdbcProperty(DB_PROPERTY_ORACLE_TRUST_STORE, DB_TLS_TRUST_STORE_FILE, "javax.net.ssl.trustStore", EnumSet.of(Database.Vendor.ORACLE)),
                setInputTlsJdbcProperty(DB_PROPERTY_ORACLE_TRUST_STORE_PASSWORD, DB_TLS_TRUST_STORE_PASSWORD, "javax.net.ssl.trustStorePassword", EnumSet.of(Database.Vendor.ORACLE)),
                setInputTlsJdbcProperty(DB_PROPERTY_ORACLE_TRUST_STORE_TYPE, DB_TLS_TRUST_STORE_TYPE, "javax.net.ssl.trustStoreType", EnumSet.of(Database.Vendor.ORACLE)),

                // MSSQL
                setTlsJdbcProperty(DB_PROPERTY_ENCRYPT, "encrypt", Map.of(Database.Vendor.MSSQL, "true")),
                setTlsJdbcProperty(DB_PROPERTY_TRUST_SERVER_CERTIFICATE, "trustServerCertificate", Map.of(Database.Vendor.MSSQL, "false")),
                setInputTlsJdbcProperty(DB_PROPERTY_TRUST_STORE, DB_TLS_TRUST_STORE_FILE, "trustStore", EnumSet.of(Database.Vendor.MSSQL)),
                setInputTlsJdbcProperty(DB_PROPERTY_TRUST_STORE_PASSWORD, DB_TLS_TRUST_STORE_PASSWORD, "trustStorePassword", EnumSet.of(Database.Vendor.MSSQL)),

                // Mysql/MariaDB/TiDB
                setTlsJdbcProperty(DatabaseOptions.DB_PROPERTY_SSL_MODE, "sslMode",
                        Map.of(
                                Database.Vendor.MARIADB, "verify-full",
                                Database.Vendor.MYSQL, "VERIFY_IDENTITY",
                                Database.Vendor.TIDB, "VERIFY_IDENTITY"
                        )
                ),
                setInputTlsJdbcProperty(DB_PROPERTY_TRUST_CERTIFICATE_KEY_STORE_URL, DB_TLS_TRUST_STORE_FILE, "trustCertificateKeyStoreUrl", EnumSet.of(Database.Vendor.MYSQL, Database.Vendor.TIDB)),
                setInputTlsJdbcProperty(DB_PROPERTY_TRUST_CERTIFICATE_KEY_STORE_PASSWORD, DB_TLS_TRUST_STORE_PASSWORD, "trustCertificateKeyStorePassword", EnumSet.of(Database.Vendor.MYSQL, Database.Vendor.TIDB)),
                setInputTlsJdbcProperty(DB_PROPERTY_SERVER_SSL_CERT, DB_TLS_TRUST_STORE_FILE, "serverSslCert", EnumSet.of(Database.Vendor.MARIADB)),

                // PosgreSQL
                setTlsJdbcProperty(DatabaseOptions.DB_PROPERTY_SSLMODE, "sslmode", Map.of(Database.Vendor.POSTGRES, "verify-full")),
                fromOption(DB_PROPERTY_SSLFACTORY)
                        .mapFrom(DB)
                        .transformer(DatabasePropertyMappers::computePostgresSSLFactory)
                        .to("quarkus.datasource.jdbc.additional-jdbc-properties.sslfactory")
                        .build(),
                setInputTlsJdbcProperty(DB_PROPERTY_SSLROOTCERT, DB_TLS_TRUST_STORE_FILE, "sslrootcert", EnumSet.of(Database.Vendor.POSTGRES))
        );

        return appendDatasourceMappers(mappers, Map.of(
                // Inherit options from the DB mappers
                DB, PropertyMapper.Builder::removeMapFrom,
                DB_POOL_INITIAL_SIZE, mapper -> mapper.mapFrom(DB_POOL_INITIAL_SIZE),
                DB_POOL_MAX_SIZE, mapper -> mapper.mapFrom(DB_POOL_MAX_SIZE)
        ));
    }

    @Override
    public void validateConfig(Picocli picocli) {
        Configuration.getOptionalIntegerValue(DB_POOL_MAX_SIZE).ifPresent(poolMaxSize -> {
            if (poolMaxSize < JDBC_PING_MIN_POOL_MAX_SIZE && isJdbcPingStack()) {
                throw new PropertyException(
                        "The JDBC_PING cache stack requires '%s' to be at least %d (current: %d). A higher value is recommended."
                                .formatted(DB_POOL_MAX_SIZE.getKey(), JDBC_PING_MIN_POOL_MAX_SIZE, poolMaxSize));
            }
        });
    }

    private static boolean isJdbcPingStack() {
        if (!CachingPropertyMappers.cacheSetToInfinispan()) {
            return false;
        }
        String stack = getOptionalKcValue(CachingOptions.CACHE_STACK).orElse(Stack.jdbc_ping.toString());
        return Stack.jdbc_ping.toString().equals(stack) || Stack.jdbc_ping_udp.toString().equals(stack);
    }

    private static final Option<String> DB_URL_PATH = new OptionBuilder<>("db-url-path", String.class)
            .hidden()
            .description("Used for internal purposes of H2 database.")
            .build();

    public static boolean isPostgresqlTargetServerTypeEnabled() {
        String db = Configuration.getConfigValue(DB).getValue();
        Database.Vendor vendor = Database.getVendor(db).orElse(null);
        if (vendor != Database.Vendor.POSTGRES) {
            return false;
        }

        String dbDriver = Configuration.getConfigValue(DatabaseOptions.DB_DRIVER).getValue();
        String dbUrl = Configuration.getConfigValue(DatabaseOptions.DB_URL).getValue();

        if (!Objects.equals(Database.getDriver(db, true).orElse(null), dbDriver) &&
                !Objects.equals(Database.getDriver(db, false).orElse(null), dbDriver)) {
            // Custom JDBC-Driver, for example, AWS JDBC Wrapper.
            return false;
        }
        // targetServerType already set to same or different value in db-url, ignore
        return dbUrl == null || !dbUrl.contains("targetServerType");
    }

    public static boolean isMssqlSendStringParametersAsUnicode() {
        String db = Configuration.getConfigValue(DB).getValue();
        Database.Vendor vendor = Database.getVendor(db).orElse(null);
        if (vendor != Database.Vendor.MSSQL) {
            return false;
        }
        String dbDriver = Configuration.getConfigValue(DatabaseOptions.DB_DRIVER).getValue();
        String dbUrl = Configuration.getConfigValue(DatabaseOptions.DB_URL).getValueOrDefault("");
        String dbUrlProperties = Configuration.getKcConfigValue(DatabaseOptions.DB_URL_PROPERTIES.getKey()).getValueOrDefault("");

        log.debugf("Determining whether to set 'sendStringParametersAsUnicode' for MSSQL based on db '%s', driver '%s', url '%s'",
                db, dbDriver, dbUrl);

        if (!Objects.equals(Database.getDriver(db, true).orElse(null), dbDriver) &&
                !Objects.equals(Database.getDriver(db, false).orElse(null), dbDriver)) {
            return false;
        }

        return !dbUrl.contains("sendStringParametersAsUnicode") &&
                !dbUrlProperties.contains("sendStringParametersAsUnicode");
    }

    public static boolean isMysqlConnectTimeoutEnabled() {
        return isConnectTimeoutEnabled(Database.Vendor.MYSQL, "connectTimeout");
    }

    public static boolean isMariadbConnectTimeoutEnabled() {
        return isConnectTimeoutEnabled(Database.Vendor.MARIADB, "connectTimeout");
    }

    public static boolean isOracleConnectTimeoutEnabled() {
        return isConnectTimeoutEnabled(Database.Vendor.ORACLE, "oracle.net.CONNECT_TIMEOUT");
    }

    public static boolean isMssqlLoginTimeoutEnabled() {
        return isConnectTimeoutEnabled(Database.Vendor.MSSQL, "loginTimeout");
    }

    public static boolean isPostgresConnectTimeoutEnabled() {
        return isConnectTimeoutEnabled(Database.Vendor.POSTGRES, "connectTimeout");
    }

    public static boolean isTidbConnectTimeoutEnabled() {
        return isConnectTimeoutEnabled(Database.Vendor.TIDB, "connectTimeout");
    }

    private static boolean isConnectTimeoutEnabled(Database.Vendor expectedVendor, String timeoutProperty) {
        String db = Configuration.getConfigValue(DB).getValue();
        Database.Vendor vendor = Database.getVendor(db).orElse(null);
        if (vendor != expectedVendor) {
            return false;
        }

        String dbDriver = Configuration.getConfigValue(DatabaseOptions.DB_DRIVER).getValue();
        if (!Objects.equals(Database.getDriver(db, true).orElse(null), dbDriver) &&
                !Objects.equals(Database.getDriver(db, false).orElse(null), dbDriver)) {
            // Custom JDBC driver (e.g. AWS JDBC Wrapper) — do not inject defaults
            return false;
        }

        String dbUrl = Configuration.getConfigValue(DatabaseOptions.DB_URL).getValueOrDefault("");
        String dbUrlProperties = Configuration.getKcConfigValue(DatabaseOptions.DB_URL_PROPERTIES.getKey()).getValueOrDefault("");

        // Property already set explicitly by the user — do not override
        return !dbUrl.contains(timeoutProperty) && !dbUrlProperties.contains(timeoutProperty);
    }

    /**
     * Starting with H2 version 2.x, marking "VALUE" as a non-keyword is necessary as some columns are named "VALUE" in the Keycloak schema.
     * <p />
     * Alternatives considered and rejected:
     * <ul>
     * <li>customizing H2 Database dialect -&gt; wouldn't work for existing Liquibase scripts.</li>
     * <li>adding quotes to <code>@Column(name="VALUE")</code> annotations -&gt; would require testing for all DBs, wouldn't work for existing Liquibase scripts.</li>
     * </ul>
     * Downsides of this solution: Release notes needed to point out that any H2 JDBC URL parameter with <code>NON_KEYWORDS</code> needs to add the keyword <code>VALUE</code> manually.
     * @return JDBC URL with <code>NON_KEYWORDS=VALUE</code> appended if the URL doesn't contain <code>NON_KEYWORDS=</code> yet
     */
    private static String addH2NonKeywords(String jdbcUrl) {
        if (!jdbcUrl.contains("NON_KEYWORDS=")) {
            jdbcUrl = jdbcUrl + ";NON_KEYWORDS=VALUE";
        }
        return jdbcUrl;
    }

    /**
     * Required so that the H2 db instance is closed only when the Agroal connection pool is closed during
     * Keycloak shutdown. We cannot rely on the default H2 ShutdownHook as this can result in the DB being
     * closed before dependent resources, e.g. JDBC_PING2, are shutdown gracefully. This solution also
     * requires the Agroal min-pool connection size to be at least 1.
     */
    private static String addH2CloseOnExit(String jdbcUrl) {
        if (!jdbcUrl.contains("DB_CLOSE_ON_EXIT=")) {
            jdbcUrl = jdbcUrl + ";DB_CLOSE_ON_EXIT=FALSE";
        }
        if (!jdbcUrl.contains("DB_CLOSE_DELAY=")) {
            jdbcUrl = jdbcUrl + ";DB_CLOSE_DELAY=0";
        }
        return jdbcUrl;
    }

    private static String amendH2(String jdbcUrl) {
        return addH2CloseOnExit(addH2NonKeywords(jdbcUrl));
    }

    private static String getDatabaseUrl(String name, String value, ConfigSourceInterceptorContext c) {
        String url = Database.getDefaultUrl(name, value).orElse(null);
        if (isDevModeDatabase(value)) {
            String key = Optional.ofNullable(name).map(
                    n -> DatabaseOptions.Datasources.getNamedKey(DatabaseOptions.DB_URL_PROPERTIES, n).orElseThrow())
                    .orElse(DatabaseOptions.DB_URL_PROPERTIES.getKey());
            String urlProps = Configuration.getKcConfigValue(key).getValue();
            if (urlProps != null) {
                url += urlProps;
            }
            url = amendH2(url);
        }
        return url;
    }

    private static String getXaOrNonXaDriver(String name, String value, ConfigSourceInterceptorContext context) {
        var key = StringUtil.isNotBlank(name) ? TransactionOptions.getNamedTxXADatasource(name) : TransactionOptions.TRANSACTION_XA_ENABLED.getKey();
        boolean isXaEnabled = Configuration.isKcPropertyTrue(key);
        return Database.getDriver(value, isXaEnabled).orElse(null);
    }

    private static String toDatabaseKind(String db, ConfigSourceInterceptorContext context) {
        return Database.getDatabaseKind(db).orElse(null);
    }

    private static boolean isDevModeDatabase(String database) {
        return Database.getDatabaseKind(database).filter(DatabaseKind.H2::equals).isPresent();
    }

    private static String transformDialect(String db, ConfigSourceInterceptorContext context) {
        return Database.getDialect(db).orElse(null);
    }

    /**
     * For H2 databases we must ensure that the min-pool size is at least one so that the DB is not shutdown until the
     * Agroal connection pool is closed on Keycloak shutdown.
     */
    private static String transformMinPoolSize(String database, ConfigSourceInterceptorContext context) {
        Supplier<String> getParentPoolMinSize = () -> Optional.ofNullable(context.proceed(NS_KEYCLOAK_PREFIX + DatabaseOptions.DB_POOL_MIN_SIZE.getKey()))
                .map(ConfigValue::getValue)
                .orElse(null);
        return isDevModeDatabase(database) ? "1" : getParentPoolMinSize.get();
    }

    private static String transformPoolMaxLifetime(String db, ConfigSourceInterceptorContext context) {
        Database.Vendor vendor = Database.getVendor(db).orElseThrow();
        return switch (vendor) {
            // Default to max lifetime slightly less than the default `wait_timeout` of 8 hours
            case MYSQL, MARIADB -> "PT7H50M";
            default -> "";
        };
    }

    public static final class Datasources {

        /**
         * Automatically create mappers for datasource options
         */
        static List<PropertyMapper<?>> appendDatasourceMappers(List<PropertyMapper<?>> mappers, Map<Option<?>, Consumer<PropertyMapper.Builder<?>>> transformDatasourceMappers) {
            List<PropertyMapper<?>> datasourceMappers = new ArrayList<>(OPTIONS_DATASOURCES.size() + mappers.size());

            for (var parent : mappers) {
                var parentOption = parent.getOption();

                var datasourceOption = getDatasourceOption(parentOption);
                if (datasourceOption.isEmpty()) {
                    log.debugf("No datasource option found for '%s'", parentOption.getKey());
                    continue;
                }

                var created = fromOption(datasourceOption.get())
                        .isMasked(parent.isMask())
                        .transformer(parent.getMapper());

                if (parent.getMapFrom() != null) {
                    var wildcardMapFromOption = getKeyForDatasource(parent.getMapFrom())
                            .orElseThrow(() -> new IllegalArgumentException("Option '%s' in mapFrom() method for mapper '%s' does not have any associated wildcard option".formatted(parent.getMapFrom(), datasourceOption.get().getKey())));
                    created.wildcardMapFrom(wildcardMapFromOption, parent.getParentMapper() != null ? (name, value, context) -> parent.getParentMapper().map(name, value, context) : null);
                }

                if (parent.getParamLabel() != null) {
                    created.paramLabel(parent.getParamLabel());
                }

                var transformedTo = transformDatasourceTo(parent.getTo());
                if (transformedTo != null) {
                    created.to(transformedTo);
                }

                var customTransformer = transformDatasourceMappers.get(parent.getOption());
                if (customTransformer != null) {
                    customTransformer.accept(created);
                }

                Option<String> primaryOption = getDatasourceOption(DB).orElseThrow();

                PropertyMapper<?> mapper = created.build();
                // if we're not the DB option, nor mapped directly from the DB option, then
                // it's considered "connected" for the purposes of discovery
                if (parentOption != DB && !primaryOption.getKey().equals(mapper.getMapFrom())) {
                    primaryOption.getConnectedOptions().add(mapper.getOption().getKey());
                }
                datasourceMappers.add(mapper);
            }

            datasourceMappers.addAll(mappers);

            return datasourceMappers;
        }

        private static String transformDatasourceTo(String to) {
            if (StringUtil.isBlank(to)) {
                return null;
            }

            if (to.startsWith("quarkus.datasource.")) {
                return to.replaceFirst("quarkus\\.datasource\\.", "quarkus.datasource.\"<datasource>\".");
            } else if (to.startsWith("kc.db-")) {
                return to.concat("-<datasource>");
            } else {
                log.warnf("Cannot determine how to map datasource option to '%s'", to);
            }
            return to;
        }
    }

    private static String transformOracleProtocol(String datasource, String value, ConfigSourceInterceptorContext configSourceInterceptorContext) {
        var tlsMode = DatabaseOptions.DatabaseTlsMode.fromCliValue(value);
        return tlsMode != DatabaseOptions.DatabaseTlsMode.DISABLED ? "@tcps:" : "@";
    }

    private static PropertyMapper<?> setTlsJdbcProperty(Option<String> option, String jdbcPropertyKey, Map<Database.Vendor, String> vendorValues) {
        return fromOption(option)
                .mapFrom(DB)
                .transformer((name, value, context) -> computeTlsProperty(vendorValues, name, value, jdbcPropertyKey))
                .to("quarkus.datasource.jdbc.additional-jdbc-properties." + jdbcPropertyKey)
                .build();
    }

    private static PropertyMapper<?> setInputTlsJdbcProperty(Option<String> option, Option<?> from, String jdbcPropertyKey, Collection<Database.Vendor> vendorValues) {
        return fromOption(option)
                .mapFrom(from)
                .transformer((name, value, context) -> transformTlsUserProperty(vendorValues, name, value, jdbcPropertyKey))
                .to("quarkus.datasource.jdbc.additional-jdbc-properties." + jdbcPropertyKey)
                .build();
    }

    private static String transformTlsUserProperty(Collection<Database.Vendor> validForVendors, String datasource, String value, String jdbcPropertyKey) {
        // db should have been assigned to the correct datasource db-kind
        var vendor = getDatabaseVendor(datasource);
        if (!validForVendors.contains(vendor)) {
            // this jdbc property is not for this vendor
            return null;
        }
        return transformTlsProperty(vendor, datasource, jdbcPropertyKey, value);
    }

    private static String computeTlsProperty(Map<Database.Vendor, String> vendorValue, String datasource, String db, String jdbcPropertyKey) {
        // db should have been assigned to the correct datasource db-kind
        var vendor = Database.getVendor(db).orElseThrow();
        return transformTlsProperty(vendor, datasource, jdbcPropertyKey, vendorValue.get(vendor));
    }

    private static String transformTlsProperty(Database.Vendor vendor, String datasource, String key, String value) {
        if (value == null) {
            //not set
            return null;
        }
        var tlsMode = getDatabaseTlsMode(datasource);
        if (tlsMode != DatabaseOptions.DatabaseTlsMode.VERIFY_SERVER) {
            // TLS mode not enabled, do not set this jdbc property
            return null;
        }
        var jdbcUrl = findDatabaseUrl(datasource).orElse("");
        if (vendor == Database.Vendor.ORACLE && !jdbcUrl.toLowerCase().contains("tcps")) {
            // Oracle needs the transport set to TCPS to support encryption
            return null;
        }

        if (jdbcUrl.contains(key)) {
            // property set by the user, do not overwrite
            return null;
        }
        return value;
    }

    private static String computePostgresSSLFactory(String datasource, String db, ConfigSourceInterceptorContext configSourceInterceptorContext) {
        var value = computeTlsProperty(Map.of(Database.Vendor.POSTGRES, "org.postgresql.ssl.DefaultJavaSSLFactory"), datasource, db, "sslfactory");
        if (value == null) {
            return null;
        }
        // if the user set the truststore file, we don't need to set the sslfactory property.
        return findTlsTrustStoreFile(datasource).isEmpty() ? value : null;
    }

    private static Optional<String> findDatabaseUrl(String datasource) {
        var option = datasource == null ?
                Optional.of(DB_URL.getKey()) :
                getNamedKey(DB_URL, datasource);
        return option.map(Configuration::getKcConfigValue)
                .map(ConfigValue::getValue);
    }

    private static Database.Vendor getDatabaseVendor(String datasource) {
        var option = datasource == null ?
                Optional.of(DB.getKey()) :
                getNamedKey(DB, datasource);
        return option.map(Configuration::getKcConfigValue)
                .map(ConfigValue::getValue)
                .flatMap(Database::getVendor)
                .orElseThrow();
    }

    private static DatabaseOptions.DatabaseTlsMode getDatabaseTlsMode(String datasource) {
        var option = datasource == null ?
                Optional.of(DB_TLS_MODE.getKey()) :
                getNamedKey(DB_TLS_MODE, datasource);
        return option.map(Configuration::getKcConfigValue)
                .map(ConfigValue::getValue)
                .map(String::toUpperCase)
                .map(DatabaseOptions.DatabaseTlsMode::fromCliValue)
                .orElse(DatabaseOptions.DatabaseTlsMode.DISABLED);
    }

    private static Optional<String> findTlsTrustStoreFile(String datasource) {
        var option = datasource == null ?
                Optional.of(DB_TLS_TRUST_STORE_FILE.getKey()) :
                getNamedKey(DB_TLS_TRUST_STORE_FILE, datasource);
        return option.map(Configuration::getKcConfigValue)
                .map(ConfigValue::getValue);
    }

}
