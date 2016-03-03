/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedHashSet;
import java.util.Set;

import org.jboss.arquillian.container.spi.Container;
import org.jboss.arquillian.container.spi.ContainerRegistry;
import org.jboss.arquillian.container.spi.event.StartContainer;
import org.jboss.arquillian.container.spi.event.StartSuiteContainers;
import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.core.api.Event;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.InstanceProducer;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.test.spi.annotation.ClassScoped;
import org.jboss.arquillian.test.spi.annotation.SuiteScoped;
import org.jboss.arquillian.test.spi.event.suite.BeforeClass;
import org.jboss.arquillian.test.spi.event.suite.BeforeSuite;
import org.jboss.logging.Logger;
import org.keycloak.testsuite.util.LogChecker;

/**
 *
 * @author tkyjovsk
 * @author vramik
 */
public class AuthServerTestEnricher {

    protected final Logger log = Logger.getLogger(this.getClass());

    @Inject
    private Instance<ContainerRegistry> containerRegistry;

    @Inject
    private Instance<ContainerController> containerController;

    @Inject
    private Event<StartContainer> startContainerEvent;

    private static final String AUTH_SERVER_CONTAINER_PROPERTY = "auth.server.container";
    private static final String AUTH_SERVER_CONTAINER_DEFAULT = "auth-server-undertow";

    private static final String MIGRATED_AUTH_SERVER_CONTAINER_PROPERTY = "migrated.auth.server.container";

    @Inject
    @SuiteScoped
    private InstanceProducer<SuiteContext> suiteContextProducer;
    private SuiteContext suiteContext;

    @Inject
    @ClassScoped
    private InstanceProducer<TestContext> testContextProducer;

    public static String getAuthServerQualifier() {
        return System.getProperty(AUTH_SERVER_CONTAINER_PROPERTY, AUTH_SERVER_CONTAINER_DEFAULT);
    }

    public static String getMigratedAuthServerQualifier() {
        return System.getProperty(MIGRATED_AUTH_SERVER_CONTAINER_PROPERTY); // == null if migration not enabled
    }

    public static String getAuthServerContextRoot() {
        return getAuthServerContextRoot(0);
    }

    public static String getAuthServerContextRoot(int clusterPortOffset) {
        int httpPort = Integer.parseInt(System.getProperty("auth.server.http.port")); // property must be set
        int httpsPort = Integer.parseInt(System.getProperty("auth.server.https.port")); // property must be set
        boolean sslRequired = Boolean.parseBoolean(System.getProperty("auth.server.ssl.required"));

        return sslRequired
                ? "https://localhost:" + (httpsPort + clusterPortOffset)
                : "http://localhost:" + (httpPort + clusterPortOffset);
    }

    public void initializeSuiteContext(@Observes(precedence = 2) BeforeSuite event) {

        Set<ContainerInfo> containers = new LinkedHashSet<>();
        for (Container c : containerRegistry.get().getContainers()) {
            containers.add(new ContainerInfo(c));
        }

        suiteContext = new SuiteContext(containers);

        String authServerQualifier = getAuthServerQualifier();
        String migratedAuthServerQualifier = getMigratedAuthServerQualifier();

        // init authServerInfo and authServerBackendsInfo
        if (authServerQualifier.startsWith("auth-server-")) {

            boolean authServerCluster = authServerQualifier.endsWith("-cluster");

            String authServerType = authServerQualifier.replaceAll("auth-server-", "").replaceAll("-cluster", "");

            log.info("authServerType:" + authServerType);

            String authServerFrontend = authServerCluster
                    ? "auth-server-" + authServerType + "-balancer" // in cluster mode the load-balancer container serves as auth server frontend
                    : authServerQualifier; // single-node mode
            String authServerBackend = "auth-server-" + authServerType + "-backend";
            int backends = 0;
            for (ContainerInfo container : suiteContext.getContainers()) {
                // frontend
                if (container.getQualifier().equals(authServerFrontend)) {
                    updateWithAuthServerInfo(container);
                    suiteContext.setAuthServerInfo(container);
                }
                // backends
                if (container.getQualifier().startsWith(authServerBackend)) {
                    updateWithAuthServerInfo(container, ++backends);
                    suiteContext.getAuthServerBackendsInfo().add(container);
                }
            }

            // validate auth server setup
            if (suiteContext.getAuthServerInfo() == null) {
                throw new RuntimeException(String.format("No auth server activated. A container matching '%s' needs to be enabled in arquillian.xml.", authServerFrontend));
            }
            if (authServerCluster && suiteContext.getAuthServerBackendsInfo().isEmpty()) {
                throw new RuntimeException(String.format("No cluster backend nodes activated. Containers matching '%sN' need to be enabled in arquillian.xml.", authServerBackend));
            }

        } else {
            throw new IllegalArgumentException(String.format("Value of %s should start with 'auth-server-' prefix.", AUTH_SERVER_CONTAINER_PROPERTY));
        }

        if (migratedAuthServerQualifier != null) {
            // init migratedAuthServerInfo
            if (migratedAuthServerQualifier.startsWith("migrated-auth-server-")) {
                for (ContainerInfo container : suiteContext.getContainers()) {
                    // migrated auth server
                    if (container.getQualifier().equals(migratedAuthServerQualifier)) {
                        updateWithAuthServerInfo(container);
                        suiteContext.setMigratedAuthServerInfo(container);
                    }
                }
            } else {
                throw new IllegalArgumentException(String.format("Value of %s should start with 'migrated-auth-server-' prefix.", MIGRATED_AUTH_SERVER_CONTAINER_PROPERTY));
            }
            // validate setup
            if (suiteContext.getMigratedAuthServerInfo() == null) {
                throw new RuntimeException(String.format("Migration test was enabled but no auth server from which to migrate was activated. "
                        + "A container matching '%s' needs to be enabled in arquillian.xml.", migratedAuthServerQualifier));
            }
        }

        suiteContextProducer.set(suiteContext);
        log.info("\n\n" + suiteContext);
    }

    private ContainerInfo updateWithAuthServerInfo(ContainerInfo authServerInfo) {
        return updateWithAuthServerInfo(authServerInfo, 0);
    }

    private ContainerInfo updateWithAuthServerInfo(ContainerInfo authServerInfo, int clusterPortOffset) {
        try {
            authServerInfo.setContextRoot(new URL(getAuthServerContextRoot(clusterPortOffset)));
        } catch (MalformedURLException ex) {
            throw new IllegalArgumentException(ex);
        }
        return authServerInfo;
    }

    public void startMigratedContainer(@Observes(precedence = 2) StartSuiteContainers event) {
        if (suiteContext.isAuthServerMigrationEnabled()) {
            log.info("\n\n### Starting keycloak " + System.getProperty("version", "- previous") + " ###\n");
            startContainerEvent.fire(new StartContainer(suiteContext.getMigratedAuthServerInfo().getArquillianContainer()));
        }
    }

    public void stopMigratedContainer(@Observes(precedence = 1) StartSuiteContainers event) {
        if (suiteContext.isAuthServerMigrationEnabled()) {
            containerController.get().stop(suiteContext.getAuthServerInfo().getQualifier());
        }
    }

    public void checkServerLogs(@Observes(precedence = -1) BeforeSuite event) throws IOException, InterruptedException {
        boolean checkLog = System.getProperty("auth.server.log.check", "true").equals("true");
        if (checkLog && suiteContext.getAuthServerInfo().isJBossBased()) {
            String jbossHomePath = suiteContext.getAuthServerInfo().getProperties().get("jbossHome");
            LogChecker.checkJBossServerLog(jbossHomePath);
        }
    }

    public void initializeTestContext(@Observes(precedence = 2) BeforeClass event) {
        TestContext testContext = new TestContext(suiteContext, event.getTestClass().getJavaClass());
        testContextProducer.set(testContext);
    }

}
