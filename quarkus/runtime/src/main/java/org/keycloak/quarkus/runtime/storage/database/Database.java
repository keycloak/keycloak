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

package org.keycloak.quarkus.runtime.storage.database;

import static java.util.Arrays.asList;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

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
            if (vendor.liquibaseTypes.contains(databaseType) && vendor.isOfKind(dbKind)) {
                return true;
            }
        }

        return false;
    }

    public static Optional<String> getDatabaseKind(String alias) {
        Vendor vendor = DATABASES.get(alias);

        if (vendor == null) {
            return Optional.empty();
        }

        return Optional.of(vendor.databaseKind);
    }

    public static Optional<String> getDefaultUrl(String alias) {
        Vendor vendor = DATABASES.get(alias);

        if (vendor == null) {
            return Optional.empty();
        }

        return Optional.of(vendor.defaultUrl.apply(alias));
    }

    public static Optional<String> getDriver(String alias) {
        Vendor vendor = DATABASES.get(alias);

        if (vendor == null) {
            return Optional.empty();
        }

        return Optional.of(vendor.driver);
    }

    public static Optional<String> getDialect(String alias) {
        Vendor vendor = DATABASES.get(alias);

        if (vendor == null) {
            return Optional.empty();
        }

        return Optional.of(vendor.dialect.apply(alias));
    }

    public static String[] getAliases() {
        return DATABASES.keySet().stream().sorted().toArray(String[]::new);
    }

    private enum Vendor {
        H2("h2",
                "org.h2.jdbcx.JdbcDataSource",
                "io.quarkus.hibernate.orm.runtime.dialect.QuarkusH2Dialect",
                new Function<String, String>() {
                    @Override
                    public String apply(String alias) {
                        if ("dev-file".equalsIgnoreCase(alias)) {
                            return "jdbc:h2:file:${kc.home.dir:${kc.db-url-path:~}}" + File.separator + "${kc.data.dir:data}"
                                    + File.separator + "h2" + File.separator
                                    + "keycloakdb${kc.db-url-properties:;;AUTO_SERVER=TRUE}";
                        }
                        return "jdbc:h2:mem:keycloakdb${kc.db-url-properties:}";
                    }
                },
                asList("liquibase.database.core.H2Database"),
                "dev-mem", "dev-file"
        ),
        MYSQL("mysql",
                "com.mysql.cj.jdbc.MysqlXADataSource",
                "org.hibernate.dialect.MySQL8Dialect",

                "jdbc:mysql://${kc.db-url-host:localhost}/${kc.db-url-database:keycloak}${kc.db-url-properties:}",
                asList("org.keycloak.connections.jpa.updater.liquibase.UpdatedMySqlDatabase")
        ),
        MARIADB("mariadb",
                "org.mariadb.jdbc.MySQLDataSource",
                "org.hibernate.dialect.MariaDBDialect",
                "jdbc:mariadb://${kc.db-url-host:localhost}/${kc.db-url-database:keycloak}${kc.db-url-properties:}",
                asList("org.keycloak.connections.jpa.updater.liquibase.UpdatedMariaDBDatabase")
        ),
        POSTGRES("postgresql",
                "org.postgresql.xa.PGXADataSource",
                "io.quarkus.hibernate.orm.runtime.dialect.QuarkusPostgreSQL10Dialect",
                "jdbc:postgresql://${kc.db-url-host:localhost}/${kc.db-url-database:keycloak}${kc.db-url-properties:}",
                asList("liquibase.database.core.PostgresDatabase",
                        "org.keycloak.connections.jpa.updater.liquibase.PostgresPlusDatabase"),
                "postgres"
        ),
        MSSQL("mssql",
                "com.microsoft.sqlserver.jdbc.SQLServerXADataSource",
                "org.hibernate.dialect.SQLServer2016Dialect",
                "jdbc:sqlserver://${kc.db-url-host:localhost}:1433;databaseName=${kc.db-url-database:keycloak}${kc.db-url-properties:}",
                asList("org.keycloak.quarkus.runtime.storage.database.liquibase.database.CustomMSSQLDatabase"),
                "mssql"
        ),
        ORACLE("oracle",
                "oracle.jdbc.xa.client.OracleXADataSource",
                "org.hibernate.dialect.Oracle12cDialect",
                "jdbc:oracle:thin:@//${kc.db-url-host:localhost}:1521/${kc.db-url-database:keycloak}",
                asList("liquibase.database.core.OracleDatabase")
        );

        final String databaseKind;
        final String driver;
        final Function<String, String> dialect;
        final Function<String, String> defaultUrl;
        final List<String> liquibaseTypes;
        final String[] aliases;

        Vendor(String databaseKind, String driver, String dialect, String defaultUrl, List<String> liquibaseTypes,
                String... aliases) {
            this(databaseKind, driver, alias -> dialect, alias -> defaultUrl, liquibaseTypes, aliases);
        }

        Vendor(String databaseKind, String driver, String dialect, Function<String, String> defaultUrl,
                List<String> liquibaseTypes, String... aliases) {
            this(databaseKind, driver, alias -> dialect, defaultUrl, liquibaseTypes, aliases);
        }

        Vendor(String databaseKind, String driver, Function<String, String> dialect, String defaultUrl,
                List<String> liquibaseTypes, String... aliases) {
            this(databaseKind, driver, dialect, alias -> defaultUrl, liquibaseTypes, aliases);
        }

        Vendor(String databaseKind, String driver, Function<String, String> dialect, Function<String, String> defaultUrl,
                List<String> liquibaseTypes,
                String... aliases) {
            this.databaseKind = databaseKind;
            this.driver = driver;
            this.dialect = dialect;
            this.defaultUrl = defaultUrl;
            this.liquibaseTypes = liquibaseTypes;
            this.aliases = aliases.length == 0 ? new String[] { databaseKind } : aliases;
        }

        public boolean isOfKind(String dbKind) {
            return databaseKind.equals(dbKind);
        }
    }
}
