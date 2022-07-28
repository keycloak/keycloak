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

package org.keycloak.it.junit5.extension;

import java.time.Duration;

import org.keycloak.it.utils.KeycloakDistribution;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.DockerImageName;

public class DatabaseContainer {

    static final String DEFAULT_PASSWORD = "Password1!";

    private final String alias;
    private GenericContainer<?> container;

    DatabaseContainer(String alias) {
        this.alias = alias;
    }

    void start() {
        container = createContainer();
        container.withStartupTimeout(Duration.ofMinutes(5)).start();
    }

    boolean isRunning() {
        return container.isRunning();
    }

    void configureDistribution(KeycloakDistribution dist) {
        if (alias.equals("infinispan")) {
            dist.setProperty("storage-hotrod-username", getUsername());
            dist.setProperty("storage-hotrod-password", getPassword());
            dist.setProperty("storage-hotrod-host", container.getContainerIpAddress());
            dist.setProperty("storage-hotrod-port", String.valueOf(container.getMappedPort(11222)));
        } else {
            dist.setProperty("db-username", getUsername());
            dist.setProperty("db-password", getPassword());
            dist.setProperty("db-url", getJdbcUrl());
        }

    }

    private String getJdbcUrl() {
        return ((JdbcDatabaseContainer)container).getJdbcUrl();
    }

    String getUsername() {
        return "keycloak";
    }

    String getPassword() {
        return DEFAULT_PASSWORD;
    }

    void stop() {
        container.stop();
        container = null;
    }

    private GenericContainer<?> configureJdbcContainer(JdbcDatabaseContainer jdbcDatabaseContainer) {
        return jdbcDatabaseContainer
                .withDatabaseName("keycloak")
                .withUsername(getUsername())
                .withPassword(getPassword())
                .withInitScript(resolveInitScript());
    }

    private GenericContainer<?> configureInfinispanUser(GenericContainer<?> infinispanContainer) {
        infinispanContainer.addEnv("USER", getUsername());
        infinispanContainer.addEnv("PASS", getPassword());
        return infinispanContainer;
    }

    private GenericContainer<?> createContainer() {
        String POSTGRES_IMAGE = System.getProperty("kc.db.postgresql.container.image", "postgres:alpine");
        String MARIADB_IMAGE = System.getProperty("kc.db.mariadb.container.image", "mariadb:10.5.9");
        String MYSQL_IMAGE = System.getProperty("kc.db.mysql.container.image", "mysql:latest");

        DockerImageName POSTGRES = DockerImageName.parse(POSTGRES_IMAGE).asCompatibleSubstituteFor("postgres");
        DockerImageName MARIADB = DockerImageName.parse(MARIADB_IMAGE).asCompatibleSubstituteFor("mariadb");
        DockerImageName MYSQL = DockerImageName.parse(MYSQL_IMAGE).asCompatibleSubstituteFor("mysql");

        switch (alias) {
            case "postgres":
                return configureJdbcContainer(new PostgreSQLContainer(POSTGRES));
            case "mariadb":
                return configureJdbcContainer(new MariaDBContainer(MARIADB));
            case "mysql":
                return configureJdbcContainer(new MySQLContainer(MYSQL));
            case "infinispan":
                return configureInfinispanUser(new GenericContainer("quay.io/infinispan/server:12.1.7.Final"))
                        .withExposedPorts(11222);
            default:
                throw new RuntimeException("Unsupported database: " + alias);
        }
    }

    private String resolveInitScript() {
        return String.format("database/scripts/init-%s.sql", alias);
    }
}
