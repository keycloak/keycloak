package org.keycloak.quarkus.runtime.configuration.mappers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.keycloak.config.DatabaseOptions;
import org.keycloak.config.Option;
import org.keycloak.config.OptionBuilder;
import org.keycloak.config.TransactionOptions;
import org.keycloak.config.database.Database;
import org.keycloak.quarkus.runtime.configuration.Configuration;
import org.keycloak.utils.StringUtil;

import io.quarkus.datasource.common.runtime.DatabaseKind;
import io.smallrye.config.ConfigSourceInterceptorContext;
import io.smallrye.config.ConfigValue;
import org.jboss.logging.Logger;

import static org.keycloak.config.DatabaseOptions.DB;
import static org.keycloak.config.DatabaseOptions.Datasources.OPTIONS_DATASOURCES;
import static org.keycloak.config.DatabaseOptions.Datasources.getDatasourceOption;
import static org.keycloak.config.DatabaseOptions.Datasources.getKeyForDatasource;
import static org.keycloak.quarkus.runtime.configuration.MicroProfileConfigProvider.NS_KEYCLOAK_PREFIX;
import static org.keycloak.quarkus.runtime.configuration.mappers.DatabasePropertyMappers.Datasources.appendDatasourceMappers;
import static org.keycloak.quarkus.runtime.configuration.mappers.PropertyMapper.fromOption;

final class DatabasePropertyMappers implements PropertyMapperGrouping {
    private static final Logger log = Logger.getLogger(DatabasePropertyMappers.class);

    @Override
    public List<PropertyMapper<?>> getPropertyMappers() {
        List<PropertyMapper<?>> mappers = List.of(
                fromOption(DatabaseOptions.DB_DIALECT)
                        .mapFrom(DatabaseOptions.DB, DatabasePropertyMappers::transformDialect)
                        .build(),
                fromOption(DatabaseOptions.DB_DRIVER)
                        .mapFrom(DatabaseOptions.DB, DatabasePropertyMappers::getXaOrNonXaDriver)
                        .to("quarkus.datasource.jdbc.driver")
                        .paramLabel("driver")
                        .build(),
                fromOption(DatabaseOptions.DB)
                        .to("quarkus.datasource.db-kind")
                        .transformer(DatabasePropertyMappers::toDatabaseKind)
                        .paramLabel("vendor")
                        .build(),
                fromOption(DatabaseOptions.DB_URL)
                        .to("quarkus.datasource.jdbc.url")
                        .mapFrom(DatabaseOptions.DB, DatabasePropertyMappers::getDatabaseUrl)
                        .paramLabel("jdbc-url")
                        .build(),
                fromOption(DatabaseOptions.DB_POSTGRESQL_TARGET_SERVER_TYPE)
                        .to("quarkus.datasource.jdbc.additional-jdbc-properties.targetServerType")
                        .mapFrom(DatabaseOptions.DB, DatabasePropertyMappers::getPostgresqlTargetServerType)
                        .isEnabled(() -> getPostgresqlTargetServerType(Configuration.getConfigValue(DB).getValue(), null) != null)
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
                fromOption(DatabaseOptions.DB_POOL_INITIAL_SIZE)
                        .to("quarkus.datasource.jdbc.initial-size")
                        .paramLabel("size")
                        .build(),
                fromOption(DatabaseOptions.DB_POOL_MIN_SIZE)
                        .mapFrom(DatabaseOptions.DB, DatabasePropertyMappers::transformMinPoolSize)
                        .to("quarkus.datasource.jdbc.min-size")
                        .paramLabel("size")
                        .build(),
                fromOption(DatabaseOptions.DB_POOL_MAX_SIZE)
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
                        .build()
        );

        return appendDatasourceMappers(mappers, Map.of(
                // Inherit options from the DB mappers
                DatabaseOptions.DB, PropertyMapper.Builder::removeMapFrom,
                DatabaseOptions.DB_POOL_INITIAL_SIZE, mapper -> mapper.mapFrom(DatabaseOptions.DB_POOL_INITIAL_SIZE),
                DatabaseOptions.DB_POOL_MAX_SIZE, mapper -> mapper.mapFrom(DatabaseOptions.DB_POOL_MAX_SIZE)
        ));
    }

    private static final Option<String> DB_URL_PATH = new OptionBuilder<>("db-url-path", String.class)
            .hidden()
            .description("Used for internal purposes of H2 database.")
            .build();

    private static String getPostgresqlTargetServerType(String db, ConfigSourceInterceptorContext context) {
        Database.Vendor vendor = Database.getVendor(db).orElse(null);
        if (vendor != Database.Vendor.POSTGRES) {
            return null;
        }

        String dbDriver = Configuration.getConfigValue(DatabaseOptions.DB_DRIVER).getValue();
        String dbUrl = Configuration.getConfigValue(DatabaseOptions.DB_URL).getValue();

        if (!Objects.equals(Database.getDriver(db, true).orElse(null), dbDriver) &&
                !Objects.equals(Database.getDriver(db, false).orElse(null), dbDriver)) {
            // Custom JDBC-Driver, for example, AWS JDBC Wrapper.
            return null;
        }
        if (dbUrl != null && dbUrl.contains("targetServerType")) {
            // targetServerType already set to same or different value in db-url, ignore
            return null;
        }
        log.debug("setting targetServerType for PostgreSQL to 'primary'");
        return "primary";
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

                datasourceMappers.add(created.build());
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
}
