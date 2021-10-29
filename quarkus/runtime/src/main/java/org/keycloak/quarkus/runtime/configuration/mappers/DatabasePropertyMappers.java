package org.keycloak.quarkus.runtime.configuration.mappers;

import io.smallrye.config.ConfigSourceInterceptorContext;
import org.keycloak.quarkus.runtime.storage.database.Database;

import java.util.Arrays;
import java.util.function.BiFunction;

import static org.keycloak.quarkus.runtime.configuration.mappers.Messages.invalidDatabaseVendor;
import static org.keycloak.quarkus.runtime.integration.QuarkusPlatform.addInitializationException;

final class DatabasePropertyMappers {

    private static final String[] supportedDatabaseVendors = {"h2-file", "h2-mem", "mariadb", "mysql", "postgres", "postgres-95", "postgres-10"};

    private DatabasePropertyMappers(){}

    public static PropertyMapper[] getDatabasePropertyMappers() {
        return new PropertyMapper[] {
                builder().from("db")
                        .to("quarkus.hibernate-orm.dialect")
                        .isBuildTimeProperty(true)
                        .transformer((db, context) -> Database.getDialect(db).orElse(null))
                        .build(),
                builder().from("db")
                        .to("quarkus.datasource.jdbc.driver")
                        .transformer((db, context) -> Database.getDriver(db).orElse(null))
                        .build(),
                builder().from("db").
                        to("quarkus.datasource.db-kind")
                        .isBuildTimeProperty(true)
                        .transformer(getSupportedDbValue())
                        .description("The database vendor. Possible values are: " + String.join(",", supportedDatabaseVendors))
                        .expectedValues(Arrays.asList(supportedDatabaseVendors))
                        .build(),
                builder().from("db")
                        .to("quarkus.datasource.jdbc.transactions")
                        .transformer((db, context) -> "xa")
                        .build(),
                builder().from("db.url")
                        .to("quarkus.datasource.jdbc.url")
                        .mapFrom("db")
                        .transformer((value, context) -> Database.getDefaultUrl(value).orElse(value))
                        .description("The database JDBC URL. If not provided, a default URL is set based on the selected database vendor. " +
                                "For instance, if using 'postgres', the JDBC URL would be 'jdbc:postgresql://localhost/keycloak'. " +
                                "The host, database and properties can be overridden by setting the following system properties," +
                                " respectively: -Dkc.db.url.host, -Dkc.db.url.database, -Dkc.db.url.properties.")
                        .build(),
                builder().from("db.username")
                        .to("quarkus.datasource.username")
                        .description("The username of the database user.")
                        .build(),
                builder().from("db.password")
                        .to("quarkus.datasource.password")
                        .description("The password of the database user.")
                        .isMasked(true)
                        .build(),
                builder().from("db.schema")
                        .to("quarkus.datasource.schema")
                        .description("The database schema to be used.")
                        .build(),
                builder().from("db.pool.initial-size")
                        .to("quarkus.datasource.jdbc.initial-size")
                        .description("The initial size of the connection pool.")
                        .build(),
                builder().from("db.pool.min-size")
                        .to("quarkus.datasource.jdbc.min-size")
                        .description("The minimal size of the connection pool.")
                        .build(),
                builder().from("db.pool.max-size")
                        .to("quarkus.datasource.jdbc.max-size")
                        .defaultValue(String.valueOf(100))
                        .description("The maximum size of the connection pool.")
                        .build()
        };
    }

    private static BiFunction<String, ConfigSourceInterceptorContext, String> getSupportedDbValue() {
        return (db, context) -> {
            if (Database.isSupported(db)) {
                return Database.getDatabaseKind(db).orElse(db);
            }
            addInitializationException(invalidDatabaseVendor(db, "h2-file", "h2-mem", "mariadb", "mysql", "postgres", "postgres-95", "postgres-10"));
            return "h2";
        };
    }

    private static PropertyMapper.Builder builder() {
        return PropertyMapper.builder(ConfigCategory.DATABASE);
    }
}
