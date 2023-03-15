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
package org.keycloak.testsuite.arquillian;

import java.lang.annotation.Annotation;
import org.jboss.arquillian.container.spi.event.StartSuiteContainers;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.test.spi.enricher.resource.ResourceProvider;
import org.jboss.arquillian.test.spi.event.suite.AfterSuite;
import org.jboss.logging.Logger;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

public class PostgresContainerProvider implements ResourceProvider {

    private final Logger log = Logger.getLogger(PostgresContainerProvider.class);

    private static final Boolean START_CONTAINER = Boolean.valueOf(System.getProperty("postgres.start-container", "false"));
    private static final String POSTGRES_DOCKER_IMAGE_NAME = System.getProperty("keycloak.map.storage.postgres.docker.image", "postgres:alpine");

    public static final String POSTGRES_DB_USER = System.getProperty("keycloak.map.storage.connectionsJpa.user", "keycloak");
    public static final String POSTGRES_DB_PASSWORD = System.getProperty("keycloak.map.storage.connectionsJpa.password", "pass");

    private static PostgreSQLContainer postgresContainer;

    public void beforeContainerStarted(@Observes(precedence = 1) StartSuiteContainers event) {
        if (START_CONTAINER) {
            postgresContainer = createContainer();
            postgresContainer.start();
            
            System.setProperty("keycloak.map.storage.connectionsJpa.url", postgresContainer.getJdbcUrl());

            log.infof("DatabaseInfo: %s, user=%s, pass=%s", postgresContainer.getJdbcUrl(), POSTGRES_DB_USER, POSTGRES_DB_PASSWORD);
        }
    }

    public void afterSuite(@Observes(precedence = 4) AfterSuite event) {
        if (START_CONTAINER) {
            postgresContainer.stop();
        }
    }

    public PostgreSQLContainer createContainer() {
        return new PostgreSQLContainer(DockerImageName.parse(POSTGRES_DOCKER_IMAGE_NAME).asCompatibleSubstituteFor("postgres"))
                .withDatabaseName("keycloak")
                .withUsername(POSTGRES_DB_USER)
                .withPassword(POSTGRES_DB_PASSWORD);
    }

    public PostgreSQLContainer getContainer() {
        return postgresContainer;
    }

    @Override
    public boolean canProvide(Class<?> type) {
        return type.equals(PostgresContainerProvider.class);
    }

    @Override
    public Object lookup(ArquillianResource ar, Annotation... antns) {
        return this;
    }

    
}
