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
import org.jetbrains.annotations.NotNull;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

public class DatabaseContainer {

    static final String DEFAULT_PASSWORD = "Password1!";

    private final String alias;
    private JdbcDatabaseContainer container;

    DatabaseContainer(String alias) {
        this.alias = alias;
    }

    void start() {
        container = createContainer()
                .withDatabaseName("keycloak")
                .withUsername(getUsername())
                .withPassword(getPassword())
                .withInitScript(resolveInitScript());

        container.withStartupTimeout(Duration.ofMinutes(5)).start();
    }

    boolean isRunning() {
        return container.isRunning();
    }

    String getJdbcUrl() {
        return container.getJdbcUrl();
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

    private JdbcDatabaseContainer createContainer() {

        String POSTGRES_IMAGE = System.getProperty("kc.db.postgresql.container.image", "postgres:alpine");
        String MARIADB_IMAGE = System.getProperty("kc.db.mariadb.container.image", "mariadb:10.5.9");

        DockerImageName POSTGRES = DockerImageName.parse(POSTGRES_IMAGE).asCompatibleSubstituteFor("postgres");
        DockerImageName MARIADB = DockerImageName.parse(MARIADB_IMAGE).asCompatibleSubstituteFor("mariadb");

        switch (alias) {
            case "postgres":
                return new PostgreSQLContainer(POSTGRES);
            case "mariadb":
                return new MariaDBContainer(MARIADB);
            default:
                throw new RuntimeException("Unsupported database: " + alias);
        }
    }

    private String resolveInitScript() {
        return String.format("database/scripts/init-%s.sql", alias);
    }
}
