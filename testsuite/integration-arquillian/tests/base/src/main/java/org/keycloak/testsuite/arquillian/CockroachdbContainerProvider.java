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
import org.testcontainers.containers.CockroachContainer;
import org.testcontainers.utility.DockerImageName;

public class CockroachdbContainerProvider implements ResourceProvider {

    private static final Logger log = Logger.getLogger(CockroachdbContainerProvider.class);

    private static final Boolean START_CONTAINER = Boolean.valueOf(System.getProperty("cockroachdb.start-container", "false"));
    private static final String COCKROACHDB_DOCKER_IMAGE_NAME = System.getProperty("keycloak.map.storage.cockroachdb.docker.image", "cockroachdb/cockroach:v22.1.0");

    public static final String COCKROACHDB_DB_USER = System.getProperty("keycloak.map.storage.connectionsJpa.user", "keycloak");
    public static final String COCKROACHDB_DB_PASSWORD = System.getProperty("keycloak.map.storage.connectionsJpa.password", "pass");

    private static CockroachContainer cockroachDbContainer;

    public void beforeContainerStarted(@Observes(precedence = 1) StartSuiteContainers event) {
        if (START_CONTAINER) {
            cockroachDbContainer = createContainer();
            cockroachDbContainer.start();

            System.setProperty("keycloak.map.storage.connectionsJpa.url", cockroachDbContainer.getJdbcUrl());

            log.infof("DatabaseInfo: %s, user=%s, pass=%s", cockroachDbContainer.getJdbcUrl(), COCKROACHDB_DB_USER, COCKROACHDB_DB_PASSWORD);
        }
    }

    public void afterSuite(@Observes(precedence = 4) AfterSuite event) {
        if (START_CONTAINER) {
            cockroachDbContainer.stop();
        }
    }

    public CockroachContainer createContainer() {
        return new CockroachContainer(DockerImageName.parse(COCKROACHDB_DOCKER_IMAGE_NAME).asCompatibleSubstituteFor("cockroachdb"))
                // Using the environment variables for now where using the withXXX() method is not supported, yet.
                // https://github.com/testcontainers/testcontainers-java/issues/6299
                .withEnv("COCKROACH_DATABASE", "keycloak")
                .withEnv("COCKROACH_USER", COCKROACHDB_DB_USER)
                // password is not used/supported in insecure mode
                .withCommand("start-single-node", "--insecure");
    }

    public CockroachContainer getContainer() {
        return cockroachDbContainer;
    }

    @Override
    public boolean canProvide(Class<?> type) {
        return type.equals(CockroachdbContainerProvider.class);
    }

    @Override
    public Object lookup(ArquillianResource ar, Annotation... antns) {
        return this;
    }
}
