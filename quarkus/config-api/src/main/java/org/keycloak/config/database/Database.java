/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.config.database;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.keycloak.config.DatabaseOptions;
import org.keycloak.config.Option;

import io.quarkus.runtime.util.StringUtil;

import static java.util.Arrays.asList;

public final class Database {

    private static final Map<String, Vendor> DATABASES = new HashMap<>();

    static {
        for (Vendor vendor : Vendor.values()) {
            for (String alias : vendor.aliases) {
                DATABASES.put(alias, vendor);
            }
        }
    }

    public static boolean isLiquibaseDatabaseSupported(String databaseType, String dbKind) {
        for (Vendor vendor : DATABASES.values()) {
            if (vendor.liquibaseType.equals(databaseType) && vendor.isOfKind(dbKind)) {
                return true;
            }
        }

        return false;
    }

    public static Optional<Vendor> getVendor(String vendor) {
        return Arrays.stream(Vendor.values())
                .filter(v -> v.isOfKind(vendor) || asList(v.aliases).contains(vendor))
                .findAny();
    }

    public static Optional<String> getDatabaseKind(String alias) {
        return mapValue(alias, vendor -> vendor.databaseKind);
    }

    /**
     * The {@param namedProperty} represents name of the named datasource if we need to set the URL for additional datasource
     */
    public static Optional<String> getDefaultUrl(String namedProperty, String alias) {
        return getVendor(alias).map(f -> f.defaultUrl.apply(namedProperty, alias));
    }

    public static Optional<String> getDriver(String alias, boolean isXaEnabled) {
        return mapValue(alias, vendor -> isXaEnabled ? vendor.xaDriver : vendor.nonXaDriver);
    }

    public static Optional<String> getDialect(String alias) {
        return mapValue(alias, vendor -> vendor.dialect.apply(alias));
    }

    private static <T> Optional<T> mapValue(String alias, Function<Vendor, T> mapper) {
        return getVendor(alias).map(mapper);
    }


    /**
     * @return List of aliases of databases
     */
    public static List<String> getDatabaseAliases() {
        return DATABASES.keySet()
                .stream()
                .sorted()
                .collect(Collectors.toList());
    }

    public enum Vendor {
        H2("h2",
                "org.h2.jdbcx.JdbcDataSource",
                "org.h2.Driver",
                "org.hibernate.dialect.H2Dialect",
                new BiFunction<>() {
                    @Override
                    public String apply(String namedProperty, String alias) {
                        if ("dev-file".equalsIgnoreCase(alias)) {
                            var separator = escapeReplacements(File.separator);
                            return new StringBuilder()
                                    .append("jdbc:h2:file:")
                                    .append("${kc.db-url-path:${kc.home.dir:%s}}".formatted(escapeReplacements(System.getProperty("user.home"))))
                                    .append(separator)
                                    .append("${kc.data.dir:data}")
                                    .append(separator)
                                    .append(getFolder(namedProperty))
                                    .append(separator)
                                    .append(getDbName(namedProperty))
                                    .toString();
                        }
                        return "jdbc:h2:mem:%s".formatted(getDbName(namedProperty));
                    }

                    private String getFolder(String namedProperty) {
                        return StringUtil.isNullOrEmpty(namedProperty) ? "h2" : "h2-%s".formatted(namedProperty);
                    }

                    private String getDbName(String namedProperty) {
                        return StringUtil.isNullOrEmpty(namedProperty) ? "keycloakdb" : "keycloakdb-%s".formatted(namedProperty);
                    }

                    private String escapeReplacements(String snippet) {
                        if (File.separator.equals("\\")) {
                            // SmallRye will do replacements of "${...}", but a "\" must not escape such an expression.
                            // As we nest multiple expressions, and each nested expression must re-escape the backslashes,
                            // the simplest way is to replace a backslash with a slash, as those are processed nicely on Windows.
                            return snippet.replace("\\", "/");
                        }
                        return snippet;
                    }

                },
                "liquibase.database.core.H2Database",
                "dev-mem", "dev-file"
        ),
        MYSQL("mysql",
                "com.mysql.cj.jdbc.MysqlXADataSource",
                "com.mysql.cj.jdbc.Driver",
                "org.hibernate.dialect.MySQLDialect",
                // default URL looks like this: "jdbc:mysql://${kc.db-url-host:localhost}:${kc.db-url-port:3306}/${kc.db-url-database:keycloak}${kc.db-url-properties:}"
                (namedProperty, alias) -> "jdbc:mysql://%s:%s/%s%s".formatted(
                        getProperty(DatabaseOptions.DB_URL_HOST, namedProperty, "localhost"),
                        getProperty(DatabaseOptions.DB_URL_PORT, namedProperty, "3306"),
                        getProperty(DatabaseOptions.DB_URL_DATABASE, namedProperty, "keycloak"),
                        getProperty(DatabaseOptions.DB_URL_PROPERTIES, namedProperty)),
                "org.keycloak.connections.jpa.updater.liquibase.UpdatedMySqlDatabase"
        ),
        TIDB("tidb",
                "com.mysql.cj.jdbc.MysqlXADataSource",
                "com.mysql.cj.jdbc.Driver",
                "org.hibernate.community.dialect.TiDBDialect",
                // default URL looks like this: "jdbc:mysql://${kc.db-url-host:localhost}:${kc.db-url-port:3306}/${kc.db-url-database:keycloak}${kc.db-url-properties:}"
                (namedProperty, alias) -> "jdbc:mysql://%s:%s/%s%s".formatted(
                        getProperty(DatabaseOptions.DB_URL_HOST, namedProperty, "localhost"),
                        getProperty(DatabaseOptions.DB_URL_PORT, namedProperty, "3306"),
                        getProperty(DatabaseOptions.DB_URL_DATABASE, namedProperty, "keycloak"),
                        getProperty(DatabaseOptions.DB_URL_PROPERTIES, namedProperty)),
                "org.keycloak.connections.jpa.updater.liquibase.UpdatedMySqlDatabase"
        ),
        MARIADB("mariadb",
                "org.mariadb.jdbc.MariaDbDataSource",
                "org.mariadb.jdbc.Driver",
                "org.hibernate.dialect.MariaDBDialect",
                // default URL looks like this: "jdbc:mariadb://${kc.db-url-host:localhost}:${kc.db-url-port:3306}/${kc.db-url-database:keycloak}${kc.db-url-properties:}"
                (namedProperty, alias) -> "jdbc:mariadb://%s:%s/%s%s".formatted(
                        getProperty(DatabaseOptions.DB_URL_HOST, namedProperty, "localhost"),
                        getProperty(DatabaseOptions.DB_URL_PORT, namedProperty, "3306"),
                        getProperty(DatabaseOptions.DB_URL_DATABASE, namedProperty, "keycloak"),
                        getProperty(DatabaseOptions.DB_URL_PROPERTIES, namedProperty)),
                "org.keycloak.connections.jpa.updater.liquibase.UpdatedMariaDBDatabase"
        ),
        POSTGRES("postgresql",
                "org.postgresql.xa.PGXADataSource",
                "org.postgresql.Driver",
                "org.hibernate.dialect.PostgreSQLDialect",
                // default URL looks like this: "jdbc:postgresql://${kc.db-url-host:localhost}:${kc.db-url-port:5432}/${kc.db-url-database:keycloak}${kc.db-url-properties:}"
                (namedProperty, alias) -> "jdbc:postgresql://%s:%s/%s%s".formatted(
                        getProperty(DatabaseOptions.DB_URL_HOST, namedProperty, "localhost"),
                        getProperty(DatabaseOptions.DB_URL_PORT, namedProperty, "5432"),
                        getProperty(DatabaseOptions.DB_URL_DATABASE, namedProperty, "keycloak"),
                        getProperty(DatabaseOptions.DB_URL_PROPERTIES, namedProperty)),
                "liquibase.database.core.PostgresDatabase",
                "postgres"
        ),
        MSSQL("mssql",
                "com.microsoft.sqlserver.jdbc.SQLServerXADataSource",
                "com.microsoft.sqlserver.jdbc.SQLServerDriver",
                "org.hibernate.dialect.SQLServerDialect",
                // default URL looks like this: "jdbc:sqlserver://${kc.db-url-host:localhost}:${kc.db-url-port:1433};databaseName=${kc.db-url-database:keycloak}${kc.db-url-properties:}"
                (namedProperty, alias) -> "jdbc:sqlserver://%s:%s;databaseName=%s%s".formatted(
                        getProperty(DatabaseOptions.DB_URL_HOST, namedProperty, "localhost"),
                        getProperty(DatabaseOptions.DB_URL_PORT, namedProperty, "1433"),
                        getProperty(DatabaseOptions.DB_URL_DATABASE, namedProperty, "keycloak"),
                        getProperty(DatabaseOptions.DB_URL_PROPERTIES, namedProperty)),
                "org.keycloak.quarkus.runtime.storage.database.liquibase.database.CustomMSSQLDatabase",
                "mssql"
        ),
        ORACLE("oracle",
                "oracle.jdbc.xa.client.OracleXADataSource",
                "oracle.jdbc.driver.OracleDriver",
                "org.hibernate.dialect.OracleDialect",
                // default URL looks like this: "jdbc:oracle:thin:@//${kc.db-url-host:localhost}:${kc.db-url-port:1521}/${kc.db-url-database:keycloak}"
                (namedProperty, alias) -> "jdbc:oracle:thin:@//%s:%s/%s".formatted(
                        getProperty(DatabaseOptions.DB_URL_HOST, namedProperty, "localhost"),
                        getProperty(DatabaseOptions.DB_URL_PORT, namedProperty, "1521"),
                        getProperty(DatabaseOptions.DB_URL_DATABASE, namedProperty, "keycloak")),
                "liquibase.database.core.OracleDatabase"
        );

        final String databaseKind;
        final String xaDriver;
        final String nonXaDriver;
        final Function<String, String> dialect;
        final BiFunction<String, String, String> defaultUrl;
        final String liquibaseType;
        final String[] aliases;

        Vendor(String databaseKind, String xaDriver, String nonXaDriver, String dialect, BiFunction<String, String, String> defaultUrl,
               String liquibaseType, String... aliases) {
            this(databaseKind, xaDriver, nonXaDriver, alias -> dialect, defaultUrl, liquibaseType, aliases);
        }

        Vendor(String databaseKind, String xaDriver, String nonXaDriver, Function<String, String> dialect, BiFunction<String, String, String> defaultUrl,
               String liquibaseType,
               String... aliases) {
            this.databaseKind = databaseKind;
            this.xaDriver = xaDriver;
            this.nonXaDriver = nonXaDriver;
            this.dialect = dialect;
            this.defaultUrl = defaultUrl;
            this.liquibaseType = liquibaseType;
            this.aliases = aliases.length == 0 ? new String[]{databaseKind} : aliases;
        }

        public boolean isOfKind(String dbKind) {
            return databaseKind.equals(dbKind);
        }

        private static String getProperty(Option<?> option, String namedProperty) {
            return getProperty(option, namedProperty, "");
        }

        private static String getProperty(Option<?> option, String namedProperty, String defaultValue) {
            return "${kc.%s:%s}".formatted(StringUtil.isNullOrEmpty(namedProperty) ? option.getKey() :
                            DatabaseOptions.Datasources.getNamedKey(option, namedProperty).orElseThrow(() -> new IllegalArgumentException("Cannot find the named property")),
                    defaultValue);
        }

        public String getLiquibaseType() {
            return liquibaseType;
        }

        @Override
        public String toString() {
            return databaseKind.toLowerCase(Locale.ROOT);
        }
    }
}
