package org.keycloak.quarkus.runtime.configuration.mappers;

import io.quarkus.datasource.common.runtime.DatabaseKind;
import io.smallrye.config.ConfigSourceInterceptorContext;
import io.smallrye.config.ConfigValue;
import org.keycloak.config.DatabaseOptions;
import org.keycloak.config.database.Database;

import java.util.Optional;

import static java.util.Optional.of;
import static org.keycloak.quarkus.runtime.Messages.invalidDatabaseVendor;
import static org.keycloak.quarkus.runtime.configuration.mappers.PropertyMapper.fromOption;
import static org.keycloak.quarkus.runtime.integration.QuarkusPlatform.addInitializationException;

final class DatabasePropertyMappers {

    private DatabasePropertyMappers(){}

    public static PropertyMapper[] getDatabasePropertyMappers() {
        return new PropertyMapper[] {
                fromOption(DatabaseOptions.DB_DIALECT)
                        .mapFrom("db")
                        .to("quarkus.hibernate-orm.dialect")
                        .transformer(DatabasePropertyMappers::transformDialect)
                        .build(),
                fromOption(DatabaseOptions.DB_DRIVER)
                        .mapFrom("db")
                        .to("quarkus.datasource.jdbc.driver")
                        .transformer(DatabasePropertyMappers::getXaOrNonXaDriver)
                        .build(),
                fromOption(DatabaseOptions.DB)
                        .to("quarkus.datasource.db-kind")
                        .transformer(DatabasePropertyMappers::toDatabaseKind)
                        .paramLabel("vendor")
                        .build(),
                fromOption(DatabaseOptions.DB_URL)
                        .to("quarkus.datasource.jdbc.url")
                        .mapFrom("db")
                        .transformer(DatabasePropertyMappers::getDatabaseUrl)
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
                        .mapFrom("db")
                        .transformer(DatabasePropertyMappers::resolveUsername)
                        .paramLabel("username")
                        .build(),
                fromOption(DatabaseOptions.DB_PASSWORD)
                        .to("quarkus.datasource.password")
                        .mapFrom("db")
                        .transformer(DatabasePropertyMappers::resolvePassword)
                        .paramLabel("password")
                        .isMasked(true)
                        .build(),
                fromOption(DatabaseOptions.DB_SCHEMA)
                        .to("quarkus.hibernate-orm.database.default-schema")
                        .paramLabel("schema")
                        .build(),
                fromOption(DatabaseOptions.DB_POOL_INITIAL_SIZE)
                        .to("quarkus.datasource.jdbc.initial-size")
                        .paramLabel("size")
                        .build(),
                fromOption(DatabaseOptions.DB_POOL_MIN_SIZE)
                        .to("quarkus.datasource.jdbc.min-size")
                        .paramLabel("size")
                        .build(),
                fromOption(DatabaseOptions.DB_POOL_MAX_SIZE)
                        .to("quarkus.datasource.jdbc.max-size")
                        .paramLabel("size")
                        .build()
        };
    }

    private static Optional<String> getDatabaseUrl(Optional<String> value, ConfigSourceInterceptorContext c) {
        Optional<String> url = Database.getDefaultUrl(value.get());

        if (url.isPresent()) {
            return url;
        }

        return value;
    }

    private static Optional<String> getXaOrNonXaDriver(Optional<String> value, ConfigSourceInterceptorContext context) {
        ConfigValue xaEnabledConfigValue = context.proceed("kc.transaction-xa-enabled");
            ConfigValue jtaEnabledConfiguration = context.proceed("kc.transaction-jta-enabled");

        boolean isXaEnabled = xaEnabledConfigValue == null || Boolean.parseBoolean(xaEnabledConfigValue.getValue());
        boolean isJtaEnabled = jtaEnabledConfiguration == null || Boolean.parseBoolean(jtaEnabledConfiguration.getValue());

        if(!isJtaEnabled) {
            isXaEnabled = false;
        }

        Optional<String> driver = Database.getDriver(value.get(), isXaEnabled);

        if (driver.isPresent()) {
            return driver;
        }

        return value;
    }

    private static Optional<String> toDatabaseKind(Optional<String> db, ConfigSourceInterceptorContext context) {
        Optional<String> databaseKind = Database.getDatabaseKind(db.get());

        if (databaseKind.isPresent()) {
            return databaseKind;
        }

        addInitializationException(invalidDatabaseVendor(db.get(), Database.getAliases()));

        return of("h2");
    }

    private static Optional<String> resolveUsername(Optional<String> value, ConfigSourceInterceptorContext context) {
        if (isDevModeDatabase(context)) {
            return of("sa");
        }

        return Database.getDatabaseKind(value.get()).isEmpty() ? value : null;
    }

    private static Optional<String> resolvePassword(Optional<String> value, ConfigSourceInterceptorContext context) {
        if (isDevModeDatabase(context)) {
            return of("password");
        }

        return Database.getDatabaseKind(value.get()).isEmpty() ? value : null;
    }

    private static boolean isDevModeDatabase(ConfigSourceInterceptorContext context) {
        String db = context.proceed("kc.db").getValue();
        return Database.getDatabaseKind(db).get().equals(DatabaseKind.H2);
    }

    private static Optional<String> transformDialect(Optional<String> db, ConfigSourceInterceptorContext context) {
        Optional<String> databaseKind = Database.getDatabaseKind(db.get());

        if (databaseKind.isEmpty()) {
            return db;
        }

        Optional<String> dialect = Database.getDialect(db.get());

        if (dialect.isPresent()) {
            return dialect;
        }

        return Database.getDialect("dev-file");
    }
}
