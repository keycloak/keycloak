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

import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.commons.lang.StringUtils;
import org.jboss.arquillian.container.spi.ContainerRegistry;
import org.jboss.arquillian.container.spi.client.container.LifecycleException;
import org.jboss.arquillian.container.spi.client.container.DeploymentException;
import org.jboss.arquillian.container.spi.event.StartContainer;
import org.jboss.arquillian.container.spi.event.StartSuiteContainers;
import org.jboss.arquillian.container.spi.event.StopContainer;
import org.jboss.arquillian.container.spi.event.container.AfterStart;
import org.jboss.arquillian.container.spi.event.container.BeforeStop;
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
import org.jboss.arquillian.test.spi.event.suite.AfterSuite;
import org.jboss.arquillian.test.spi.event.suite.BeforeClass;
import org.jboss.arquillian.test.spi.event.suite.BeforeSuite;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.common.util.StringPropertyReplacer;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.services.error.KeycloakErrorHandler;
import org.keycloak.testsuite.arquillian.annotation.SetDefaultProvider;
import org.keycloak.testsuite.arquillian.annotation.UncaughtServerErrorExpected;
import org.keycloak.testsuite.arquillian.annotation.EnableVault;
import org.keycloak.testsuite.client.KeycloakTestingClient;
import org.keycloak.testsuite.util.LogChecker;
import org.keycloak.testsuite.util.OAuthClient;
import org.keycloak.testsuite.util.SpiProvidersSwitchingUtils;
import org.keycloak.testsuite.util.SqlUtils;
import org.keycloak.testsuite.util.SystemInfoHelper;
import org.keycloak.testsuite.util.VaultUtils;
import org.keycloak.testsuite.util.ServerURLs;
import org.keycloak.testsuite.util.TextFileChecker;
import org.wildfly.extras.creaper.core.ManagementClient;
import org.wildfly.extras.creaper.core.online.OnlineManagementClient;
import org.wildfly.extras.creaper.core.online.OnlineOptions;
import org.wildfly.extras.creaper.core.online.operations.Address;
import org.wildfly.extras.creaper.core.online.operations.Operations;
import org.wildfly.extras.creaper.core.online.operations.admin.Administration;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.Provider;
import java.security.Security;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.ws.rs.NotFoundException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.jboss.arquillian.test.spi.event.suite.After;
import org.jboss.arquillian.test.spi.event.suite.Before;
import org.jboss.shrinkwrap.api.importer.ZipImporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.junit.Assert;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import static org.keycloak.testsuite.arquillian.ServerTestEnricherUtil.addHttpsListener;
import static org.keycloak.testsuite.arquillian.ServerTestEnricherUtil.reloadOrRestartTimeoutClient;
import static org.keycloak.testsuite.arquillian.ServerTestEnricherUtil.removeHttpsListener;
import static org.keycloak.testsuite.util.ServerURLs.getAuthServerContextRoot;
import static org.keycloak.testsuite.util.ServerURLs.removeDefaultPorts;

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

    private JavaArchive testsuiteProvidersArchive;
    private String currentContainerName;

    public static final String AUTH_SERVER_CONTAINER_DEFAULT = "auth-server-undertow";
    public static final String AUTH_SERVER_CONTAINER_PROPERTY = "auth.server.container";
    public static final String AUTH_SERVER_CONTAINER = System.getProperty(AUTH_SERVER_CONTAINER_PROPERTY, AUTH_SERVER_CONTAINER_DEFAULT);

    public static final String AUTH_SERVER_BACKEND_DEFAULT = AUTH_SERVER_CONTAINER + "-backend";
    public static final String AUTH_SERVER_BACKEND_PROPERTY = "auth.server.backend";
    public static final String AUTH_SERVER_BACKEND = System.getProperty(AUTH_SERVER_BACKEND_PROPERTY, AUTH_SERVER_BACKEND_DEFAULT);

    public static final String AUTH_SERVER_LEGACY = "auth-server-legacy";

    public static final String AUTH_SERVER_BALANCER_DEFAULT = "auth-server-balancer";
    public static final String AUTH_SERVER_BALANCER_PROPERTY = "auth.server.balancer";
    public static final String AUTH_SERVER_BALANCER = System.getProperty(AUTH_SERVER_BALANCER_PROPERTY, AUTH_SERVER_BALANCER_DEFAULT);

    public static final String AUTH_SERVER_CLUSTER_PROPERTY = "auth.server.cluster";
    public static final boolean AUTH_SERVER_CLUSTER = Boolean.parseBoolean(System.getProperty(AUTH_SERVER_CLUSTER_PROPERTY, "false"));
    public static final String AUTH_SERVER_CROSS_DC_PROPERTY = "auth.server.crossdc";
    public static final boolean AUTH_SERVER_CROSS_DC = Boolean.parseBoolean(System.getProperty(AUTH_SERVER_CROSS_DC_PROPERTY, "false"));

    public static final String AUTH_SERVER_HOME_PROPERTY = "auth.server.home";

    public static final String CACHE_SERVER_LIFECYCLE_SKIP_PROPERTY = "cache.server.lifecycle.skip";
    public static final boolean CACHE_SERVER_LIFECYCLE_SKIP = Boolean.parseBoolean(System.getProperty(CACHE_SERVER_LIFECYCLE_SKIP_PROPERTY, "false"));


    private static final String MIGRATION_MODE_PROPERTY = "migration.mode";
    private static final String MIGRATION_MODE_AUTO = "auto";
    private static final String MIGRATION_MODE_MANUAL = "manual";
    public static final Boolean START_MIGRATION_CONTAINER = MIGRATION_MODE_AUTO.equals(System.getProperty(MIGRATION_MODE_PROPERTY)) ||
            MIGRATION_MODE_MANUAL.equals(System.getProperty(MIGRATION_MODE_PROPERTY));

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

    public static boolean isAuthServerRemote() {
        return AUTH_SERVER_CONTAINER.equals("auth-server-remote");
    }

    public static boolean isAuthServerQuarkus() {
        return AUTH_SERVER_CONTAINER.equals("auth-server-quarkus");
    }

    public static String getHttpAuthServerContextRoot() {
        String host = System.getProperty("auth.server.host", "localhost");
        int httpPort = Integer.parseInt(System.getProperty("auth.server.http.port")); // property must be set

        return removeDefaultPorts(String.format("%s://%s:%s", "http", host, httpPort));
    }

    public static String getHttpsAuthServerContextRoot() {
        String host = System.getProperty("auth.server.host", "localhost");
        int httpPort = Integer.parseInt(System.getProperty("auth.server.https.port")); // property must be set

        return removeDefaultPorts(String.format("%s://%s:%s", "https", host, httpPort));
    }

    public static String getAuthServerBrowserContextRoot() throws MalformedURLException {
        return getAuthServerBrowserContextRoot(new URL(getAuthServerContextRoot()));
    }

    public static String getAuthServerBrowserContextRoot(URL contextRoot) {
        String browserHost = System.getProperty("auth.server.browserHost");
        if (StringUtils.isEmpty(browserHost)) {
            browserHost = contextRoot.getHost();
        }
        return String.format("%s://%s%s", contextRoot.getProtocol(), browserHost,
                contextRoot.getPort() == -1 || contextRoot.getPort() == contextRoot.getDefaultPort()
                        ? ""
                        : ":" + contextRoot.getPort());
    }

    public static OnlineManagementClient getManagementClient() {
        try {
            return ManagementClient.online(OnlineOptions
                    .standalone()
                    .hostAndPort(System.getProperty("auth.server.management.host", "localhost"), Integer.parseInt(System.getProperty("auth.server.management.port", "10090")))
                    .auth("admin", "admin")
                    .build()
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void distinguishContainersInConsoleOutput(@Observes(precedence = 5) StartContainer event) {
        log.info("************************" + event.getContainer().getName()
                + "*****************************************************************************");
        currentContainerName = event.getContainer().getName();
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
                    .filter(c -> c.getQualifier().startsWith("cache-server-"))
                    .sorted((a, b) -> a.getQualifier().compareTo(b.getQualifier()))
                    .forEach(containerInfo -> {
                        
                        log.info(String.format("cache container: %s", containerInfo.getQualifier()));
                        
                        int prefixSize = containerInfo.getQualifier().lastIndexOf("-") + 1;
                        int dcIndex = Integer.parseInt(containerInfo.getQualifier().substring(prefixSize)) - 1;
                        
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

            if (Boolean.parseBoolean(System.getProperty("auth.server.jboss.legacy"))) {
                ContainerInfo legacy = containers.stream()
                    .filter(c -> c.getQualifier().startsWith(AUTH_SERVER_LEGACY))
                    .findAny()
                    .orElseThrow(() -> new IllegalStateException("Not found legacy container: " + AUTH_SERVER_LEGACY));
                updateWithAuthServerInfo(legacy, 500);
                suiteContext.setLegacyAuthServerInfo(legacy);
            }

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

        // Remove all map storages present in target directory
        // This is useful for example in intellij where target directory is not removed between test runs
        File dir = new File(System.getProperty("project.build.directory", "target"));
        FileFilter fileFilter = new WildcardFileFilter("map-*.json");
        File[] files = dir.listFiles(fileFilter);
        if (files != null) {
            for (File f : files) {
                f.delete();
            }
        }
    }

    public static void executeCli(String... commands) throws Exception {
        OnlineManagementClient client = AuthServerTestEnricher.getManagementClient();
        Administration administration = new Administration(client);

        for (String c : commands) {
            client.execute(c).assertSuccess();
        }

        administration.reload();

        client.close();
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
            initializeTLS(suiteContext.getMigratedAuthServerInfo());
        }
    }

    public void deployProviders(@Observes(precedence = -1) AfterStart event) throws DeploymentException {
        if (isAuthServerRemote() && currentContainerName.contains("auth-server")) {
            this.testsuiteProvidersArchive = ShrinkWrap.create(ZipImporter.class, "testsuiteProviders.jar")
                    .importFrom(Maven.configureResolverViaPlugin()
                        .resolve("org.keycloak.testsuite:integration-arquillian-testsuite-providers")
                        .withoutTransitivity()
                        .asSingleFile()
                    ).as(JavaArchive.class)
                    .addAsManifestResource("jboss-deployment-structure.xml");
                    
            event.getDeployableContainer().deploy(testsuiteProvidersArchive);
        }
    }

    public void unDeployProviders(@Observes(precedence = 20) BeforeStop event) throws DeploymentException {
        if (testsuiteProvidersArchive != null) {
            event.getDeployableContainer().undeploy(testsuiteProvidersArchive);
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

        try {
            startContainerEvent.fire(new StartContainer(suiteContext.getAuthServerInfo().getArquillianContainer()));
        } catch (Exception e) {
            // It is expected that server startup fails with migration-mode-manual
            if (e instanceof LifecycleException && handleManualMigration()) {
                log.info("Set log file checker to end of file.");
                try {
                    // this will mitigate possible issues in manual server update tests
                    // when the auth server started with not updated DB
                    // e.g. Caused by: org.keycloak.ServerStartupError: Database not up-to-date, please migrate database with
                    if (suiteContext.getServerLogChecker() == null) {
                        setServerLogChecker();
                    }
                    suiteContext.getServerLogChecker()
                        .updateLastCheckedPositionsOfAllFilesToEndOfFile();
                } catch (IOException ioe) {
                    log.warn("Server log checker failed to update position:", ioe);
                }
                log.info("Starting server again after manual DB migration was finished");
                startContainerEvent.fire(new StartContainer(suiteContext.getAuthServerInfo().getArquillianContainer()));
                return;
            }

            // Just re-throw the exception
            throw e;
        }
    }


    /**
     * Returns true if we are in manual DB migration test and if the previously created SQL script was successfully executed.
     * Returns false if we are not in manual DB migration test or SQL script couldn't be executed for any reason.
     * @return see method description
     */
    private boolean handleManualMigration() {
        // It is expected that server startup fails with migration-mode-manual
        if (!MIGRATION_MODE_MANUAL.equals(System.getProperty(MIGRATION_MODE_PROPERTY))) {
            return false;
        }

        String authServerHome = System.getProperty(AUTH_SERVER_HOME_PROPERTY);
        if (authServerHome == null) {
            log.warnf("Property '%s' was missing during manual mode migration test", AUTH_SERVER_HOME_PROPERTY);
            return false;
        }

        String sqlScriptPath = authServerHome + File.separator + "keycloak-database-update.sql";
        if (!new File(sqlScriptPath).exists()) {
            log.warnf("File '%s' didn't exists during manual mode migration test", sqlScriptPath);
            return false;
        }

        // Run manual migration with the ant task
        log.infof("Running SQL script created by liquibase during manual migration flow", sqlScriptPath);
        String prefix = "keycloak.connectionsJpa.";
        String jdbcDriver = System.getProperty(prefix + "driver");
        String dbUrl = StringPropertyReplacer.replaceProperties(System.getProperty(prefix + "url"));
        String dbUser = System.getProperty(prefix + "user");
        String dbPassword = System.getProperty(prefix + "password");

        SqlUtils.runSqlScript(sqlScriptPath, jdbcDriver, dbUrl, dbUser, dbPassword);

        return true;
    }


    private static final Pattern RECOGNIZED_ERRORS = Pattern.compile("ERROR \\[|SEVERE \\[|Exception ");
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

    private void setServerLogChecker() throws IOException {
        String jbossHomePath = suiteContext.getAuthServerInfo().getProperties().get("jbossHome");
        suiteContext.setServerLogChecker(LogChecker.getJBossServerLogsChecker(jbossHomePath));
    }

    public void checkServerLogs(@Observes(precedence = -1) BeforeSuite event) throws IOException, InterruptedException {
        if (! suiteContext.getAuthServerInfo().isJBossBased()) {
            suiteContext.setServerLogChecker(new TextFileChecker());    // checks nothing
            return;
        }
        if (suiteContext.getServerLogChecker() == null) {
            setServerLogChecker();
        }
        boolean checkLog = Boolean.parseBoolean(System.getProperty("auth.server.log.check", "true"));
        if (checkLog) {
            suiteContext.getServerLogChecker()
                .checkFiles(true, AuthServerTestEnricher::failOnRecognizedErrorInLog);
        }
    }

    public void restartAuthServer() throws Exception {
        if (isAuthServerRemote()) {
            try (OnlineManagementClient client = getManagementClient()) {
                int timeoutInSec = Integer.getInteger(System.getProperty("auth.server.jboss.startup.timeout"), 300);
                Administration administration = new Administration(client, timeoutInSec);
                administration.reload();
            }
        } else {
            stopContainerEvent.fire(new StopContainer(suiteContext.getAuthServerInfo().getArquillianContainer()));
            startContainerEvent.fire(new StartContainer(suiteContext.getAuthServerInfo().getArquillianContainer()));
        }
    }

    public void initializeTestContext(@Observes(precedence = 2) BeforeClass event) throws Exception {
        TestContext testContext = new TestContext(suiteContext, event.getTestClass().getJavaClass());
        testContextProducer.set(testContext);

        if (!isAuthServerRemote() && !isAuthServerQuarkus()) {
            boolean wasUpdated = false;

            if (event.getTestClass().isAnnotationPresent(EnableVault.class)) {
                VaultUtils.enableVault(suiteContext, event.getTestClass().getAnnotation(EnableVault.class).providerId());
                wasUpdated = true;
            }
            if (event.getTestClass().isAnnotationPresent(SetDefaultProvider.class)) {
                SetDefaultProvider defaultProvider = event.getTestClass().getAnnotation(SetDefaultProvider.class);

                if (defaultProvider.beforeEnableFeature()) {
                    SpiProvidersSwitchingUtils.addProviderDefaultValue(suiteContext, defaultProvider);
                    wasUpdated = true;
                }
            }

            if (wasUpdated) {
                restartAuthServer();
                testContext.reconnectAdminClient();
            }
        }
    }

    public void initializeTLS(@Observes(precedence = 3) BeforeClass event) throws Exception {
        // TLS for Undertow is configured in KeycloakOnUndertow since it requires
        // SSLContext while initializing HTTPS handlers
        if (!suiteContext.isAuthServerCrossDc() && !suiteContext.isAuthServerCluster()) {
            initializeTLS(suiteContext.getAuthServerInfo());
        }
    }

    public static void initializeTLS(ContainerInfo containerInfo) {
        if (ServerURLs.AUTH_SERVER_SSL_REQUIRED && containerInfo.isJBossBased()) {
            log.infof("\n\n### Setting up TLS for %s ##\n\n", containerInfo);
            try (OnlineManagementClient client = getManagementClient(containerInfo)) {
                AuthServerTestEnricher.enableTLS(client);
            } catch (Exception e) {
                log.warn("Failed to set up TLS for container '" + containerInfo.getQualifier() + "'. This may lead to unexpected behavior unless the test" +
                        " sets it up manually", e);
            }

        }
    }

    /** KEYCLOAK-15692 Work-around the OpenJSSE TlsMasterSecretGenerator error:
     *
     *      https://github.com/openjsse/openjsse/issues/11
     *
     *  To prevent above TLS handshake error when initiating a TLS connection
     *  ensure:
     *  * Either both server and client endpoints of the future TLS connection
     *    simultaneously utilize a JSSE security provider using the OpenJSSE
     *    extension,
     *
     *  * Or both server and client endpoints simultaneously use a JSSE
     *    security provider, which doesn't depend on the OpenJSSE extension.
     *
     *  Do this by performing the following:
     *  * On platforms where implementation of the SunJSSE provider depends on
     *  OpenJSSE extension ensure only SunJSSE provider is used to define the
     *  SSL context of the Elytron client used for outbound SSL connections.
     *
     *  * On other platforms, use any suitable JSSE provider by querying all
     *  the platform providers for respective property.
     *
     */
    public static void setJsseSecurityProviderForOutboundSslConnectionsOfElytronClient(@Observes(precedence = 100) StartSuiteContainers event) {
        log.info(
            "Determining the JSSE security provider to use for outbound " +
            "SSL/TLS connections of the Elytron client"
        );

        // Use path to wildfly-config.xml directly if specified
        String wildflyConfigXmlPath =
            System.getProperty("wildfly-client.config.path");

        // Otherwise scan the classpath to determine its location
        if (wildflyConfigXmlPath == null) {
            log.debug("Scanning classpath to locate wildfly-config.xml");
            final String javaClassPath = System.getProperty("java.class.path");
            for (String dir : javaClassPath.split(File.pathSeparator)) {
                if (!dir.isEmpty()) {
                    String candidatePath = dir + File.separator +
                        "wildfly-config.xml";
                    if (new File(candidatePath).exists()) {
                        wildflyConfigXmlPath = candidatePath;
                        log.debugf(
                            "Found wildfly-config.xml at '%s' location",
                            wildflyConfigXmlPath
                        );
                        break;
                    }
                }
            }
        }

        final File wildflyConfigXml = ( wildflyConfigXmlPath != null ) ?
            new File(wildflyConfigXmlPath)                             :
            null;

        // Throw an error if wildfly-config.xml path specified directly via the
        // 'wildfly-client.config.path' property doesn't represent a regular file
        // on the file system, or if it wasn't found by scanning the classpath
        if ( wildflyConfigXml == null || ! wildflyConfigXml.exists() ) {
            throw new RuntimeException(
                "Failed to locate the wildfly-config.xml to use for " +
                "the configuration of Elytron client"
            );
        } else {
            log.debugf(
                "Using wildfly-config.xml from '%s' location",
                wildflyConfigXmlPath
            );
        }

        /** Determine the name of the system property from wildfly-config.xml
         *  holding the name of the security provider which is used by Elytron
         *  client to define its SSL context for outbound SSL connections.
         */
        String jsseSecurityProviderSystemProperty = null;
        try {
            DocumentBuilder documentBuilder = DocumentBuilderFactory
                .newInstance().newDocumentBuilder();

            Document xmlDoc = documentBuilder.parse(wildflyConfigXml);
            NodeList nodeList = xmlDoc.getElementsByTagName("provider-name");
            // Sanity check
            if (nodeList.getLength() != 1) {
                throw new RuntimeException(
                    "Failed to locate the 'provider-name' element " +
                    "in wildfly-config.xml XML file"
                );
            }
            String providerNameElement = nodeList.item(0).getAttributes()
                .getNamedItem("name").getNodeValue();

            // Drop Wildfly's expressions notation from the attribute's value
            jsseSecurityProviderSystemProperty = providerNameElement
                .replaceAll("(\\$|\\{|\\}|(:.*$))", new String());

        } catch (IOException e) {
            throw new RuntimeException(String.format(
                "Error reading the '%s' file. Please make sure the provided " +
                "path is correct and retry",
                wildflyConfigXml.getAbsolutePath()
            ));
        } catch (ParserConfigurationException|SAXException e) {
            throw new RuntimeException(String.format(
                "Failed to parse the '%s' XML file",
                wildflyConfigXml.getAbsolutePath()
            ));
        }

        boolean determineJsseSecurityProviderName = false;
        if (jsseSecurityProviderSystemProperty != null) {
            // Does JSSE security provider system property already exist?
            if (
                System.getProperty(jsseSecurityProviderSystemProperty) == null
            ) {
                // If not, determine it
                determineJsseSecurityProviderName = true;
            }
        } else {
            throw new RuntimeException(
                "Failed to determine the name of system property " +
                "holding JSSE security provider's name for Elytron client"
            );
        }

        if (determineJsseSecurityProviderName) {

            /** Detect if OpenJSSE extension is present on the platform
             *
             *  Since internal 'com.sun.net.ssl.*' classes of the SunJSSE
             *  provider have identical names regardless if the OpenJSSE
             *  extension is used or not:
             *
             *    https://github.com/openjsse/openjsse/blob/master/pom.xml#L125
             *
             *  detect the presence of the OpenJSSE extension by checking the
             *  presence of the 'openjsse.jar' file within the JRE extensions
             *  directory.
             *
             */
            final String jreExtensionsDir = System.getProperty("java.home") +
                File.separator + "lib" + File.separator + "ext" +
                File.separator + "openjsse.jar";

            boolean openJsseExtensionPresent = new File(
                jreExtensionsDir).exists();

            Provider platformJsseProvider = Security
                .getProviders("SSLContext.TLSv1.2")[0];

            if (platformJsseProvider != null) {
                // If OpenJSSE extension is present
                if (openJsseExtensionPresent) {
                    // Sanity check - confirm SunJSSE provider is present on
                    // the platform (if OpenJSSE extension is present, it
                    // shouldn't ever happen SunJSSE won't be, but double-check
                    // for any case)
                    Provider sunJsseProvider = Stream.of(
                            Security.getProviders()
                        ).filter(p -> p.getName().equals("SunJSSE"))
                        .collect(Collectors.toList())
                        .get(0);

                    // Use it or throw an error if absent
                    if (sunJsseProvider != null) {
                        platformJsseProvider = sunJsseProvider;
                    } else {
                        throw new RuntimeException(
                            "The SunJSSE provider is not present " +
                            "on the platform"
                        );
                    }
                }
                // Propagate the final provider name to system property used by
                // wildfly-config.xml to configure the JSSE provider name
                System.setProperty(
                    jsseSecurityProviderSystemProperty,
                    platformJsseProvider.getName()
                );
            } else {
                throw new RuntimeException(
                    "Cannot identify a security provider for Elytron client " +
                    "offering the TLSv1.2 capability"
                );
            }
            log.infof(
                "Using the '%s' JSSE provider", platformJsseProvider.getName()
            );
        }
    }

    private static OnlineManagementClient getManagementClient(ContainerInfo containerInfo) {
        try {
            return ManagementClient.online(OnlineOptions
                    .standalone()
                    .hostAndPort("localhost", Integer.parseInt(containerInfo.getProperties().get("managementPort")))
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

            removeHttpsListener(client, administration);
            addHttpsListener(client);
            reloadOrRestartTimeoutClient(administration);
        } else {
            log.info("## The Auth Server has already configured TLS. Skipping ##");
        }
    }

    protected boolean isAuthServerJBossBased() {
        return containerRegistry.get().getContainers().stream()
              .map(ContainerInfo::new)
              .anyMatch(ContainerInfo::isJBossBased);
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

    public void startTestClassProvider(@Observes(precedence = 1) BeforeSuite beforeSuite) {
        TestClassProvider testClassProvider = new TestClassProvider();
        testClassProvider.start();
        suiteContext.setTestClassProvider(testClassProvider);
    }

    public void stopTestClassProvider(@Observes(precedence = -1) AfterSuite afterSuite) {
        suiteContext.getTestClassProvider().stop();
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
            suiteContext.getServerLogChecker().checkFiles(false, this::checkForNoUnexpectedUncaughtError);
        }
    }

    public void afterClass(@Observes(precedence = 1) AfterClass event) throws Exception {
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

        if (!isAuthServerRemote() && !isAuthServerQuarkus()) {
            
            boolean wasUpdated = false;
            if (event.getTestClass().isAnnotationPresent(EnableVault.class)) {
                VaultUtils.disableVault(suiteContext, event.getTestClass().getAnnotation(EnableVault.class).providerId());
                wasUpdated = true;
            }

            if (event.getTestClass().isAnnotationPresent(SetDefaultProvider.class)) {
                SpiProvidersSwitchingUtils.removeProvider(suiteContext, event.getTestClass().getAnnotation(SetDefaultProvider.class));
                wasUpdated = true;
            }

            if (wasUpdated) {
                restartAuthServer();
                testContext.reconnectAdminClient();
            }
        }

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
