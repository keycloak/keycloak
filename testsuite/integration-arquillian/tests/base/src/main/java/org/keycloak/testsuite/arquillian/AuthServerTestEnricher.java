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

import org.apache.commons.lang.StringUtils;
import org.jboss.arquillian.container.spi.ContainerRegistry;
import org.jboss.arquillian.container.spi.event.StartContainer;
import org.jboss.arquillian.container.spi.event.StartSuiteContainers;
import org.jboss.arquillian.container.spi.event.StopContainer;
import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.core.api.Event;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.InstanceProducer;
import org.jboss.arquillian.core.api.annotation.ApplicationScoped;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.test.spi.annotation.ClassScoped;
import org.jboss.arquillian.test.spi.annotation.SuiteScoped;
import org.jboss.arquillian.test.spi.event.suite.AfterClass;
import org.jboss.arquillian.test.spi.event.suite.BeforeClass;
import org.jboss.arquillian.test.spi.event.suite.BeforeSuite;
import org.jboss.logging.Logger;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.services.error.KeycloakErrorHandler;
import org.keycloak.testsuite.arquillian.annotation.UncaughtServerErrorExpected;
import org.keycloak.testsuite.client.KeycloakTestingClient;
import org.keycloak.testsuite.util.LogChecker;
import org.keycloak.testsuite.util.OAuthClient;
import org.keycloak.testsuite.util.SystemInfoHelper;
import org.wildfly.extras.creaper.commands.undertow.AddUndertowListener;
import org.wildfly.extras.creaper.commands.undertow.RemoveUndertowListener;
import org.wildfly.extras.creaper.commands.undertow.SslVerifyClient;
import org.wildfly.extras.creaper.commands.undertow.UndertowListenerType;
import org.wildfly.extras.creaper.core.CommandFailedException;
import org.keycloak.testsuite.util.TextFileChecker;
import org.wildfly.extras.creaper.core.ManagementClient;
import org.wildfly.extras.creaper.core.online.CliException;
import org.wildfly.extras.creaper.core.online.OnlineManagementClient;
import org.wildfly.extras.creaper.core.online.OnlineOptions;
import org.wildfly.extras.creaper.core.online.operations.Address;
import org.wildfly.extras.creaper.core.online.operations.OperationException;
import org.wildfly.extras.creaper.core.online.operations.Operations;
import org.wildfly.extras.creaper.core.online.operations.admin.Administration;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.ws.rs.NotFoundException;
import org.jboss.arquillian.test.spi.event.suite.After;
import org.jboss.arquillian.test.spi.event.suite.Before;
import org.junit.Assert;

/**
 *
 * @author tkyjovsk
 * @author vramik
 */
public class AuthServerTestEnricher {

    protected static final Logger log = Logger.getLogger(AuthServerTestEnricher.class);

    @Inject
    private Instance<ContainerController> containerConroller;
    @Inject
    private Instance<ContainerRegistry> containerRegistry;

    @Inject
    private Event<StartContainer> startContainerEvent;
    @Inject
    private Event<StopContainer> stopContainerEvent;

    protected static final boolean AUTH_SERVER_SSL_REQUIRED = Boolean.parseBoolean(System.getProperty("auth.server.ssl.required", "true"));

    public static final String AUTH_SERVER_CONTAINER_DEFAULT = "auth-server-undertow";
    public static final String AUTH_SERVER_CONTAINER_PROPERTY = "auth.server.container";
    public static final String AUTH_SERVER_CONTAINER = System.getProperty(AUTH_SERVER_CONTAINER_PROPERTY, AUTH_SERVER_CONTAINER_DEFAULT);

    public static final String AUTH_SERVER_BACKEND_DEFAULT = AUTH_SERVER_CONTAINER + "-backend";
    public static final String AUTH_SERVER_BACKEND_PROPERTY = "auth.server.backend";
    public static final String AUTH_SERVER_BACKEND = System.getProperty(AUTH_SERVER_BACKEND_PROPERTY, AUTH_SERVER_BACKEND_DEFAULT);

    public static final String AUTH_SERVER_BALANCER_DEFAULT = "auth-server-balancer";
    public static final String AUTH_SERVER_BALANCER_PROPERTY = "auth.server.balancer";
    public static final String AUTH_SERVER_BALANCER = System.getProperty(AUTH_SERVER_BALANCER_PROPERTY, AUTH_SERVER_BALANCER_DEFAULT);

    public static final String AUTH_SERVER_CLUSTER_PROPERTY = "auth.server.cluster";
    public static final boolean AUTH_SERVER_CLUSTER = Boolean.parseBoolean(System.getProperty(AUTH_SERVER_CLUSTER_PROPERTY, "false"));
    public static final String AUTH_SERVER_CROSS_DC_PROPERTY = "auth.server.crossdc";
    public static final boolean AUTH_SERVER_CROSS_DC = Boolean.parseBoolean(System.getProperty(AUTH_SERVER_CROSS_DC_PROPERTY, "false"));

    public static final String CACHE_SERVER_LIFECYCLE_SKIP_PROPERTY = "cache.server.lifecycle.skip";
    public static final boolean CACHE_SERVER_LIFECYCLE_SKIP = Boolean.parseBoolean(System.getProperty(CACHE_SERVER_LIFECYCLE_SKIP_PROPERTY, "false"));


    public static final Boolean START_MIGRATION_CONTAINER = "auto".equals(System.getProperty("migration.mode")) ||
            "manual".equals(System.getProperty("migration.mode"));

    @Inject
    @SuiteScoped
    private InstanceProducer<SuiteContext> suiteContextProducer;
    private SuiteContext suiteContext;

    @Inject
    @ApplicationScoped // needed in AdapterTestExecutionDecider
    private InstanceProducer<TestContext> testContextProducer;

    @Inject
    @ClassScoped
    private InstanceProducer<OAuthClient> oAuthClientProducer;

    public static String getAuthServerContextRoot() {
        return getAuthServerContextRoot(0);
    }

    public static String getAuthServerContextRoot(int clusterPortOffset) {
        String host = System.getProperty("auth.server.host", "localhost");
        int httpPort = Integer.parseInt(System.getProperty("auth.server.http.port")); // property must be set
        int httpsPort = Integer.parseInt(System.getProperty("auth.server.https.port")); // property must be set

        String scheme = AUTH_SERVER_SSL_REQUIRED ? "https" : "http";
        int port = AUTH_SERVER_SSL_REQUIRED ? httpsPort : httpPort;

        return String.format("%s://%s:%s", scheme, host, port + clusterPortOffset);
    }

    public static String getAuthServerBrowserContextRoot() throws MalformedURLException {
        return getAuthServerBrowserContextRoot(new URL(getAuthServerContextRoot()));
    }

    public static String getAuthServerBrowserContextRoot(URL contextRoot) {
        String browserHost = System.getProperty("auth.server.browserHost");
        if (StringUtils.isEmpty(browserHost)) {
            browserHost = contextRoot.getHost();
        }
        return String.format("%s://%s:%s", contextRoot.getProtocol(), browserHost, contextRoot.getPort());
    }

    public static OnlineManagementClient getManagementClient() {
        try {
            return ManagementClient.online(OnlineOptions
                    .standalone()
                    .hostAndPort(System.getProperty("auth.server.host", "localhost"), Integer.parseInt(System.getProperty("auth.server.management.port", "10090")))
                    .build()
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void distinguishContainersInConsoleOutput(@Observes(precedence = 5) StartContainer event) {
        log.info("************************" + event.getContainer().getName()
                + "*****************************************************************************");
    }

    public void initializeSuiteContext(@Observes(precedence = 2) BeforeSuite event) {
        Set<ContainerInfo> containers = containerRegistry.get().getContainers().stream()
          .map(ContainerInfo::new)
          .collect(Collectors.toSet());

        suiteContext = new SuiteContext(containers);

        if (AUTH_SERVER_CROSS_DC) {
            // if cross-dc mode enabled, load-balancer is the frontend of datacenter cluster
            containers.stream()
                .filter(c -> c.getQualifier().startsWith(AUTH_SERVER_BALANCER + "-cross-dc"))
                .forEach(c -> {
                    String portOffsetString = c.getArquillianContainer().getContainerConfiguration().getContainerProperties().getOrDefault("bindHttpPortOffset", "0");
                    String dcString = c.getArquillianContainer().getContainerConfiguration().getContainerProperties().getOrDefault("dataCenter", "0");
                    updateWithAuthServerInfo(c, Integer.valueOf(portOffsetString));
                    suiteContext.addAuthServerInfo(Integer.valueOf(dcString), c);
                });

            if (suiteContext.getDcAuthServerInfo().isEmpty()) {
                throw new IllegalStateException("Not found frontend container (load balancer): " + AUTH_SERVER_BALANCER);
            }
            if (suiteContext.getDcAuthServerInfo().stream().anyMatch(Objects::isNull)) {
                throw new IllegalStateException("Frontend container (load balancer) misconfiguration");
            }

            containers.stream()
                    .filter(c -> c.getQualifier().startsWith("auth-server-" + System.getProperty("node.name") + "-"))
                    .sorted((a, b) -> a.getQualifier().compareTo(b.getQualifier()))
                    .forEach(c -> {
                        String portOffsetString = c.getArquillianContainer().getContainerConfiguration().getContainerProperties().getOrDefault("bindHttpPortOffset", "0");
                        updateWithAuthServerInfo(c, Integer.valueOf(portOffsetString));

                        String dcString = c.getArquillianContainer().getContainerConfiguration().getContainerProperties().getOrDefault("dataCenter", "0");
                        suiteContext.addAuthServerBackendsInfo(Integer.valueOf(dcString), c);
                    });

            containers.stream()
                    .filter(c -> c.getQualifier().startsWith("cache-server-cross-dc-"))
                    .sorted((a, b) -> a.getQualifier().compareTo(b.getQualifier()))
                    .forEach(containerInfo -> {
                        int prefixSize = "cache-server-cross-dc-".length();
                        int dcIndex = Integer.parseInt(containerInfo.getQualifier().substring(prefixSize)) -1;
                        suiteContext.addCacheServerInfo(dcIndex, containerInfo);
                    });

            if (suiteContext.getDcAuthServerInfo().isEmpty()) {
                throw new RuntimeException(String.format("No auth server container matching '%s' found in arquillian.xml.", AUTH_SERVER_BACKEND));
            }
            if (suiteContext.getDcAuthServerBackendsInfo().stream().anyMatch(Objects::isNull)) {
                throw new IllegalStateException("Frontend container (load balancer) misconfiguration");
            }
            if (suiteContext.getDcAuthServerBackendsInfo().stream().anyMatch(List::isEmpty)) {
                throw new RuntimeException(String.format("Some data center has no auth server container matching '%s' defined in arquillian.xml.", AUTH_SERVER_BACKEND));
            }
            if (suiteContext.getCacheServersInfo().isEmpty() && !CACHE_SERVER_LIFECYCLE_SKIP) {
                throw new IllegalStateException("Cache containers misconfiguration");
            }

            log.info("Using frontend containers: " + this.suiteContext.getDcAuthServerInfo().stream()
              .map(ContainerInfo::getQualifier)
              .collect(Collectors.joining(", ")));
        } else if (AUTH_SERVER_CLUSTER) {
            // if cluster mode enabled, load-balancer is the frontend
            ContainerInfo container = containers.stream()
              .filter(c -> c.getQualifier().startsWith(AUTH_SERVER_BALANCER))
              .findAny()
              .orElseThrow(() -> new IllegalStateException("Not found frontend container: " + AUTH_SERVER_BALANCER));
            updateWithAuthServerInfo(container);
            suiteContext.setAuthServerInfo(container);

            containers.stream()
                .filter(c -> c.getQualifier().startsWith(AUTH_SERVER_BACKEND))
                .sorted((a, b) -> a.getQualifier().compareTo(b.getQualifier())) // ordering is expected by the cluster tests
                .forEach(c -> {
                    int portOffset = Integer.parseInt(c.getQualifier().substring(AUTH_SERVER_BACKEND.length()));
                    updateWithAuthServerInfo(c, portOffset);
                    suiteContext.addAuthServerBackendsInfo(0, c);
                });

            if (suiteContext.getAuthServerBackendsInfo().isEmpty()) {
                throw new RuntimeException(String.format("No auth server container matching '%s' found in arquillian.xml.", AUTH_SERVER_BACKEND));
            }

            log.info("Using frontend container: " + container.getQualifier());
        } else {
            // frontend-only
            ContainerInfo container = containers.stream()
              .filter(c -> c.getQualifier().startsWith(AUTH_SERVER_CONTAINER))
              .findAny()
              .orElseThrow(() -> new IllegalStateException("Not found frontend container: " + AUTH_SERVER_CONTAINER));
            updateWithAuthServerInfo(container);
            suiteContext.setAuthServerInfo(container);
        }

        if (START_MIGRATION_CONTAINER) {
            // init migratedAuthServerInfo
            for (ContainerInfo container : suiteContext.getContainers()) {
                // migrated auth server
                if (container.getQualifier().equals("auth-server-jboss-migration")) {
                    updateWithAuthServerInfo(container);
                    suiteContext.setMigratedAuthServerInfo(container);
                }
            }
            // validate setup
            if (suiteContext.getMigratedAuthServerInfo() == null) {
                throw new RuntimeException(String.format("Migration test was enabled but no auth server from which to migrate was activated. "
                        + "A container matching auth-server-jboss-migration needs to be enabled in arquillian.xml."));
            }
        }

        suiteContextProducer.set(suiteContext);
        CrossDCTestEnricher.initializeSuiteContext(suiteContext);
        log.info("\n\n" + suiteContext);
        log.info("\n\n" + SystemInfoHelper.getSystemInfo());
    }

    private ContainerInfo updateWithAuthServerInfo(ContainerInfo authServerInfo) {
        return updateWithAuthServerInfo(authServerInfo, 0);
    }

    private ContainerInfo updateWithAuthServerInfo(ContainerInfo authServerInfo, int clusterPortOffset) {
        try {
            URL contextRoot = new URL(getAuthServerContextRoot(clusterPortOffset));

            authServerInfo.setContextRoot(contextRoot);
            authServerInfo.setBrowserContextRoot(new URL(getAuthServerBrowserContextRoot(contextRoot)));
        } catch (MalformedURLException ex) {
            throw new IllegalArgumentException(ex);
        }
        return authServerInfo;
    }

    public void startMigratedContainer(@Observes(precedence = 3) StartSuiteContainers event) {
        if (suiteContext.isAuthServerMigrationEnabled()) {
            log.info("\n\n### Starting keycloak " + System.getProperty("migrated.auth.server.version", "- previous") + " ###\n\n");
            startContainerEvent.fire(new StartContainer(suiteContext.getMigratedAuthServerInfo().getArquillianContainer()));
        }
    }

    public void runPreMigrationTask(@Observes(precedence = 2) StartSuiteContainers event) throws Exception {
        if (suiteContext.isAuthServerMigrationEnabled()) {
            log.info("\n\n### Run preMigration task on keycloak " + System.getProperty("migrated.auth.server.version", "- previous") + " ###\n\n");
            suiteContext.getMigrationContext().runPreMigrationTask();
        }
    }

    public void stopMigratedContainer(@Observes(precedence = 1) StartSuiteContainers event) {
        if (suiteContext.isAuthServerMigrationEnabled()) {
            log.info("## STOP old container: " + suiteContext.getMigratedAuthServerInfo().getQualifier());
            stopContainerEvent.fire(new StopContainer(suiteContext.getMigratedAuthServerInfo().getArquillianContainer()));
        }
    }

    public void startAuthContainer(@Observes(precedence = 0) StartSuiteContainers event) {
        //frontend-only (either load-balancer or auth-server)
        log.debug("Starting auth server before suite");
        startContainerEvent.fire(new StartContainer(suiteContext.getAuthServerInfo().getArquillianContainer()));
    }

    private static final Pattern RECOGNIZED_ERRORS = Pattern.compile("ERROR|SEVERE|Exception ");
    private static final Pattern IGNORED = Pattern.compile("Jetty ALPN support not found|org.keycloak.events");

    private static final boolean isRecognizedErrorLog(String logText) {
        //There is expected string "Exception" in server log: Adding provider
        //singleton org.keycloak.services.resources.ModelExceptionMapper
        return RECOGNIZED_ERRORS.matcher(logText).find() && ! IGNORED.matcher(logText).find();
    }

    private static final void failOnRecognizedErrorInLog(Stream<String> logStream) {
        Optional<String> anyRecognizedError = logStream.filter(AuthServerTestEnricher::isRecognizedErrorLog).findAny();
        if (anyRecognizedError.isPresent()) {
            throw new RuntimeException(String.format("Server log file contains ERROR: '%s'", anyRecognizedError.get()));
        }
    }

    public void checkServerLogs(@Observes(precedence = -1) BeforeSuite event) throws IOException, InterruptedException {
        if (! suiteContext.getAuthServerInfo().isJBossBased()) {
            suiteContext.setServerLogChecker(new TextFileChecker());    // checks nothing
            return;
        }

        boolean checkLog = Boolean.parseBoolean(System.getProperty("auth.server.log.check", "true"));
        String jbossHomePath = suiteContext.getAuthServerInfo().getProperties().get("jbossHome");
        if (checkLog) {
            LogChecker.getJBossServerLogsChecker(true, jbossHomePath).checkFiles(AuthServerTestEnricher::failOnRecognizedErrorInLog);
        }
        suiteContext.setServerLogChecker(LogChecker.getJBossServerLogsChecker(false, jbossHomePath));
    }

    public void initializeTestContext(@Observes(precedence = 2) BeforeClass event) {
        TestContext testContext = new TestContext(suiteContext, event.getTestClass().getJavaClass());
        testContextProducer.set(testContext);
    }

    public void initializeTLS(@Observes(precedence = 3) BeforeClass event) throws Exception {
        // TLS for Undertow is configured in KeycloakOnUndertow since it requires
        // SSLContext while initializing HTTPS handlers
        if (!suiteContext.isAuthServerCrossDc() && !suiteContext.isAuthServerCluster()) {
            initializeTLS(suiteContext.getAuthServerInfo());
        }
    }

    public static void initializeTLS(ContainerInfo containerInfo) {
        if (AUTH_SERVER_SSL_REQUIRED && containerInfo.isJBossBased()) {
            log.infof("\n\n### Setting up TLS for %s ##\n\n", containerInfo);
            try {
                OnlineManagementClient client = getManagementClient(containerInfo);
                AuthServerTestEnricher.enableTLS(client);
                client.close();
            } catch (Exception e) {
                log.warn("Failed to set up TLS for container '" + containerInfo.getQualifier() + "'. This may lead to unexpected behavior unless the test" +
                        " sets it up manually", e);
            }

        }
    }

    private static OnlineManagementClient getManagementClient(ContainerInfo containerInfo) {
        try {
            return ManagementClient.online(OnlineOptions
                    .standalone()
                    .hostAndPort("localhost", Integer.valueOf(containerInfo.getProperties().get("managementPort")))
                    .build()
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void enableTLS(OnlineManagementClient client) throws Exception {
        Administration administration = new Administration(client);
        Operations operations = new Operations(client);

        if(!operations.exists(Address.coreService("management").and("security-realm", "UndertowRealm"))) {
            client.execute("/core-service=management/security-realm=UndertowRealm:add()");
            client.execute("/core-service=management/security-realm=UndertowRealm/server-identity=ssl:add(keystore-relative-to=jboss.server.config.dir,keystore-password=secret,keystore-path=keycloak.jks");
            client.execute("/core-service=management/security-realm=UndertowRealm/authentication=truststore:add(keystore-relative-to=jboss.server.config.dir,keystore-password=secret,keystore-path=keycloak.truststore");

            client.apply(new RemoveUndertowListener.Builder(UndertowListenerType.HTTPS_LISTENER, "https")
                  .forDefaultServer());

            administration.reloadIfRequired();

            client.apply(new AddUndertowListener.HttpsBuilder("https", "default-server", "https")
                  .securityRealm("UndertowRealm")
                  .verifyClient(SslVerifyClient.REQUESTED)
                  .build());

            administration.reloadIfRequired();
        } else {
            log.info("## The Auth Server has already configured TLS. Skipping... ##");
        }
    }

    protected boolean isAuthServerJBossBased() {
        return containerRegistry.get().getContainers().stream()
              .map(ContainerInfo::new)
              .filter(ci -> ci.isJBossBased())
              .findFirst().isPresent();
    }

    public void initializeOAuthClient(@Observes(precedence = 4) BeforeClass event) {
        // TODO workaround. Check if can be removed
        OAuthClient.updateURLs(suiteContext.getAuthServerInfo().getContextRoot().toString());
        OAuthClient oAuthClient = new OAuthClient();
        oAuthClientProducer.set(oAuthClient);
    }

    public void beforeTest(@Observes(precedence = 100) Before event) throws IOException {
        suiteContext.getServerLogChecker().updateLastCheckedPositionsOfAllFilesToEndOfFile();
    }

    private static final Pattern UNEXPECTED_UNCAUGHT_ERROR = Pattern.compile(
      KeycloakErrorHandler.class.getSimpleName()
        + ".*"
        + Pattern.quote(KeycloakErrorHandler.UNCAUGHT_SERVER_ERROR_TEXT)
        + "[\\s:]*(.*)$"
    );

    private void checkForNoUnexpectedUncaughtError(Stream<String> logStream) {
        Optional<Matcher> anyUncaughtError = logStream.map(UNEXPECTED_UNCAUGHT_ERROR::matcher).filter(Matcher::find).findAny();
        if (anyUncaughtError.isPresent()) {
            Matcher m = anyUncaughtError.get();
            Assert.fail("Uncaught server error detected: " + m.group(1));
        }
    }

    public void afterTest(@Observes(precedence = -1) After event) throws IOException {
        if (event.getTestMethod().getAnnotation(UncaughtServerErrorExpected.class) == null) {
            suiteContext.getServerLogChecker().checkFiles(this::checkForNoUnexpectedUncaughtError);
        }
    }

    public void afterClass(@Observes(precedence = 1) AfterClass event) {
        //check if a test accidentally left the auth-server not running
        ContainerController controller = containerConroller.get();
        if (!controller.isStarted(suiteContext.getAuthServerInfo().getQualifier())) {
            log.warn("Auth server wasn't running. Starting " + suiteContext.getAuthServerInfo().getQualifier());
            controller.start(suiteContext.getAuthServerInfo().getQualifier());
        }

        TestContext testContext = testContextProducer.get();

        Keycloak adminClient = testContext.getAdminClient();
        KeycloakTestingClient testingClient = testContext.getTestingClient();

        removeTestRealms(testContext, adminClient);

        if (adminClient != null) {
            adminClient.close();
        }

        if (testingClient != null) {
            testingClient.close();
        }
    }


    public static void removeTestRealms(TestContext testContext, Keycloak adminClient) {
        List<RealmRepresentation> testRealmReps = testContext.getTestRealmReps();
        if (testRealmReps != null && !testRealmReps.isEmpty()) {
            log.info("removing test realms after test class");
            StringBuilder realms = new StringBuilder();
            for (RealmRepresentation testRealm : testRealmReps) {
                try {
                    adminClient.realms().realm(testRealm.getRealm()).remove();
                    realms.append(testRealm.getRealm()).append(", ");
                } catch (NotFoundException e) {
                    // Ignore
                }
            }
            log.info("removed realms: " + realms);
        }
    }

}
