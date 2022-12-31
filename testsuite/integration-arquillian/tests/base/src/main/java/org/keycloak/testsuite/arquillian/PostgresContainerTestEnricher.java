/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.testsuite.arquillian;

import org.jboss.arquillian.container.spi.event.StartSuiteContainers;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.test.spi.event.suite.AfterSuite;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

public class PostgresContainerTestEnricher {

    private static final Boolean START_CONTAINER = Boolean.valueOf(System.getProperty("postgres.start-container", "false"));
    private static final String POSTGRES_DOCKER_IMAGE_NAME = System.getProperty("keycloak.map.storage.postgres.docker.image", "postgres:alpine");
    private static final PostgreSQLContainer POSTGRES_CONTAINER = new PostgreSQLContainer(DockerImageName.parse(POSTGRES_DOCKER_IMAGE_NAME).asCompatibleSubstituteFor("postgres"));
    private static final String POSTGRES_DB_USER = System.getProperty("keycloak.map.storage.connectionsJpa.user", "keycloak");
    private static final String POSTGRES_DB_PASSWORD = System.getProperty("keycloak.map.storage.connectionsJpa.password", "pass");

    public void beforeContainerStarted(@Observes(precedence = 1) StartSuiteContainers event) {
        if (START_CONTAINER) {
            POSTGRES_CONTAINER
                    .withDatabaseName("keycloak")
                    .withUsername(POSTGRES_DB_USER)
                    .withPassword(POSTGRES_DB_PASSWORD)
                    .start();

            System.setProperty("keycloak.map.storage.connectionsJpa.url", POSTGRES_CONTAINER.getJdbcUrl());
        }
    }

    public void afterSuite(@Observes(precedence = 4) AfterSuite event) {
        if (START_CONTAINER) {
            POSTGRES_CONTAINER.stop();
        }
    }
}
