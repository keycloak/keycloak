package org.keycloak.config;

import org.keycloak.config.database.Database;

import java.util.ArrayList;
import java.util.List;

public class DatabaseOptions {

    public static final Option<String> DB_DIALECT = new OptionBuilder<>("db-dialect", String.class)
            .category(OptionCategory.DATABASE)
            .hidden()
            .buildTime(true)
            .build();

    public static final Option<String> DB_DRIVER = new OptionBuilder<>("db-driver", String.class)
            .category(OptionCategory.DATABASE)
            .hidden()
            .defaultValue(Database.getDriver("dev-file", true).get())
            .build();

    public static final Option<String> DB = new OptionBuilder<>("db", String.class)
            .category(OptionCategory.DATABASE)
            .description(String.format("The database vendor. Possible values are: %s.", String.join(", ", Database.getAliases())))
            .defaultValue("dev-file")
            .expectedStringValues(Database.getAliases())
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
            .description("Sets the properties of the default JDBC URL of the chosen vendor. If the `db-url` option is set, this option is ignored.")
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

    public static final List<Option<?>> ALL_OPTIONS = new ArrayList<>();

    static {
        ALL_OPTIONS.add(DB_DIALECT);
        ALL_OPTIONS.add(DB_DRIVER);
        ALL_OPTIONS.add(DB);
        ALL_OPTIONS.add(DB_URL);
        ALL_OPTIONS.add(DB_URL_HOST);
        ALL_OPTIONS.add(DB_URL_DATABASE);
        ALL_OPTIONS.add(DB_URL_PORT);
        ALL_OPTIONS.add(DB_URL_PROPERTIES);
        ALL_OPTIONS.add(DB_USERNAME);
        ALL_OPTIONS.add(DB_PASSWORD);
        ALL_OPTIONS.add(DB_SCHEMA);
        ALL_OPTIONS.add(DB_POOL_INITIAL_SIZE);
        ALL_OPTIONS.add(DB_POOL_MIN_SIZE);
        ALL_OPTIONS.add(DB_POOL_MAX_SIZE);
    }
}
