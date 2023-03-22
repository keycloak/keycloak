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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    public static Optional<String> getDriver(String alias, boolean isXaEnabled) {
        Vendor vendor = DATABASES.get(alias);

        if (vendor == null) {
            return Optional.empty();
        }

        if (isXaEnabled) {
            return Optional.of(vendor.xaDriver);
        }

        return Optional.of(vendor.nonXaDriver);
    }

    public static Optional<String> getDialect(String alias) {
        Vendor vendor = DATABASES.get(alias);

        if (vendor == null) {
            return Optional.empty();
        }

        return Optional.of(vendor.dialect.apply(alias));
    }

    /**
     * @return List of aliases of databases enabled on legacy store.
     */
    public static List<String> getLegacyStoreAliases() {
        return DATABASES.entrySet().stream()
                .filter(e -> e.getValue().isEnabledOnLegacyStore())
                .map(Entry::getKey)
                .sorted()
                .collect(Collectors.toList());
    }

    /**
     * @return List of aliases of databases enabled on jpa map store.
     */
    public static List<String> getAvailableMapStoreAliases() {
        return Stream.of(Database.Vendor.values())
                .filter(Database.Vendor::isEnabledOnNewStore)
                .map(v -> v.aliases)// may be replaced by Database.Vendor::getAliases if we add the getter into Database.Vendor
                .flatMap(Stream::of)
                .collect(Collectors.toList());
    }

    public enum Vendor {
        H2("h2",
                Enabled.LEGACY_ONLY, 
                "org.h2.jdbcx.JdbcDataSource",
                "org.h2.Driver",
                "io.quarkus.hibernate.orm.runtime.dialect.QuarkusH2Dialect",
                new Function<String, String>() {
                    @Override
                    public String apply(String alias) {
                        if ("dev-file".equalsIgnoreCase(alias)) {
                            return addH2NonKeywords("jdbc:h2:file:${kc.home.dir:${kc.db-url-path:" + escapeReplacements(System.getProperty("user.home")) + "}}" + escapeReplacements(File.separator) + "${kc.data.dir:data}"
                                    + escapeReplacements(File.separator) + "h2" + escapeReplacements(File.separator)
                                    + "keycloakdb${kc.db-url-properties:;;AUTO_SERVER=TRUE}");
                        }
                        return addH2NonKeywords("jdbc:h2:mem:keycloakdb${kc.db-url-properties:}");
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
                    private String addH2NonKeywords(String jdbcUrl) {
                        if (!jdbcUrl.contains("NON_KEYWORDS=")) {
                            jdbcUrl = jdbcUrl + ";NON_KEYWORDS=VALUE";
                        }
                        return jdbcUrl;
                    }
                },
                asList("liquibase.database.core.H2Database"),
                "dev-mem", "dev-file"
        ),
        MYSQL("mysql",
                Enabled.LEGACY_ONLY,
                "com.mysql.cj.jdbc.MysqlXADataSource",
                "com.mysql.cj.jdbc.Driver",
                "org.hibernate.dialect.MySQL8Dialect",
                "jdbc:mysql://${kc.db-url-host:localhost}:${kc.db-url-port:3306}/${kc.db-url-database:keycloak}${kc.db-url-properties:}",
                asList("org.keycloak.connections.jpa.updater.liquibase.UpdatedMySqlDatabase")
        ),
        MARIADB("mariadb",
                Enabled.LEGACY_ONLY,
                "org.mariadb.jdbc.MariaDbDataSource",
                "org.mariadb.jdbc.Driver",
                "org.hibernate.dialect.MariaDBDialect",
                "jdbc:mariadb://${kc.db-url-host:localhost}:${kc.db-url-port:3306}/${kc.db-url-database:keycloak}${kc.db-url-properties:}",
                asList("org.keycloak.connections.jpa.updater.liquibase.UpdatedMariaDBDatabase")
        ),
        POSTGRES("postgresql",
                Enabled.ENABLED,
                "org.postgresql.xa.PGXADataSource",
                "org.postgresql.Driver",
                "io.quarkus.hibernate.orm.runtime.dialect.QuarkusPostgreSQL10Dialect",
                "jdbc:postgresql://${kc.db-url-host:localhost}:${kc.db-url-port:5432}/${kc.db-url-database:keycloak}${kc.db-url-properties:}",
                asList("liquibase.database.core.PostgresDatabase", "org.keycloak.connections.jpa.updater.liquibase.PostgresPlusDatabase"),
                "postgres"
        ),
        COCKROACH(POSTGRES.databaseKind, //needs to be aligned with https://quarkus.io/guides/datasource#default-datasource
                Enabled.MAP_STORE_ONLY,
                POSTGRES.xaDriver,
                POSTGRES.nonXaDriver,
                "org.hibernate.dialect.CockroachDB201Dialect",
                "jdbc:postgresql://${kc.db-url-host:localhost}:${kc.db-url-port:26257}/${kc.db-url-database:keycloak}${kc.db-url-properties:}",
                List.of("liquibase.database.core.CockroachDatabase"),
                "cockroach"
        ),
        MSSQL("mssql",
                Enabled.LEGACY_ONLY,
                "com.microsoft.sqlserver.jdbc.SQLServerXADataSource",
                "com.microsoft.sqlserver.jdbc.SQLServerDriver",
                "org.hibernate.dialect.SQLServer2016Dialect",
                "jdbc:sqlserver://${kc.db-url-host:localhost}:${kc.db-url-port:1433};databaseName=${kc.db-url-database:keycloak}${kc.db-url-properties:}",
                asList("org.keycloak.quarkus.runtime.storage.database.liquibase.database.CustomMSSQLDatabase"),
                "mssql"
        ),
        ORACLE("oracle",
                Enabled.LEGACY_ONLY,
                "oracle.jdbc.xa.client.OracleXADataSource",
                "oracle.jdbc.driver.OracleDriver",
                "org.hibernate.dialect.Oracle12cDialect",
                "jdbc:oracle:thin:@//${kc.db-url-host:localhost}:${kc.db-url-port:1521}/${kc.db-url-database:keycloak}",
                asList("liquibase.database.core.OracleDatabase")
        );

        final String databaseKind;
        final Enabled enabled;
        final String xaDriver;
        final String nonXaDriver;
        final Function<String, String> dialect;
        final Function<String, String> defaultUrl;
        final List<String> liquibaseTypes;
        final String[] aliases;

        Vendor(String databaseKind, Enabled enabled, String xaDriver, String nonXaDriver, String dialect, String defaultUrl, List<String> liquibaseTypes,
               String... aliases) {
            this(databaseKind, enabled, xaDriver, nonXaDriver, alias -> dialect, alias -> defaultUrl, liquibaseTypes, aliases);
        }

        Vendor(String databaseKind, Enabled enabled, String xaDriver, String nonXaDriver, String dialect, Function<String, String> defaultUrl,
               List<String> liquibaseTypes, String... aliases) {
            this(databaseKind, enabled, xaDriver, nonXaDriver, alias -> dialect, defaultUrl, liquibaseTypes, aliases);
        }

        Vendor(String databaseKind, Enabled enabled, String xaDriver, String nonXaDriver, Function<String, String> dialect, Function<String, String> defaultUrl,
               List<String> liquibaseTypes,
               String... aliases) {
            this.databaseKind = databaseKind;
            this.enabled = enabled;
            this.xaDriver = xaDriver;
            this.nonXaDriver = nonXaDriver;
            this.dialect = dialect;
            this.defaultUrl = defaultUrl;
            this.liquibaseTypes = liquibaseTypes;
            this.aliases = aliases.length == 0 ? new String[] { databaseKind } : aliases;
        }

        public boolean isEnabledOnLegacyStore() {
            return enabled.legacyStore;
        }

        public boolean isEnabledOnNewStore() {
            return enabled.jpaMapStore;
        }

        public boolean isOfKind(String dbKind) {
            return databaseKind.equals(dbKind);
        }

        @Override
        public String toString() {
            return databaseKind.toLowerCase(Locale.ROOT);
        }
    }

    private static class Enabled {
        final static Enabled LEGACY_ONLY = new Enabled(true, false);
        final static Enabled MAP_STORE_ONLY = new Enabled(false, true);
        final static Enabled ENABLED = new Enabled(true, true);

        final boolean legacyStore;
        final boolean jpaMapStore;

        private Enabled(boolean legacyStore, boolean jpaMapStore) {
            this.legacyStore = legacyStore;
            this.jpaMapStore = jpaMapStore;
        }
    }
}
