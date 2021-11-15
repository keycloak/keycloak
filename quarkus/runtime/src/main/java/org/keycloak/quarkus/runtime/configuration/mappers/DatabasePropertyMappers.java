package org.keycloak.quarkus.runtime.configuration.mappers;

import io.smallrye.config.ConfigSourceInterceptorContext;
import org.keycloak.quarkus.runtime.storage.database.Database;

import java.util.Optional;
import java.util.function.BiFunction;

import static java.util.Arrays.asList;
import static org.keycloak.quarkus.runtime.configuration.mappers.Messages.invalidDatabaseVendor;
import static org.keycloak.quarkus.runtime.integration.QuarkusPlatform.addInitializationException;

final class DatabasePropertyMappers {

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
                        .transformer(toDatabaseKind())
                        .description("The database vendor. Possible values are: " + String.join(", ", Database.getAliases()))
                        .paramLabel("vendor")
                        .expectedValues(asList(Database.getAliases()))
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
                        .paramLabel("jdbc-url")
                        .build(),
                builder().from("db.username")
                        .to("quarkus.datasource.username")
                        .description("The username of the database user.")
                        .paramLabel("username")
                        .build(),
                builder().from("db.password")
                        .to("quarkus.datasource.password")
                        .description("The password of the database user.")
                        .paramLabel("password")
                        .isMasked(true)
                        .build(),
                builder().from("db.schema")
                        .to("quarkus.datasource.schema")
                        .description("The database schema to be used.")
                        .paramLabel("schema")
                        .build(),
                builder().from("db.pool.initial-size")
                        .to("quarkus.datasource.jdbc.initial-size")
                        .description("The initial size of the connection pool.")
                        .paramLabel("size")
                        .build(),
                builder().from("db.pool.min-size")
                        .to("quarkus.datasource.jdbc.min-size")
                        .description("The minimal size of the connection pool.")
                        .paramLabel("size")
                        .build(),
                builder().from("db.pool.max-size")
                        .to("quarkus.datasource.jdbc.max-size")
                        .defaultValue(String.valueOf(100))
                        .description("The maximum size of the connection pool.")
                        .paramLabel("size")
                        .build()
        };
    }

    private static BiFunction<String, ConfigSourceInterceptorContext, String> toDatabaseKind() {
        return (db, context) -> {
            Optional<String> databaseKind = Database.getDatabaseKind(db);

            if (databaseKind.isPresent()) {
                return databaseKind.get();
            }

            addInitializationException(invalidDatabaseVendor(db, Database.getAliases()));

            return null;
        };
    }

    private static PropertyMapper.Builder builder() {
        return PropertyMapper.builder(ConfigCategory.DATABASE);
    }
}
