/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.quarkus.runtime.configuration;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.keycloak.common.Profile;
import org.keycloak.config.HealthOptions;
import org.keycloak.config.MetricsOptions;
import org.keycloak.config.database.Database;

import static java.util.Collections.emptySet;

import static org.keycloak.quarkus.runtime.Environment.getCurrentOrCreateFeatureProfile;

/**
 * Ignore particular artifacts based on build configuration
 */
public class IgnoredArtifacts {

    public static Set<String> getDefaultIgnoredArtifacts() {
        return Stream.of(
                        fips(),
                        jdbcDrivers(),
                        health(),
                        metrics()
                )
                .flatMap(Collection::stream)
                .collect(Collectors.toUnmodifiableSet());
    }

    // FIPS
    public static final Set<String> FIPS_ENABLED = Set.of(
            "org.bouncycastle:bcprov-jdk18on",
            "org.bouncycastle:bcpkix-jdk18on",
            "org.bouncycastle:bcutil-jdk18on",
            "org.keycloak:keycloak-crypto-default"
    );

    public static final Set<String> FIPS_DISABLED = Set.of(
            "org.keycloak:keycloak-crypto-fips1402",
            "org.bouncycastle:bc-fips",
            "org.bouncycastle:bctls-fips",
            "org.bouncycastle:bcpkix-fips",
            "org.bouncycastle:bcutil-fips"
    );

    private static Set<String> fips() {
        final Profile profile = getCurrentOrCreateFeatureProfile();
        boolean isFipsEnabled = profile.getFeatures().get(Profile.Feature.FIPS);

        return isFipsEnabled ? FIPS_ENABLED : FIPS_DISABLED;
    }

    // JDBC Drivers
    public static final Set<String> JDBC_H2 = Set.of(
            "io.quarkus:quarkus-jdbc-h2",
            "io.quarkus:quarkus-jdbc-h2-deployment",
            "com.h2database:h2",
            "org.locationtech.jts:jts-core"
    );

    public static final Set<String> JDBC_POSTGRES = Set.of(
            "io.quarkus:quarkus-jdbc-postgresql",
            "io.quarkus:quarkus-jdbc-postgresql-deployment",
            "org.postgresql:postgresql"
    );

    public static final Set<String> JDBC_MARIADB = Set.of(
            "io.quarkus:quarkus-jdbc-mariadb",
            "io.quarkus:quarkus-jdbc-mariadb-deployment",
            "org.mariadb.jdbc:mariadb-java-client"
    );

    public static final Set<String> JDBC_MYSQL = Set.of(
            "io.quarkus:quarkus-jdbc-mysql",
            "io.quarkus:quarkus-jdbc-mysql-deployment",
            "com.mysql:mysql-connector-j"
    );

    public static final Set<String> JDBC_MSSQL = Set.of(
            "io.quarkus:quarkus-jdbc-mssql",
            "io.quarkus:quarkus-jdbc-mssql-deployment",
            "com.microsoft.sqlserver:mssql-jdbc"
    );

    public static final Set<String> JDBC_ORACLE = Set.of(
            "io.quarkus:quarkus-jdbc-oracle",
            "io.quarkus:quarkus-jdbc-oracle-deployment",
            "com.oracle.database.jdbc:ojdbc17",
            "com.oracle.database.nls:orai18n"
    );

    public static final Set<String> JDBC_DRIVERS = Stream.of(
                    JDBC_H2,
                    JDBC_POSTGRES,
                    JDBC_MARIADB,
                    JDBC_MYSQL,
                    JDBC_MSSQL,
                    JDBC_ORACLE
            )
            .flatMap(Collection::stream)
            .collect(Collectors.toUnmodifiableSet());

    private static Set<String> jdbcDrivers() {
        final Set<Database.Vendor> vendorsOfAllDatasources = new HashSet<>();

        Configuration.getConfig().getPropertyNames().forEach(p -> {
            if (p.startsWith("quarkus.datasource.") && p.endsWith(".db-kind")) {
                Configuration.getOptionalValue(p)
                        .flatMap(Database::getVendor)
                        .ifPresent(vendorsOfAllDatasources::add);
            }
        });

        final Set<String> jdbcArtifacts = vendorsOfAllDatasources.stream()
                .map(vendor -> switch (vendor) {
                    case H2 -> JDBC_H2;
                    case MYSQL, TIDB -> JDBC_MYSQL;
                    case MARIADB -> JDBC_MARIADB;
                    case POSTGRES -> JDBC_POSTGRES;
                    case MSSQL -> JDBC_MSSQL;
                    case ORACLE -> JDBC_ORACLE;
                })
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());

        final Set<String> allJdbcDrivers = new HashSet<>(JDBC_DRIVERS);
        allJdbcDrivers.removeAll(jdbcArtifacts);
        return allJdbcDrivers;
    }

    // Health
    public static final Set<String> HEALTH = Set.of(
            "io.quarkus:quarkus-smallrye-health",
            "io.quarkus:quarkus-smallrye-health-deployment"
    );

    private static Set<String> health() {
        boolean isHealthEnabled = Configuration.isTrue(HealthOptions.HEALTH_ENABLED);
        return !isHealthEnabled ? HEALTH : emptySet();
    }

    // Metrics
    public static Set<String> METRICS = Set.of(
            "io.quarkus:quarkus-micrometer",
            "io.quarkus:quarkus-micrometer-deployment",
            "io.quarkus:quarkus-micrometer-registry-prometheus",
            "io.quarkus:quarkus-micrometer-registry-prometheus-deployment"
    );

    private static Set<String> metrics() {
        boolean isMetricsEnabled = Configuration.isTrue(MetricsOptions.METRICS_ENABLED);
        return !isMetricsEnabled ? METRICS : emptySet();
    }
}
