package org.keycloak.quarkus.runtime.configuration.mappers;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.keycloak.common.Profile;
import org.keycloak.common.util.DurationConverter;
import org.keycloak.config.CachingOptions;
import org.keycloak.config.CachingOptions.Stack;
import org.keycloak.config.DatabaseOptions;
import org.keycloak.config.Option;
import org.keycloak.config.OptionBuilder;
import org.keycloak.config.OptionCategory;
import org.keycloak.config.OptionsUtil;
import org.keycloak.config.TransactionOptions;
import org.keycloak.config.WildcardOptionsUtil;
import org.keycloak.config.database.Database;
import org.keycloak.config.database.Database.Vendor;
import org.keycloak.connections.jpa.util.JpaUtils;
import org.keycloak.quarkus.runtime.cli.Picocli;
import org.keycloak.quarkus.runtime.cli.PropertyException;
import org.keycloak.quarkus.runtime.configuration.Configuration;
import org.keycloak.quarkus.runtime.configuration.HibernateOrmProperties;
import org.keycloak.quarkus.runtime.configuration.HibernateOrmProperties.HibernateOrmProperty;
import org.keycloak.quarkus.runtime.configuration.mappers.PropertyMapper.ValueMapper;
import org.keycloak.utils.StringUtil;

import io.quarkus.datasource.common.runtime.DatabaseKind;
import io.smallrye.config.ConfigSourceInterceptorContext;
import io.smallrye.config.ConfigValue;
import org.jboss.logging.Logger;

import static org.keycloak.config.DatabaseOptions.DB;
import static org.keycloak.config.DatabaseOptions.DB_KIND;
import static org.keycloak.config.DatabaseOptions.DB_MTLS_KEY_STORE_FILE;
import static org.keycloak.config.DatabaseOptions.DB_MTLS_KEY_STORE_PASSWORD;
import static org.keycloak.config.DatabaseOptions.DB_MTLS_KEY_STORE_TYPE;
import static org.keycloak.config.DatabaseOptions.DB_POOL_INITIAL_SIZE;
import static org.keycloak.config.DatabaseOptions.DB_POOL_MAX_SIZE;
import static org.keycloak.config.DatabaseOptions.DB_TLS_MODE;
import static org.keycloak.config.DatabaseOptions.DB_TLS_TRUST_STORE_FILE;
import static org.keycloak.config.DatabaseOptions.DB_TLS_TRUST_STORE_PASSWORD;
import static org.keycloak.config.DatabaseOptions.DB_TLS_TRUST_STORE_TYPE;
import static org.keycloak.config.DatabaseOptions.DB_URL;
import static org.keycloak.quarkus.runtime.configuration.Configuration.getOptionalKcValue;
import static org.keycloak.quarkus.runtime.configuration.MicroProfileConfigProvider.NS_KEYCLOAK_PREFIX;
import static org.keycloak.quarkus.runtime.configuration.mappers.DatabasePropertyMappers.Datasources.appendDatasourceMappers;
import static org.keycloak.quarkus.runtime.configuration.mappers.PropertyMapper.fromOption;
import static org.keycloak.quarkus.runtime.storage.database.jpa.QuarkusJpaConnectionProviderFactory.QUERY_PROPERTY_PREFIX;

public final class DatabasePropertyMappers implements PropertyMapperGrouping {
    private static final Option<String> SYNTHETIC_RUNTIME_DB_OPTION = DB.toBuilder().synthetic().buildTime(false).build();
    private static final Option<String> SYNTHETIC_RUNTIME_DB_OPTION_NO_DEFAULT = new OptionBuilder<>("db-synthetic-no-default",
            String.class).synthetic().buildTime(false).defaultValue(Optional.empty()).build();
    public static final String PG_TARGET_SERVER_TYPE = "quarkus.datasource.jdbc.additional-jdbc-properties.targetServerType";
    public static final String PG_LOG_SERVER_ERROR_DETAIL = "quarkus.datasource.jdbc.additional-jdbc-properties.logServerErrorDetail";
    public static final String MSSQL_SEND_STRING_PARAMETER_AS_UNICODE = "quarkus.datasource.jdbc.additional-jdbc-properties.sendStringParametersAsUnicode";
    public static final String CONNECT_TIMEOUT = "quarkus.datasource.jdbc.additional-jdbc-properties.connectTimeout";
    public static final String SOCKET_TIMEOUT = "quarkus.datasource.jdbc.additional-jdbc-properties.socketTimeout";
    public static final String ORACLEDB_CONNECT_TIMEOUT = "quarkus.datasource.jdbc.additional-jdbc-properties.oracle.net.CONNECT_TIMEOUT";
    public static final String ORACLEDB_CONNECTION_PROPERTIES = "quarkus.datasource.jdbc.additional-jdbc-properties.ConnectionProperties";
    public static final String MSSQL_CONNECT_TIMEOUT = "quarkus.datasource.jdbc.additional-jdbc-properties.loginTimeout";
    private static final String QUARKUS_HIBERNATE_ORM_PREFIX = "quarkus.hibernate-orm.";
    private static final String ORACLE_NET_CONNECT_TIMEOUT = "oracle.net.CONNECT_TIMEOUT";
    public static final String JDBC_LOGIN_TIMEOUT = "quarkus.datasource.jdbc.login-timeout";
    public static final String JDBC_ACQUISITION_TIMEOUT = "quarkus.datasource.jdbc.acquisition-timeout";

    private static final Logger log = Logger.getLogger(DatabasePropertyMappers.class);

    /**
     * Minimum {@code db-pool-max-size} required for {@link Stack#jdbc_ping} and {@link Stack#jdbc_ping_udp}.
     * Determined experimentally — lower values cause startup failures due to connection pool exhaustion.
     * Verified by {@code KeycloakDeploymentTest#testDocumentedMinimalPoolMaxSizeWorks}.
     */
    private static final int JDBC_PING_MIN_POOL_MAX_SIZE = 4;

    @Override
    public List<PropertyMapper<?>> getPropertyMappers() {
        List<PropertyMapper<?>> allSourceMappers = List.of(
                fromOption(DatabaseOptions.DB_DIALECT)
                        .mapFrom(DB, DatabasePropertyMappers::transformDialect)
                        .build(),
                fromOption(DatabaseOptions.DB_DRIVER)
                        .mapFrom(DB, DatabasePropertyMappers::getXaOrNonXaDriver)
                        .to("quarkus.datasource.jdbc.driver")
                        .paramLabel("driver")
                        .build(),
                fromOption(DatabaseOptions.DB_URL)
                        .to("quarkus.datasource.jdbc.url")
                        .mapFrom(DB, DatabasePropertyMappers::getDatabaseUrl)
                        .paramLabel("jdbc-url")
                        .build(),
                fromOption(DatabaseOptions.DB_CONNECT_TIMEOUT)
                        .to(JDBC_LOGIN_TIMEOUT)
                        .validator(DatabasePropertyMappers::validateConnectTimeout)
                        .paramLabel("timeout")
                        .build(),
                fromOption(DatabaseOptions.DB_POOL_ACQUISITION_TIMEOUT)
                        .to(JDBC_ACQUISITION_TIMEOUT)
                        .mapFrom(DatabaseOptions.DB_CONNECT_TIMEOUT, (name, value, context) -> computeAcquisitionTimeout(value))
                        .build(),
                fromOption(DatabaseOptions.DB_CONNECT_TIMEOUT)
                        .to(CONNECT_TIMEOUT)
                        .mapFrom(DatabaseOptions.DB_CONNECT_TIMEOUT, getConnectTimeout(EnumSet.of(Database.Vendor.MYSQL, Database.Vendor.MARIADB, Database.Vendor.POSTGRES, Database.Vendor.TIDB), "connectTimeout"))
                        .build(),
                fromOption(DatabaseOptions.DB_CONNECT_TIMEOUT)
                        .to(ORACLEDB_CONNECTION_PROPERTIES)
                        .mapFrom(DatabaseOptions.DB_CONNECT_TIMEOUT, getOracleConnectTimeout(true))
                        .build(),
                fromOption(DatabaseOptions.DB_CONNECT_TIMEOUT)
                        .to(ORACLEDB_CONNECT_TIMEOUT)
                        .mapFrom(DatabaseOptions.DB_CONNECT_TIMEOUT, getOracleConnectTimeout(false))
                        .build(),
                fromOption(DatabaseOptions.DB_CONNECT_TIMEOUT)
                        .to(MSSQL_CONNECT_TIMEOUT)
                        .mapFrom(DatabaseOptions.DB_CONNECT_TIMEOUT, getConnectTimeout(EnumSet.of(Database.Vendor.MSSQL), "loginTimeout"))
                        .build(),
                /* For MySQL based databases, setting the login timeout is not sufficient as there are some additional SQL statements running
                   directly after the login. Once the connection is later acquired for a transaction, the UpdateSocketTimeoutOnConnectionAcquireInterceptor
                   will then later overwrite the socket timeout.
                   See https://github.com/keycloak/keycloak/issues/47174 for the discussion.
                 */
                fromOption(DatabaseOptions.DB_CONNECT_TIMEOUT)
                        .to(SOCKET_TIMEOUT)
                        .mapFrom(DatabaseOptions.DB_CONNECT_TIMEOUT, getSocketTimeout(EnumSet.of(Database.Vendor.MYSQL, Database.Vendor.MARIADB, Database.Vendor.TIDB), "socketTimeout"))
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
                fromOption(DatabaseOptions.DB_SQL_JPA_DEBUG)
                        .build(),
                fromOption(DatabaseOptions.DB_SQL_LOG_SLOW_QUERIES)
                        .paramLabel("milliseconds")
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
                fromOption(DB_MTLS_KEY_STORE_FILE)
                        .paramLabel("path")
                        .build(),
                fromOption(DB_MTLS_KEY_STORE_TYPE)
                        .paramLabel("type")
                        .build(),
                fromOption(DB_MTLS_KEY_STORE_PASSWORD)
                        .paramLabel("password")
                        .isMasked(true)
                        .build(),

                // Oracle
                setTlsJdbcProperty("ssl_server_dn_match", Map.of(Database.Vendor.ORACLE, "true")),
                setInputTlsJdbcProperty(DB_TLS_TRUST_STORE_FILE, "javax.net.ssl.trustStore", EnumSet.of(Database.Vendor.ORACLE)),
                setInputTlsJdbcProperty(DB_TLS_TRUST_STORE_PASSWORD, "javax.net.ssl.trustStorePassword", EnumSet.of(Database.Vendor.ORACLE)),
                setInputTlsJdbcProperty(DB_TLS_TRUST_STORE_TYPE, "javax.net.ssl.trustStoreType", EnumSet.of(Database.Vendor.ORACLE)),

                setInputTlsJdbcProperty(DB_MTLS_KEY_STORE_FILE, "javax.net.ssl.keyStore", EnumSet.of(Database.Vendor.ORACLE)),
                setInputTlsJdbcProperty(DB_MTLS_KEY_STORE_PASSWORD, "javax.net.ssl.keyStorePassword", EnumSet.of(Database.Vendor.ORACLE)),
                setInputTlsJdbcProperty(DB_MTLS_KEY_STORE_TYPE, "javax.net.ssl.keyStoreType", EnumSet.of(Database.Vendor.ORACLE)),
                fromOption(DB_MTLS_KEY_STORE_FILE)
                        .mapFrom(DB_MTLS_KEY_STORE_FILE, (name, value, context) -> computeOracleAuthenticationServices(name, value))
                        .to("quarkus.datasource.jdbc.additional-jdbc-properties.oracle.net.authentication_services")
                        .build(),

                // MSSQL
                setTlsJdbcProperty("encrypt", Map.of(Database.Vendor.MSSQL, "true")),
                setTlsJdbcProperty("trustServerCertificate", Map.of(Database.Vendor.MSSQL, "false")),
                setInputTlsJdbcProperty(DB_TLS_TRUST_STORE_FILE, "trustStore", EnumSet.of(Database.Vendor.MSSQL)),
                setInputTlsJdbcProperty(DB_TLS_TRUST_STORE_PASSWORD, "trustStorePassword", EnumSet.of(Database.Vendor.MSSQL)),

                // Mysql/MariaDB/TiDB
                setTlsJdbcProperty("sslMode",
                        Map.of(
                                Database.Vendor.MARIADB, "verify-full",
                                Database.Vendor.MYSQL, "VERIFY_IDENTITY",
                                Database.Vendor.TIDB, "VERIFY_IDENTITY"
                        )
                ),
                setInputTlsJdbcProperty(DB_TLS_TRUST_STORE_FILE, "trustCertificateKeyStoreUrl", EnumSet.of(Database.Vendor.MYSQL, Database.Vendor.TIDB)),
                setInputTlsJdbcProperty(DB_TLS_TRUST_STORE_PASSWORD, "trustCertificateKeyStorePassword", EnumSet.of(Database.Vendor.MYSQL, Database.Vendor.TIDB)),

                setInputTlsJdbcProperty(DB_MTLS_KEY_STORE_FILE, "clientCertificateKeyStoreUrl", EnumSet.of(Database.Vendor.MYSQL, Database.Vendor.TIDB)),
                setInputTlsJdbcProperty(DB_MTLS_KEY_STORE_PASSWORD, "clientCertificateKeyStorePassword", EnumSet.of(Database.Vendor.MYSQL, Database.Vendor.TIDB)),
                setInputTlsJdbcProperty(DB_MTLS_KEY_STORE_TYPE, "clientCertificateKeyStoreType", EnumSet.of(Database.Vendor.MYSQL, Database.Vendor.TIDB)),

                setInputTlsJdbcProperty(DB_TLS_TRUST_STORE_FILE, "serverSslCert", EnumSet.of(Database.Vendor.MARIADB)),
                setInputTlsJdbcProperty(DB_MTLS_KEY_STORE_FILE, "keyStore", EnumSet.of(Database.Vendor.MARIADB)),
                setInputTlsJdbcProperty(DB_MTLS_KEY_STORE_PASSWORD, "keyStorePassword", EnumSet.of(Database.Vendor.MARIADB)),

                // PostgreSQL
                setTlsJdbcProperty("sslmode", Map.of(Database.Vendor.POSTGRES, "verify-full")),
                fromOption(SYNTHETIC_RUNTIME_DB_OPTION)
                        .mapFrom(DB)
                        .transformer(DatabasePropertyMappers::computePostgresSSLFactory)
                        .to("quarkus.datasource.jdbc.additional-jdbc-properties.sslfactory")
                        .build(),
                setInputTlsJdbcProperty(DB_TLS_TRUST_STORE_FILE, "sslrootcert", EnumSet.of(Database.Vendor.POSTGRES)),

                // PostgreSQL mTLS (pgjdbc supports PKCS#12 via sslkey with .p12/.pfx extension since 42.2.9)
                setInputTlsJdbcProperty(DB_MTLS_KEY_STORE_FILE, "sslkey", EnumSet.of(Database.Vendor.POSTGRES)),
                setInputTlsJdbcProperty(DB_MTLS_KEY_STORE_PASSWORD, "sslpassword", EnumSet.of(Database.Vendor.POSTGRES))
        );
        
        // Hibernate ORM configuration of the default persistence unit, including the Quarkus Hibernate ORM properties
        // exposed as db-orm-* options (see addHibernateOrmMappers). Named datasources get the same mappers (e.g.
        // db-dialect-<datasource>), targeting the persistence unit that db-jpa-packages-<datasource> defines for the
        // datasource, see Datasources#appendDatasourceMappers.
        List<PropertyMapper<?>> persistenceUnitMappers = new ArrayList<>(List.of(
                fromOption(DatabaseOptions.DB_DIALECT)
                        .mapFrom(DatabaseOptions.DB_DIALECT)
                        .to("quarkus.hibernate-orm.dialect")
                        .build(),
                fromOption(SYNTHETIC_RUNTIME_DB_OPTION_NO_DEFAULT)
                        .mapFrom(DatabaseOptions.DB_SQL_JPA_DEBUG,
                                (name, value, context) -> Boolean.parseBoolean(value) ? Boolean.TRUE.toString() : null)
                        .to("quarkus.hibernate-orm.unsupported-properties.\"hibernate.use_sql_comments\"")
                        .build(),
                fromOption(DatabaseOptions.DB_SQL_LOG_SLOW_QUERIES)
                        .mapFrom(DatabaseOptions.DB_SQL_LOG_SLOW_QUERIES)
                        .to("quarkus.hibernate-orm.log.queries-slower-than-ms")
                        .build(),
                // the default persistence unit also applies the schema through KeycloakRecorder#createDefaultUnitListener
                fromOption(DatabaseOptions.DB_SCHEMA)
                        .mapFrom(DatabaseOptions.DB_SCHEMA)
                        .to("quarkus.hibernate-orm.database.default-schema")
                        .build()
        ));
        // properties of the extension as a whole have no named persistence unit variant
        List<PropertyMapper<?>> globalHibernateOrmMappers = new ArrayList<>();
        addHibernateOrmMappers(persistenceUnitMappers, globalHibernateOrmMappers);

        List<PropertyMapper<?>> result = appendDatasourceMappers(allSourceMappers, persistenceUnitMappers, Map.of(
                // Inherit options from the DB mappers
                DB_POOL_INITIAL_SIZE, mapper -> mapper.mapFrom(DB_POOL_INITIAL_SIZE),
                DB_POOL_MAX_SIZE, mapper -> mapper.mapFrom(DB_POOL_MAX_SIZE)
        ));

        // finally add mappers that aren't intended to work with all datasources
        // - also this usage of isEnabled won't work correctly with wildcard mappers
        result.addAll(List.of(
                fromOption(DB)
                        .to("quarkus.datasource.db-kind")
                        .transformer(DatabasePropertyMappers::toDatabaseKind)
                        .paramLabel("vendor")
                        .build(),
                fromOption(DB_KIND)
                        .to("quarkus.datasource.\"<datasource>\".db-kind")
                        .transformer(DatabasePropertyMappers::toDatabaseKind)
                        .paramLabel("vendor")
                        .build(),
                fromOption(DatabaseOptions.DB_HEALTH_EXCLUDE)
                        .to("quarkus.datasource.\"<datasource>\".health-exclude")
                        .build(),
                fromOption(DatabaseOptions.DB_POOL_MAX_LIFETIME)
                        .to("quarkus.datasource.jdbc.max-lifetime")
                        .mapFrom(DB, DatabasePropertyMappers::transformPoolMaxLifetime)
                        .paramLabel("duration")
                        .build(),
                fromOption(DatabaseOptions.DB_ENABLED_DATASOURCE)
                        .to("quarkus.datasource.\"<datasource>\".active")
                        .build(),
                fromOption(SYNTHETIC_RUNTIME_DB_OPTION).mapFrom(DB, (name, value, context) -> "primary")
                        .to(PG_TARGET_SERVER_TYPE)
                        .isEnabled(DatabasePropertyMappers::isPostgresqlTargetServerTypeEnabled)
                        .build(),
                fromOption(SYNTHETIC_RUNTIME_DB_OPTION).mapFrom(DB, (name, value, context) -> "false")
                        .to(PG_LOG_SERVER_ERROR_DETAIL)
                        .isEnabled(DatabasePropertyMappers::isPostgresqlLogServerErrorDetailEnabled)
                        .build(),
                fromOption(SYNTHETIC_RUNTIME_DB_OPTION).mapFrom(DB, (name, value, context) -> "false")
                        .to(MSSQL_SEND_STRING_PARAMETER_AS_UNICODE)
                        .isEnabled(DatabasePropertyMappers::isMssqlSendStringParametersAsUnicode)
                        .build(),
                fromOption(SYNTHETIC_RUNTIME_DB_OPTION).mapFrom(DB, (name, value, context) -> "read-committed")
                        .to("quarkus.datasource.jdbc.transaction-isolation-level")
                        .isEnabled(DatabasePropertyMappers::isReadCommittedIsolationRequired)
                        .build()
        ));

        // Keycloak's named queries are specific to the default persistence unit
        result.addAll(namedQueryMappers());

        result.addAll(globalHibernateOrmMappers);

        // configuration-defined persistence unit of a named datasource, named after the datasource
        result.add(fromOption(DatabaseOptions.DB_JPA_PACKAGES)
                .to("quarkus.hibernate-orm.\"<datasource>\".packages")
                .paramLabel("packages")
                .build());
        result.add(fromOption(DatabaseOptions.DB_JPA_PACKAGES)
                .to("quarkus.hibernate-orm.\"<datasource>\".datasource")
                .wildcardMapFrom(DatabaseOptions.DB_JPA_PACKAGES, (datasource, value, context) -> datasource)
                .build());

        return result;
    }

    /**
     * Hibernate ORM properties, or groups of them, without a {@code db-orm-*} option: properties that Keycloak configures
     * itself (the packages and the datasource of a persistence unit, the persistence.xml and orm.xml processing) and
     * properties that Keycloak cannot work with (disabling Hibernate ORM or a persistence unit, multitenancy, schema
     * management and naming strategies of the Liquibase managed schema, SQL scripts, dev mode settings).
     */
    static final Set<String> HIBERNATE_ORM_PROPERTIES_NOT_EXPOSED = Set.of("packages", "datasource", "persistence-xml.ignore", "mapping-files",
            "enabled", "active", "jdbc.enabled", "reactive.enabled", "multitenant", "schema-management", "sql-load-script",
            "physical-naming-strategy", "implicit-naming-strategy", "quote-identifiers.strategy", "dev-ui.allow-hql", "validate-in-dev-mode");

    private static boolean isHibernateOrmPropertyExposed(String suffix) {
        return HIBERNATE_ORM_PROPERTIES_NOT_EXPOSED.stream().noneMatch(excluded -> suffix.equals(excluded) || suffix.startsWith(excluded + "."));
    }

    /**
     * Adds a {@code db-orm-*} mapper (see {@link DatabaseOptions#DB_ORM_PREFIX}) for every
     * {@linkplain HibernateOrmProperties Hibernate ORM property}, except {@link #HIBERNATE_ORM_PROPERTIES_NOT_EXPOSED} and the
     * properties that another option maps to, such as {@code db-dialect}. The options have no default: the Quarkus default
     * of the property applies. The options of a named datasource are validated by {@link #validateConfig}.
     *
     * @param persistenceUnitMappers receives the mappers of persistence unit properties, which also get a
     *        {@code -<datasource>} variant
     * @param globalMappers receives the mappers of the other properties
     */
    static void addHibernateOrmMappers(List<PropertyMapper<?>> persistenceUnitMappers, List<PropertyMapper<?>> globalMappers) {
        Set<String> mappedProperties = persistenceUnitMappers.stream().map(PropertyMapper::getTo).collect(Collectors.toSet());
        Map<String, HibernateOrmProperty> optionProperties = new HashMap<>();
        for (HibernateOrmProperty property : HibernateOrmProperties.getProperties().values()) {
            if (mappedProperties.contains(property.name()) || !isHibernateOrmPropertyExposed(property.suffix())) {
                continue;
            }
            String key = DatabaseOptions.DB_ORM_PREFIX + property.suffix().replace('.', '-');
            HibernateOrmProperty other = optionProperties.putIfAbsent(key, property);
            if (other != null) {
                throw new IllegalStateException("The Hibernate ORM properties '%s' and '%s' would both be exposed as the option '%s'"
                        .formatted(other.name(), property.name(), key));
            }
            (property.perUnit() ? persistenceUnitMappers : globalMappers).add(hibernateOrmMapper(key, property.type(), property));
        }
    }

    private static <T> PropertyMapper<T> hibernateOrmMapper(String key, Class<T> type, HibernateOrmProperty property) {
        Option<T> option = new OptionBuilder<>(key, type)
                .category(OptionCategory.DATABASE)
                .description("Sets the Quarkus Hibernate ORM property '%s'.".formatted(property.name()))
                .buildTime(property.buildTime())
                .cli(false)
                .defaultValue(Optional.empty()) // no default, not even for a Boolean option
                .build();
        return fromOption(option)
                .to(property.name())
                .build();
    }

    private static List<PropertyMapper<?>> namedQueryMappers() {
        Set<String> queryKeys = new TreeSet<>();

        var kindToNamedQueries = Database.getDatabaseAliases().stream()
                .map(Database::getDatabaseKind)
                .flatMap(Optional::stream)
                .distinct()
                .collect(Collectors.toMap(Function.identity(), JpaUtils::loadSpecificNamedQueries));

        kindToNamedQueries.values().forEach((namedQueries) -> queryKeys.addAll(namedQueries.stringPropertyNames()));

        List<PropertyMapper<?>> mappers = new ArrayList<>();
        for (String queryKey : queryKeys) {
            mappers.add(fromOption(SYNTHETIC_RUNTIME_DB_OPTION_NO_DEFAULT)
                    .mapFrom(DB, (name, db, context) -> db == null ? null
                            : Database.getDatabaseKind(db)
                                    .map(kindToNamedQueries::get)
                                    .map(named -> named.getProperty(queryKey))
                                    .orElse(null))
                    .to("quarkus.hibernate-orm.unsupported-properties.\"" + QUERY_PROPERTY_PREFIX + queryKey + "\"")
                    .build());
        }
        return mappers;
    }

    @Override
    public void validateConfig(Picocli picocli) {
        validateHibernateOrmOptionsOfDatasources();
        Configuration.getOptionalIntegerValue(DB_POOL_MAX_SIZE).ifPresent(poolMaxSize -> {
            if (poolMaxSize < JDBC_PING_MIN_POOL_MAX_SIZE && isJdbcPingStack()) {
                throw new PropertyException(
                        "The JDBC_PING cache stack requires '%s' to be at least %d (current: %d). A higher value is recommended."
                                .formatted(DB_POOL_MAX_SIZE.getKey(), JDBC_PING_MIN_POOL_MAX_SIZE, poolMaxSize));
            }
        });
    }

    /**
     * A {@code db-orm-*-<datasource>} option applies to the persistence unit that {@code db-jpa-packages-<datasource>}
     * defines only, see {@link Datasources#forConfiguredPersistenceUnit}: setting it without the unit is an error.
     */
    private static void validateHibernateOrmOptionsOfDatasources() {
        Map<String, Set<String>> unitless = new TreeMap<>();
        for (String name : Configuration.getPropertyNames()) {
            PropertyMapper<?> mapper = PropertyMappers.getMapper(name);
            if (mapper == null || !mapper.hasWildcard() || !mapper.getOption().getKey().startsWith(DatabaseOptions.DB_ORM_PREFIX)
                    || !name.equals(mapper.forKey(name).getFrom())) {
                continue;
            }
            String datasource = ((WildcardPropertyMapper<?>) mapper).extractWildcardValue(name).orElseThrow();
            if (!Configuration.isUserModifiable(Configuration.getConfigValue(name)) || isPersistenceUnitConfigured(datasource)) {
                continue;
            }
            unitless.computeIfAbsent(datasource, d -> new TreeSet<>()).add(name.substring(NS_KEYCLOAK_PREFIX.length()));
        }
        if (unitless.isEmpty()) {
            return;
        }
        throw new PropertyException(unitless.entrySet().stream().map(entry -> {
            Set<String> options = entry.getValue();
            return "The option%s %s appl%s to the persistence unit that '%s' defines, which is not set.".formatted(
                    options.size() == 1 ? "" : "s",
                    options.stream().map(option -> "'" + option + "'").collect(Collectors.joining(", ")),
                    options.size() == 1 ? "ies" : "y",
                    WildcardOptionsUtil.getWildcardNamedKey(DatabaseOptions.DB_JPA_PACKAGES.getKey(), entry.getKey()));
        }).collect(Collectors.joining("\n")));
    }

    private static boolean isPersistenceUnitConfigured(String datasource) {
        return Configuration.getOptionalKcValue(WildcardOptionsUtil.getWildcardNamedKey(DatabaseOptions.DB_JPA_PACKAGES.getKey(), datasource)).isPresent();
    }

    private static boolean isJdbcPingStack() {
        if (!CachingPropertyMappers.cacheSetToInfinispan()) {
            return false;
        }
        String stack = getOptionalKcValue(CachingOptions.CACHE_STACK).orElse(Stack.jdbc_ping.toString());
        return Stack.jdbc_ping.toString().equals(stack) || Stack.jdbc_ping_udp.toString().equals(stack);
    }

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

    public static boolean isPostgresqlLogServerErrorDetailEnabled() {
        String db = Configuration.getConfigValue(DB).getValue();
        Database.Vendor vendor = Database.getVendor(db).orElse(null);
        if (vendor != Database.Vendor.POSTGRES) {
            return false;
        }

        String dbUrl = Configuration.getConfigValue(DatabaseOptions.DB_URL).getValue();

        // logServerErrorDetail already set to same or different value in db-url, ignore
        return dbUrl == null || !dbUrl.contains("logServerErrorDetail");
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

    /**
     * MySQL and MariaDB default to REPEATABLE READ transaction isolation, which acquires gap locks on
     * {@code INSERT ... ON DUPLICATE KEY UPDATE} statements. When the stateless feature is enabled,
     * concurrent login requests execute such upserts on authentication session and login failure tables,
     * causing deadlocks under load. Switching to READ COMMITTED eliminates gap locks and resolves
     * these deadlocks. This matches the isolation level PostgreSQL, Oracle, and SQL Server use by default.
     */
    public static boolean isReadCommittedIsolationRequired() {
        String db = Configuration.getConfigValue(DB).getValue();
        Database.Vendor vendor = Database.getVendor(db).orElse(null);
        if (vendor != Database.Vendor.MYSQL && vendor != Database.Vendor.MARIADB && vendor != Database.Vendor.TIDB) {
            return false;
        }
        return Profile.isFeatureEnabled(Profile.Feature.STATELESS);
    }

    private static ValueMapper getConnectTimeout(Collection<Database.Vendor> validForVendors, String timeoutProperty) {
        return (String datasource, String value, ConfigSourceInterceptorContext context) -> {
            String db = getDatasourceOptionValue(DB, datasource).orElse(null);
            Database.Vendor vendor = Database.getVendor(db).orElse(null);

            if (checkSettingsAndVendor(validForVendors, timeoutProperty, datasource, vendor, db)) {
                return null;
            }

            if (vendor == Vendor.MSSQL || vendor == Vendor.POSTGRES) {
                return durationToSeconds(value);
            }
            if (vendor == Vendor.MYSQL || vendor == Vendor.MARIADB || vendor == Vendor.TIDB) {
                return durationToMillis(value);
            }

            // We don't know if it is seconds or milliseconds for other databases.
            throw new IllegalArgumentException("Vendor " + vendor + " not supported for socket timeout calculation");
        };
    }

    private static ValueMapper getOracleConnectTimeout(boolean forXa) {
        return (String datasource, String value, ConfigSourceInterceptorContext context) -> {
            String db = getDatasourceOptionValue(DB, datasource).orElse(null);
            Database.Vendor vendor = Database.getVendor(db).orElse(null);

            if (checkSettingsAndVendor(EnumSet.of(Database.Vendor.ORACLE), ORACLE_NET_CONNECT_TIMEOUT, datasource, vendor, db)) {
                return null;
            }

            var key = StringUtil.isNotBlank(datasource) ? TransactionOptions.getNamedTxXADatasource(datasource) : TransactionOptions.TRANSACTION_XA_ENABLED.getKey();
            boolean isXaEnabled = Configuration.isKcPropertyTrue(key);

            if (forXa != isXaEnabled) {
                return null;
            }

            if (forXa) {
                String connectionPropertiesKey = StringUtil.isNotBlank(datasource)
                        ? "quarkus.datasource.\"" + datasource + "\".jdbc.additional-jdbc-properties.ConnectionProperties"
                        : ORACLEDB_CONNECTION_PROPERTIES;
                ConfigValue existing = context.proceed(connectionPropertiesKey);
                if (existing != null && existing.getValue() != null) {
                    if (!existing.getValue().contains(ORACLE_NET_CONNECT_TIMEOUT)) {
                        log.warnf("Custom ConnectionProperties does not contain '%s'; the socket connect timeout will not be set.",
                                ORACLE_NET_CONNECT_TIMEOUT);
                    }
                    return null;
                }
                return ORACLE_NET_CONNECT_TIMEOUT + "=" + durationToMillis(value);
            }
            return durationToMillis(value);
        };
    }

    private static ValueMapper getSocketTimeout(Collection<Database.Vendor> validForVendors, String timeoutProperty) {
        return (String datasource, String value, ConfigSourceInterceptorContext context) -> {
            String db = getDatasourceOptionValue(DB, datasource).orElse(null);
            Database.Vendor vendor = Database.getVendor(db).orElse(null);

            if (checkSettingsAndVendor(validForVendors, timeoutProperty, datasource, vendor, db)) {
                return null;
            }

            if (vendor == Vendor.MYSQL || vendor == Vendor.MARIADB || vendor == Vendor.TIDB) {
                return durationToMillis(value);
            }

            // We don't know if it is seconds or milliseconds for other database.
            throw new IllegalArgumentException("Vendor " + vendor + " not supported for socket timeout calculation");
        };
    }

    private static boolean checkSettingsAndVendor(Collection<Vendor> validForVendors, String timeoutProperty, String datasource, Vendor vendor, String db) {
        if (!validForVendors.contains(vendor)) {
            // this jdbc property is not for this vendor
            return true;
        }

        String dbDriver = getDatasourceOptionValue(DatabaseOptions.DB_DRIVER, datasource).orElse(null);
        if (!Objects.equals(Database.getDriver(db, true).orElse(null), dbDriver) &&
                !Objects.equals(Database.getDriver(db, false).orElse(null), dbDriver)) {
            // Custom JDBC driver (e.g. AWS JDBC Wrapper) — do not inject defaults
            return true;
        }

        String dbUrl = findDatabaseUrl(datasource).orElse("");
        String dbUrlProperties = getDatasourceOptionValue(DatabaseOptions.DB_URL_PROPERTIES, datasource).orElse("");

        // Property already set explicitly by the user — do not override
        if  (dbUrl.contains(timeoutProperty) || dbUrlProperties.contains(timeoutProperty)) {
            return true;
        }
        return false;
    }

    private static String durationToMillis(String value) {
        return String.valueOf(DurationConverter.parseDuration(value).toMillis());
    }

    private static String durationToSeconds(String value) {
        return String.valueOf(DurationConverter.parseDuration(value).toSeconds());
    }

    private static String getDatabaseUrl(String name, String value, ConfigSourceInterceptorContext c) {
        return Database.getDefaultUrl(option -> getDatasourceOptionValue(option, name).orElse(null), name, value).orElse(null);
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

    /**
     * Whether the name is a key of the {@code unsupported-properties} map of a Hibernate ORM persistence unit
     * (e.g. {@code quarkus.hibernate-orm."<unit>".unsupported-properties."hibernate.use_sql_comments"}).
     * Keycloak only contributes such keys from runtime options.
     */
    public static boolean isHibernateUnsupportedProperty(String name) {
        return name.startsWith(QUARKUS_HIBERNATE_ORM_PREFIX) && name.contains(".unsupported-properties.");
    }

    /**
     * Whether the name is a property of a named Hibernate ORM persistence unit, e.g.
     * {@code quarkus.hibernate-orm."<unit>".dialect}.
     */
    public static boolean isNamedPersistenceUnitProperty(String name) {
        return name.startsWith(QUARKUS_HIBERNATE_ORM_PREFIX + "\"");
    }

    /**
     * Whether the option of the mapper takes effect for the given Quarkus property: a {@code db-orm-*-<datasource>} option
     * applies to the persistence unit that {@code db-jpa-packages-<datasource>} defines only, see
     * {@link Datasources#forConfiguredPersistenceUnit}, not to a unit of a persistence.xml file.
     */
    public static boolean appliesToPersistenceUnit(PropertyMapper<?> mapper, String quarkusProperty) {
        if (!mapper.hasWildcard() || !mapper.getOption().getKey().startsWith(DatabaseOptions.DB_ORM_PREFIX)
                || !isNamedPersistenceUnitProperty(quarkusProperty)) {
            return true;
        }
        String datasource = ((WildcardPropertyMapper<?>) mapper).extractWildcardValue(quarkusProperty).orElseThrow();
        return Configuration.getOptionalKcValue(WildcardOptionsUtil.getWildcardNamedKey(DatabaseOptions.DB_JPA_PACKAGES.getKey(), datasource)).isPresent();
    }

    public static final class Datasources extends org.keycloak.config.DatabaseOptions.Datasources {

        /**
         * Automatically create mappers for datasource options
         *
         * @param persistenceUnitMappers the mappers of the Hibernate ORM configuration of the persistence unit: for a
         *        named datasource, they target the persistence unit that {@code db-jpa-packages-<datasource>} defines,
         *        see {@link #forConfiguredPersistenceUnit}
         */
        static List<PropertyMapper<?>> appendDatasourceMappers(List<PropertyMapper<?>> mappers, List<PropertyMapper<?>> persistenceUnitMappers,
                Map<Option<?>, Consumer<PropertyMapper.Builder<?>>> transformDatasourceMappers) {
            List<PropertyMapper<?>> allMappers = Stream.concat(mappers.stream(), persistenceUnitMappers.stream()).toList();
            List<PropertyMapper<?>> datasourceMappers = new ArrayList<>(allMappers.size() * 2);

            Map<String, Option<?>> cachedDatasourceOptions = new HashMap<>();
            cachedDatasourceOptions.put(DB.getKey(), DB_KIND);
            allMappers.stream().map(PropertyMapper::getOption).forEach(o -> cachedDatasourceOptions.computeIfAbsent(o.getKey(), k -> getDatasourceOption(o)));

            for (var parent : mappers) {
                datasourceMappers.add(createDatasourceMapper(parent, transformDatasourceTo(parent.getTo()), UnaryOperator.identity(),
                        cachedDatasourceOptions, transformDatasourceMappers));
            }
            for (var parent : persistenceUnitMappers) {
                datasourceMappers.add(createDatasourceMapper(parent, transformPersistenceUnitTo(parent.getTo()), Datasources::forConfiguredPersistenceUnit,
                        cachedDatasourceOptions, transformDatasourceMappers));
            }

            datasourceMappers.addAll(allMappers);

            return datasourceMappers;
        }

        /**
         * @param to the property the datasource mapper maps to
         * @param valueMappers applied to the value mappers of the parent, the transformer and the one of mapFrom, to
         *        obtain the value mappers of the datasource mapper. Called with {@code null} for a value mapper the
         *        parent does not have.
         */
        private static PropertyMapper<?> createDatasourceMapper(PropertyMapper<?> parent, String to, UnaryOperator<ValueMapper> valueMappers,
                Map<String, Option<?>> cachedDatasourceOptions, Map<Option<?>, Consumer<PropertyMapper.Builder<?>>> transformDatasourceMappers) {
            var parentOption = parent.getOption();

            var datasourceOption = cachedDatasourceOptions.get(parentOption.getKey());

            var created = fromOption(datasourceOption)
                    .isMasked(parent.isMask())
                    .transformer(valueMappers.apply(parent.getMapper()));

            if (parent.getMapFrom() != null) {
                Option<?> mapFrom = cachedDatasourceOptions.get(parent.getMapFrom());
                if (mapFrom == null) {
                    throw new IllegalArgumentException("Option '%s' in mapFrom() method for mapper '%s' does not have any associated wildcard option".formatted(parent.getMapFrom(), datasourceOption.getKey()));
                }
                ValueMapper parentMapper = parent.getParentMapper() != null ? (name, value, context) -> parent.getParentMapper().map(name, value, context) : null;
                created.wildcardMapFrom(mapFrom, valueMappers.apply(parentMapper));
            }

            if (parent.getParamLabel() != null) {
                created.paramLabel(parent.getParamLabel());
            }

            if (to != null) {
                created.to(to);
            }

            var customTransformer = transformDatasourceMappers.get(parent.getOption());
            if (customTransformer != null) {
                customTransformer.accept(created);
            }

            Option<String> primaryOption = DB_KIND;

            PropertyMapper<?> mapper = created.build();
            // if we're not the DB option, nor mapped directly from the DB option, then
            // it's considered "connected" for the purposes of discovery
            if (parentOption != DB && !primaryOption.getKey().equals(mapper.getMapFrom())) {
                primaryOption.getConnectedOptions().add(mapper.getOption().getKey());
            }
            return mapper;
        }

        /**
         * Hibernate ORM properties of a named datasource configure the persistence unit named after the datasource,
         * which exists only when {@code db-jpa-packages-<datasource>} defines it. Otherwise the property must stay
         * unset: any build time {@code quarkus.hibernate-orm."<datasource>".*} value makes Quarkus define a persistence
         * unit of that name, which fails without packages and clashes with a persistence.xml unit of the same name.
         *
         * @param mapper the value mapper of the parent, or {@code null} to map the value as is
         */
        private static ValueMapper forConfiguredPersistenceUnit(ValueMapper mapper) {
            return (datasource, value, context) -> {
                if (!isPersistenceUnitConfigured(datasource, context)) {
                    return null;
                }
                return mapper == null ? value : mapper.map(datasource, value, context);
            };
        }

        static boolean isPersistenceUnitConfigured(String datasource, ConfigSourceInterceptorContext context) {
            String key = NS_KEYCLOAK_PREFIX + WildcardOptionsUtil.getWildcardNamedKey(DatabaseOptions.DB_JPA_PACKAGES.getKey(), datasource);
            ConfigValue packages = context.restart(key);
            return packages != null && packages.getValue() != null;
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

        /**
         * The property of the persistence unit of the named datasource, see {@link #forConfiguredPersistenceUnit}
         */
        private static String transformPersistenceUnitTo(String to) {
            if (to == null || !to.startsWith(QUARKUS_HIBERNATE_ORM_PREFIX)) {
                throw new IllegalArgumentException("A persistence unit mapper must map to a '%s' property, but it maps to '%s'".formatted(QUARKUS_HIBERNATE_ORM_PREFIX, to));
            }
            return QUARKUS_HIBERNATE_ORM_PREFIX + "\"<datasource>\"." + to.substring(QUARKUS_HIBERNATE_ORM_PREFIX.length());
        }
    }

    private static String computeOracleAuthenticationServices(String datasource, String keyStoreFile) {
        if (keyStoreFile == null) {
            return null;
        }
        var vendor = getDatabaseVendor(datasource);
        if (vendor != Database.Vendor.ORACLE) {
            return null;
        }
        var tlsMode = getDatabaseTlsMode(datasource);
        if (tlsMode != DatabaseOptions.DatabaseTlsMode.VERIFY_SERVER) {
            return null;
        }
        var jdbcUrl = findDatabaseUrl(datasource).orElse("");
        if (jdbcUrl.contains("oracle.net.authentication_services")) {
            return null;
        }
        return "(TCPS)";
    }

    private static PropertyMapper<?> setTlsJdbcProperty(String jdbcPropertyKey, Map<Database.Vendor, String> vendorValues) {
        return fromOption(SYNTHETIC_RUNTIME_DB_OPTION)
                .mapFrom(DB)
                .transformer((name, value, context) -> computeTlsProperty(vendorValues, name, value, jdbcPropertyKey))
                .to("quarkus.datasource.jdbc.additional-jdbc-properties." + jdbcPropertyKey)
                .build();
    }

    private static PropertyMapper<?> setInputTlsJdbcProperty(Option<?> from, String jdbcPropertyKey, Collection<Database.Vendor> vendorValues) {
        return fromOption(from)
                .mapFrom(from, (name, value, context) -> transformTlsUserProperty(vendorValues, name, value, jdbcPropertyKey))
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

    public static Optional<String> getDatasourceOptionValue(Option<?> opt, String datasource) {
        if (datasource == null) {
            return Configuration.getOptionalKcValue(opt);
        }
        return opt.getWildcardKey().map(k -> WildcardOptionsUtil.getWildcardNamedKey(k, datasource)).flatMap(Configuration::getOptionalKcValue);
    }

    private static Optional<String> findDatabaseUrl(String datasource) {
        return getDatasourceOptionValue(DB_URL, datasource);
    }

    private static Database.Vendor getDatabaseVendor(String datasource) {
        return getDatasourceOptionValue(DB, datasource).flatMap(Database::getVendor).orElseThrow();
    }

    private static DatabaseOptions.DatabaseTlsMode getDatabaseTlsMode(String datasource) {
        return getDatasourceOptionValue(DB_TLS_MODE, datasource)
                .map(DatabaseOptions.DatabaseTlsMode::fromCliValue)
                .orElse(DatabaseOptions.DatabaseTlsMode.DISABLED);
    }

    private static Optional<String> findTlsTrustStoreFile(String datasource) {
        return getDatasourceOptionValue(DB_TLS_TRUST_STORE_FILE, datasource);
    }

    private static void validateConnectTimeout(String value) {
        try {
            Duration duration = DurationConverter.parseDuration(value);
            if (duration == null || duration.isNegative()) {
                throw new PropertyException("Invalid duration '%s' for option '%s'. Duration must be non-negative."
                        .formatted(value, DatabaseOptions.DB_CONNECT_TIMEOUT.getKey()));
            }
        } catch (IllegalArgumentException e) {
            throw new PropertyException("Invalid duration format '%s' for option '%s'. %s"
                    .formatted(value, DatabaseOptions.DB_CONNECT_TIMEOUT.getKey(), OptionsUtil.DURATION_DESCRIPTION));
        }
    }

    private static String computeAcquisitionTimeout(String connectTimeoutValue) {
        Duration connectTimeout = DurationConverter.parseDuration(connectTimeoutValue);
        Duration transactionSetupTimeout = DurationConverter.parseDuration(
                Configuration.getKcConfigValue(TransactionOptions.TRANSACTION_SETUP_TIMEOUT.getKey()).getValue()
        );
        Duration acquisitionTimeout = connectTimeout.multipliedBy(2);
        if (acquisitionTimeout.compareTo(transactionSetupTimeout) > 0) {
            acquisitionTimeout = transactionSetupTimeout;
        }
        if (acquisitionTimeout.compareTo(connectTimeout) < 0) {
            acquisitionTimeout = connectTimeout;
        }
        return acquisitionTimeout.toString();
    }

}
