/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.testsuite.federation.storage;

import java.util.Arrays;
import org.jboss.arquillian.container.spi.Container;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.junit.BeforeClass;
import org.keycloak.testsuite.arquillian.ContainerInfo;
import org.keycloak.testsuite.arquillian.SuiteContext;
import org.keycloak.testsuite.arquillian.containers.KeycloakQuarkusServerDeployableContainer;
import org.keycloak.testsuite.util.ContainerAssume;

public class ClientStorageOnQuarkusTest extends ClientStorageTest {

    @BeforeClass
    public static void enabled() {
        ContainerAssume.assumeAuthServerQuarkus();
    }

    @ArquillianResource
    SuiteContext suiteContext;

    @Override
    public void testClientStats() throws Exception {
        try {
            enablePreLoadOfflineSessions();
            super.testClientStats();
        } finally {
            reset();
        }
    }

    private void enablePreLoadOfflineSessions() throws Exception {
        KeycloakQuarkusServerDeployableContainer container = getQuarkusContainer();

        container.setAdditionalBuildArgs(Arrays.asList("--spi-user-sessions-infinispan-preload-offline-sessions-from-database=true"));
        container.restartServer();
        reconnectAdminClient();
    }

    private void reset() {
        KeycloakQuarkusServerDeployableContainer container = getQuarkusContainer();
        container.resetConfiguration();
    }

    private KeycloakQuarkusServerDeployableContainer getQuarkusContainer() {
        ContainerInfo authServerInfo = suiteContext.getAuthServerInfo();
        Container arquillianContainer = authServerInfo.getArquillianContainer();
        KeycloakQuarkusServerDeployableContainer container = (KeycloakQuarkusServerDeployableContainer) arquillianContainer.getDeployableContainer();
        return container;
    }
}
