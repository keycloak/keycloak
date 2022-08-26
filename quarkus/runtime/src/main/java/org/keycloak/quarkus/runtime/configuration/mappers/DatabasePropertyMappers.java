package org.keycloak.quarkus.runtime.configuration.mappers;

import io.quarkus.datasource.common.runtime.DatabaseKind;
import io.smallrye.config.ConfigSourceInterceptorContext;
import io.smallrye.config.ConfigValue;
import org.keycloak.config.DatabaseOptions;
import org.keycloak.config.StorageOptions;
import org.keycloak.config.database.Database;
import org.keycloak.quarkus.runtime.configuration.Configuration;

import java.util.Optional;

import static java.util.Optional.of;
import static org.keycloak.config.StorageOptions.STORAGE;
import static org.keycloak.quarkus.runtime.Messages.invalidDatabaseVendor;
import static org.keycloak.quarkus.runtime.configuration.Configuration.getRawValue;
import static org.keycloak.quarkus.runtime.configuration.MicroProfileConfigProvider.NS_KEYCLOAK_PREFIX;
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
                        .transformer(DatabasePropertyMappers::resolveDatabaseVendor)
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
            if (isJpaStore()) {
                return Database.getDefaultUrl(Database.Vendor.POSTGRES.name().toLowerCase());
            }
            return url;
        }

        return value;
    }

    private static Optional<String> getXaOrNonXaDriver(Optional<String> value, ConfigSourceInterceptorContext context) {
        if (isJpaStore()) {
            return Database.getDriver(Database.Vendor.POSTGRES.name().toLowerCase(), false);
        }

        ConfigValue xaEnabledConfigValue = context.proceed("kc.transaction-xa-enabled");
        ConfigValue jtaEnabledConfiguration = context.proceed("kc.transaction-jta-enabled");

        boolean isXaEnabled = xaEnabledConfigValue == null || Boolean.parseBoolean(xaEnabledConfigValue.getValue());
        boolean isJtaEnabled = jtaEnabledConfiguration == null || Boolean.parseBoolean(jtaEnabledConfiguration.getValue());

        if (!isJtaEnabled) {
            isXaEnabled = false;
        }

        Optional<String> driver = Database.getDriver(value.get(), isXaEnabled);

        if (driver.isPresent()) {
            return driver;
        }

        return value;
    }

    private static Optional<String> toDatabaseKind(Optional<String> db, ConfigSourceInterceptorContext context) {
        if (isJpaStore()) {
            return Database.getDatabaseKind(Database.Vendor.POSTGRES.name().toLowerCase());
        }

        Optional<String> databaseKind = Database.getDatabaseKind(db.get());

        if (databaseKind.isPresent()) {
            return databaseKind;
        }

        addInitializationException(invalidDatabaseVendor(db.get(), Database.getAliases()));

        return of("h2");
    }

    private static Optional<String> resolveDatabaseVendor(Optional<String> db, ConfigSourceInterceptorContext context) {
        if (isJpaStore()) {
            return of(Database.Vendor.POSTGRES.name().toLowerCase());
        }

        if (db.isEmpty()) {
            return of("dev-file");
        }

        return db;
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
        if (isJpaStore()) {
            return false;
        }

        String db = Configuration.getConfig().getConfigValue("kc.db").getValue();
        return Database.getDatabaseKind(db).get().equals(DatabaseKind.H2);
    }

    private static Optional<String> transformDialect(Optional<String> db, ConfigSourceInterceptorContext context) {
        if (isJpaStore()) {
            return of("org.keycloak.models.map.storage.jpa.hibernate.dialect.JsonbPostgreSQL95Dialect");
        }

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

    private static String getDefaultVendor() {
        if (isJpaStore()) {
            return Database.Vendor.POSTGRES.name().toLowerCase();
        }

        return "dev-file";
    }

    private static boolean isJpaStore() {
        String storage = getRawValue(NS_KEYCLOAK_PREFIX.concat(STORAGE.getKey()));
        return storage != null && StorageOptions.StorageType.jpa.name().equals(storage);
    }
}
