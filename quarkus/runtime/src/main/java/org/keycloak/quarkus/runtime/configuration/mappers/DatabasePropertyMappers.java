package org.keycloak.quarkus.runtime.configuration.mappers;

import io.quarkus.datasource.common.runtime.DatabaseKind;
import io.quarkus.logging.Log;
import io.smallrye.config.ConfigSourceInterceptorContext;
import org.keycloak.config.DatabaseOptions;
import org.keycloak.config.Option;
import org.keycloak.config.TransactionOptions;
import org.keycloak.config.database.Database;
import org.keycloak.quarkus.runtime.configuration.Configuration;
import org.keycloak.utils.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import static org.keycloak.config.DatabaseOptions.OPTIONS_DATASOURCES;
import static org.keycloak.config.DatabaseOptions.getDatasourceOption;
import static org.keycloak.quarkus.runtime.configuration.mappers.PropertyMapper.fromOption;

final class DatabasePropertyMappers {

    private DatabasePropertyMappers(){}

    public static PropertyMapper<?>[] getDatabasePropertyMappers() {
        var mappers = new PropertyMapper<?>[]{
                fromOption(DatabaseOptions.DB_DIALECT)
                        .mapFrom(DatabaseOptions.DB, DatabasePropertyMappers::transformDialect)
                        .build(),
                /*fromOption(DB_DIALECT_DATASOURCE)
                        .wildcardMapFrom(getDatasourceOption(DatabaseOptions.DB), (name, value, context) -> transformDialect(value, context))
                        .build(),*/
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
                fromOption(DatabaseOptions.DB_URL_HOST)
                        .to("kc.db-url-host")
                        .paramLabel("hostname")
                        .build(),
                fromOption(DatabaseOptions.DB_URL_DATABASE)
                        .to("kc.db-url-database")
                        .paramLabel("dbname")
                        .build(),
                fromOption(DatabaseOptions.DB_URL_PORT)
                        .to("kc.db-url-port")
                        .paramLabel("port")
                        .build(),
                fromOption(DatabaseOptions.DB_URL_PROPERTIES)
                        .to("kc.db-url-properties")
                        .paramLabel("properties")
                        .build(),
                fromOption(DatabaseOptions.DB_USERNAME)
                        .to("quarkus.datasource.username")
                        .transformer(DatabasePropertyMappers::resolveUsername)
                        .paramLabel("username")
                        .build(),
                fromOption(DatabaseOptions.DB_PASSWORD)
                        .to("quarkus.datasource.password")
                        .transformer(DatabasePropertyMappers::resolvePassword)
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
                        .to("quarkus.datasource.jdbc.min-size")
                        .transformer(DatabasePropertyMappers::transformMinPoolSize)
                        .paramLabel("size")
                        .build(),
                fromOption(DatabaseOptions.DB_POOL_MAX_SIZE)
                        .to("quarkus.datasource.jdbc.max-size")
                        .paramLabel("size")
                        .build()
        };

        return appendDatasourceMappers(mappers, Map.of(
                // Inherit options from the DB mappers
                DatabaseOptions.DB_POOL_INITIAL_SIZE, mapper -> mapper.mapFrom(DatabaseOptions.DB_POOL_INITIAL_SIZE),
                DatabaseOptions.DB_POOL_MIN_SIZE, mapper -> mapper.mapFrom(DatabaseOptions.DB_POOL_MIN_SIZE),
                DatabaseOptions.DB_POOL_MAX_SIZE, mapper -> mapper.mapFrom(DatabaseOptions.DB_POOL_MAX_SIZE)
        ));
    }

    private static String getDatabaseUrl(String value, ConfigSourceInterceptorContext c) {
        return Database.getDefaultUrl(value).orElse(null);
    }

    private static String getXaOrNonXaDriver(String value, ConfigSourceInterceptorContext context) {
        Optional<String> xaEnabledConfigValue = Configuration.getOptionalKcValue(TransactionOptions.TRANSACTION_XA_ENABLED);
        boolean isXaEnabled = xaEnabledConfigValue.map(Boolean::parseBoolean).orElse(false);

        return Database.getDriver(value, isXaEnabled).orElse(null);
    }

    private static String toDatabaseKind(String db, ConfigSourceInterceptorContext context) {
        return Database.getDatabaseKind(db).orElse(null);
    }

    private static String resolveUsername(String value, ConfigSourceInterceptorContext context) {
        if (isDevModeDatabase(context)) {
            return "sa";
        }

        return value;
    }

    private static String resolvePassword(String value, ConfigSourceInterceptorContext context) {
        if (isDevModeDatabase(context)) {
            return "password";
        }

        return value;
    }

    private static boolean isDevModeDatabase(ConfigSourceInterceptorContext context) {
        String db = Configuration.getConfig().getConfigValue("kc.db").getValue();
        return Database.getDatabaseKind(db).filter(DatabaseKind.H2::equals).isPresent();
    }

    private static String transformDialect(String db, ConfigSourceInterceptorContext context) {
        return Database.getDialect(db).orElse(null);
    }

    /**
     * For H2 databases we must ensure that the min-pool size is at least one so that the DB is not shutdown until the
     * Agroal connection pool is closed on Keycloak shutdown.
     */
    private static String transformMinPoolSize(String min, ConfigSourceInterceptorContext context) {
        return isDevModeDatabase(context) && (min == null || "0".equals(min)) ? "1" : min;
    }

    private static Set<String> getCategories(Set<String> categories) {
        return categories;
    }

    // Datasources

    /**
     * Automatically create mappers for datasource options
     */
    private static PropertyMapper<?>[] appendDatasourceMappers(PropertyMapper<?>[] mappers, Map<Option<?>, Consumer<PropertyMapper.Builder<?>>> transformDatasourceMappers) {
        List<PropertyMapper<?>> datasourceMappers = new ArrayList<>(mappers.length + OPTIONS_DATASOURCES.size());

        for (var parent : mappers) {
            if (!OPTIONS_DATASOURCES.contains(parent.getOption())) {
                continue;
            }

            var dsOption = getDatasourceOption(parent.getOption());
            var created = fromOption(dsOption)
                    .isMasked(parent.isMask())
                    .transformer(parent.getMapper());

            // set transformer
            if (parent.getMapFrom() != null) {
                var parentFromOption = Arrays.stream(mappers)
                        .filter(f -> parent.getMapFrom().equals(f.getOption().getKey()))
                        .findFirst()
                        .map(PropertyMapper::getOption)
                        .orElseThrow(() -> new RuntimeException(String.format("Cannot find parent option defined as '.mapFrom(%s)'", parent.getMapFrom())));

                created.wildcardMapFrom(Objects.requireNonNull(getDatasourceOption(parentFromOption)), parent.getParentMapper() != null ? (name, value, context) -> parent.getParentMapper().apply(value, context) : null);
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

        datasourceMappers.addAll(List.of(mappers));

        return datasourceMappers.toArray(new PropertyMapper[0]);
    }

    private static String transformDatasourceTo(String to) {
        if (StringUtil.isBlank(to)) {
            return null;
        }

        if (to.startsWith("quarkus.datasource.")) {
            return to.replaceFirst("quarkus\\.datasource\\.", "quarkus.datasource.\"<datasource>\".");
        } else if (to.startsWith("kc.db-")) {
            return to.replaceFirst("kc\\.db-", "kc.db-<datasource>-");
        } else {
            Log.warnf("Cannot determine how to map datasource option to '%s'", to);
        }
        return to;
    }
}
