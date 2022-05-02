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

import java.io.File;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.test.spi.event.suite.AfterClass;
import org.jboss.arquillian.test.spi.event.suite.BeforeClass;
import org.jboss.logging.Logger;
import org.keycloak.testsuite.arquillian.annotation.AppServerContainer;
import org.keycloak.testsuite.arquillian.annotation.AppServerContainers;
import org.keycloak.testsuite.arquillian.containers.SelfManagedAppContainerLifecycle;
import org.keycloak.testsuite.utils.arquillian.ContainerConstants;
import org.wildfly.extras.creaper.commands.web.AddConnector;
import org.wildfly.extras.creaper.commands.web.AddConnectorSslConfig;
import org.wildfly.extras.creaper.core.CommandFailedException;
import org.wildfly.extras.creaper.core.ManagementClient;
import org.wildfly.extras.creaper.core.online.CliException;
import org.wildfly.extras.creaper.core.online.ManagementProtocol;
import org.wildfly.extras.creaper.core.online.OnlineManagementClient;
import org.wildfly.extras.creaper.core.online.OnlineOptions;
import org.wildfly.extras.creaper.core.online.operations.Address;
import org.wildfly.extras.creaper.core.online.operations.OperationException;
import org.wildfly.extras.creaper.core.online.operations.Operations;
import org.wildfly.extras.creaper.core.online.operations.admin.Administration;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import static org.keycloak.testsuite.arquillian.ServerTestEnricherUtil.addHttpsListenerAppServer;
import static org.keycloak.testsuite.arquillian.ServerTestEnricherUtil.reloadOrRestartTimeoutClient;
import static org.keycloak.testsuite.arquillian.ServerTestEnricherUtil.removeHttpsListener;
import static org.keycloak.testsuite.util.ServerURLs.getAppServerContextRoot;
import static org.keycloak.testsuite.util.ServerURLs.getAuthServerContextRoot;

/**
 *
 * @author tkyjovsk
 */
public class AppServerTestEnricher {

    private static final Logger log = Logger.getLogger(AppServerTestEnricher.class);

    public static final String CURRENT_APP_SERVER = System.getProperty("app.server", "undertow");
    public static final boolean APP_SERVER_SSL_REQUIRED = Boolean.parseBoolean(System.getProperty("app.server.ssl.required", "false"));

    @Inject private Instance<ContainerController> containerConrollerInstance;
    @Inject private Instance<TestContext> testContextInstance;
    private TestContext testContext;

    public static Set<String> getAppServerQualifiers(Class testClass) {
        Set<String> appServerQualifiers = new HashSet<>();

        Class<?> annotatedClass = getNearestSuperclassWithAppServerAnnotation(testClass);

        if (annotatedClass != null) {

            AppServerContainer[] appServerContainers = annotatedClass.getAnnotationsByType(AppServerContainer.class);

            for (AppServerContainer appServerContainer : appServerContainers) {
                appServerQualifiers.add(appServerContainer.value());
            }

        }

        for (Method method : testClass.getDeclaredMethods()) {
            if (method.isAnnotationPresent(AppServerContainers.class)) {
                for (AppServerContainer appServerContainer : method.getAnnotation(AppServerContainers.class).value()) {
                    appServerQualifiers.add(appServerContainer.value());
                }
            }
            if (method.isAnnotationPresent(AppServerContainer.class)) {
                appServerQualifiers.add(method.getAnnotation(AppServerContainer.class).value());
            }
        }

        return appServerQualifiers;
    }

    public static String getAppServerBrowserContextRoot() throws MalformedURLException {
        return getAppServerBrowserContextRoot(new URL(getAuthServerContextRoot()));
    }

    public static String getAppServerBrowserContextRoot(URL contextRoot) {
        String browserHost = System.getProperty("app.server.browserHost");
        if (StringUtils.isEmpty(browserHost)) {
            browserHost = contextRoot.getHost();
        }
        return String.format("%s://%s:%s", contextRoot.getProtocol(), browserHost, contextRoot.getPort());
    }

    public void updateTestContextWithAppServerInfo(@Observes(precedence = 1) BeforeClass event) {
        testContext = testContextInstance.get();

        Set<String> appServerQualifiers = getAppServerQualifiers(testContext.getTestClass());
        if (appServerQualifiers.isEmpty()) { // no adapter test
            log.info("\n\n" + testContext);
            return;
        }

        String appServerQualifier = null;
        for (String qualifier : appServerQualifiers) {
            if (qualifier.contains(";")) {// cluster adapter test
                final List<String> appServers = Arrays.asList(qualifier.split("\\s*;\\s*"));
                List<ContainerInfo> appServerBackendsInfo = testContext.getSuiteContext().getContainers().stream()
                    .filter(ci -> appServers.contains(ci.getQualifier()))
                    .map(this::updateWithAppServerInfo)
                    .collect(Collectors.toList());
                testContext.setAppServerBackendsInfo(appServerBackendsInfo);
            } else {// non-cluster adapter test
                for (ContainerInfo container : testContext.getSuiteContext().getContainers()) {
                    if (container.getQualifier().equals(qualifier)) {
                        testContext.setAppServerInfo(updateWithAppServerInfo(container));
                        appServerQualifier = qualifier;
                        break;
                    }
                    //TODO add warning if there are two or more matching containers.
                }
            }
        }
        // validate app server
        if (appServerQualifier != null && testContext.getAppServerInfo() == null) {
            throw new RuntimeException(String.format("No app server container matching '%s' was activated. Check if defined and enabled in arquillian.xml.", appServerQualifier));
        }
        log.info("\n\n" + testContext);
    }

    private ContainerInfo updateWithAppServerInfo(ContainerInfo appServerInfo) {
        return updateWithAppServerInfo(appServerInfo, 0);
    }

    private ContainerInfo updateWithAppServerInfo(ContainerInfo appServerInfo, int clusterPortOffset) {
        try {

            URL appServerContextRoot = new URL(isRelative()
                    ? getAuthServerContextRoot(clusterPortOffset)
                    : getAppServerContextRoot(clusterPortOffset));

            appServerInfo.setContextRoot(appServerContextRoot);
            appServerInfo.setBrowserContextRoot(new URL(getAppServerBrowserContextRoot(appServerContextRoot)));

        } catch (MalformedURLException ex) {
            throw new IllegalArgumentException(ex);
        }
        return appServerInfo;
    }

    public static OnlineManagementClient getManagementClient() {
        return getManagementClient(200);
    }

    public static OnlineManagementClient getManagementClient(int portOffset) {
        try {
            return ManagementClient.online(OnlineOptions
                    .standalone()
                    .hostAndPort(System.getProperty("app.server.host", "localhost"), System.getProperty("app.server","").startsWith("eap6") ? 9999 + portOffset : 9990 + portOffset)
                    .protocol(System.getProperty("app.server","").startsWith("eap6") ? ManagementProtocol.REMOTE : ManagementProtocol.HTTP_REMOTING)
                    .build()
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void startAppServer(@Observes(precedence = -1) BeforeClass event) throws MalformedURLException, InterruptedException, IOException {
        // if testClass implements SelfManagedAppContainerLifecycle we skip starting container and let the test to manage the lifecycle itself
        if (SelfManagedAppContainerLifecycle.class.isAssignableFrom(event.getTestClass().getJavaClass())) {
            log.debug("Skipping starting App server. Server should be started by testClass.");
            return;
        }
        if (testContext.isAdapterContainerEnabled() && !testContext.isRelativeAdapterTest()) {
            if (isJBossBased()) {
                prepareServerDir("standalone");
            }
            ContainerController controller = containerConrollerInstance.get();
            if (!controller.isStarted(testContext.getAppServerInfo().getQualifier())) {
                log.info("Starting app server: " + testContext.getAppServerInfo().getQualifier());
                controller.start(testContext.getAppServerInfo().getQualifier());
            }
        }
    }

    public static void enableHTTPSForManagementClient(OnlineManagementClient client) throws CommandFailedException, InterruptedException, TimeoutException, IOException, CliException, OperationException {
        Administration administration = new Administration(client);
        Operations operations = new Operations(client);

        if(!operations.exists(Address.coreService("management").and("security-realm", "UndertowRealm"))) {
            client.execute("/core-service=management/security-realm=UndertowRealm:add()");
            client.execute("/core-service=management/security-realm=UndertowRealm/server-identity=ssl:add(keystore-relative-to=jboss.server.config.dir,keystore-password=secret,keystore-path=adapter.jks");
        }

        client.execute("/system-property=javax.net.ssl.trustStore:add(value=${jboss.server.config.dir}/keycloak.truststore)");
        client.execute("/system-property=javax.net.ssl.trustStorePassword:add(value=secret)");

        if (AppServerTestEnricher.isEAP6AppServer()) {
            if(!operations.exists(Address.subsystem("web").and("connector", "https"))) {
                client.apply(new AddConnector.Builder("https")
                        .protocol("HTTP/1.1")
                        .scheme("https")
                        .socketBinding("https")
                        .secure(true)
                        .build());

                client.apply(new AddConnectorSslConfig.Builder("https")
                        .password("secret")
                        .certificateKeyFile("${jboss.server.config.dir}/adapter.jks")
                        .build());


                String appServerJavaHome = System.getProperty("app.server.java.home", "");
                if (appServerJavaHome.contains("ibm")) {
                    // Workaround for bug in IBM JDK: https://bugzilla.redhat.com/show_bug.cgi?id=1430730
                    // Source: https://access.redhat.com/solutions/4133531
                    client.execute("/subsystem=web/connector=https/configuration=ssl:write-attribute(name=cipher-suite, value=\"SSL_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256,SSL_ECDHE_RSA_WITH_AES_128_CBC_SHA256,SSL_RSA_WITH_AES_128_CBC_SHA256,SSL_ECDH_ECDSA_WITH_AES_128_CBC_SHA256,SSL_ECDH_RSA_WITH_AES_128_CBC_SHA256,SSL_DHE_RSA_WITH_AES_128_CBC_SHA256,SSL_DHE_DSS_WITH_AES_128_CBC_SHA256,SSL_ECDHE_ECDSA_WITH_AES_128_CBC_SHA,SSL_ECDHE_RSA_WITH_AES_128_CBC_SHA,SSL_RSA_WITH_AES_128_CBC_SHA,SSL_ECDH_ECDSA_WITH_AES_128_CBC_SHA,SSL_ECDH_RSA_WITH_AES_128_CBC_SHA,SSL_DHE_RSA_WITH_AES_128_CBC_SHA,SSL_DHE_DSS_WITH_AES_128_CBC_SHA\")");
                }
            }
        } else {
            removeHttpsListener(client, administration);
            addHttpsListenerAppServer(client);
        }

        reloadOrRestartTimeoutClient(administration);
    }

    public static void enableHTTPSForAppServer() throws CommandFailedException, InterruptedException, TimeoutException, IOException, CliException, OperationException {
        try (OnlineManagementClient client = getManagementClient()) {
            enableHTTPSForManagementClient(client);
        }
    }

    public static void enableHTTPSForAppServer(int portOffset) throws CommandFailedException, InterruptedException, TimeoutException, IOException, CliException, OperationException {
        try (OnlineManagementClient client = AppServerTestEnricher.getManagementClient(portOffset)) {
            enableHTTPSForManagementClient(client);
        }
    }

    /*
     * For Fuse: precedence = 2 - app server has to be stopped 
     * before AuthServerTestEnricher.afterClass is executed
     */
    public void stopAppServer(@Observes(precedence = 2) AfterClass event) {
        if (testContext.getAppServerInfo() == null) {
            return; // no adapter test
        }

        ContainerController controller = containerConrollerInstance.get();

        if (controller.isStarted(testContext.getAppServerInfo().getQualifier())) {
            log.info("Stopping app server: " + testContext.getAppServerInfo().getQualifier());
            controller.stop(testContext.getAppServerInfo().getQualifier());
        }
    }

    /**
     * Workaround for WFARQ-44. It cannot be used 'cleanServerBaseDir' property.
     *
     * It copies deployments and configuration into $JBOSS_HOME/standalone-test from where
     * the container is started for the test
     *
     * @param baseDir string representing folder name, relative to app.server.home, from which the copy is made
     * @throws IOException
     */
    public static void prepareServerDir(String baseDir) throws IOException {
        log.debug("Creating cleanServerBaseDir from: " + baseDir);
        Path path = Paths.get(System.getProperty("app.server.home"), "standalone-test");
        File targetSubdirFile = path.toFile();
        FileUtils.deleteDirectory(targetSubdirFile);
        FileUtils.forceMkdir(targetSubdirFile);
        FileUtils.copyDirectory(Paths.get(System.getProperty("app.server.home"), baseDir, "deployments").toFile(), new File(targetSubdirFile, "deployments"));
        FileUtils.copyDirectory(Paths.get(System.getProperty("app.server.home"), baseDir, "configuration").toFile(), new File(targetSubdirFile, "configuration"));
    }

    /**
     *
     * @param testClass
     * @param annotationClass
     * @return testClass or the nearest superclass of testClass annotated with
     * annotationClass
     */
    public static Class getNearestSuperclassWithAppServerAnnotation(Class<?> testClass) {
        return (testClass.isAnnotationPresent(AppServerContainer.class) || testClass.isAnnotationPresent(AppServerContainers.class)) ? testClass
                : (testClass.getSuperclass().equals(Object.class) ? null // stop recursion
                : getNearestSuperclassWithAppServerAnnotation(testClass.getSuperclass())); // continue recursion
    }

    public static boolean hasAppServerContainerAnnotation(Class testClass) {
        return getNearestSuperclassWithAppServerAnnotation(testClass) != null;
    }

    public static boolean isUndertowAppServer() {
        return CURRENT_APP_SERVER.equals("undertow");
    }

    public static boolean isRelative() {
        return CURRENT_APP_SERVER.equals("relative");
    }

    public static boolean isWildflyAppServer() {
        return CURRENT_APP_SERVER.equals("wildfly");
    }

    public static boolean isWildfly10AppServer() {
        return CURRENT_APP_SERVER.equals("wildfly10");
    }

    public static boolean isWildfly9AppServer() {
        return CURRENT_APP_SERVER.equals("wildfly9");
    }

    public static boolean isTomcatAppServer() {
        return CURRENT_APP_SERVER.startsWith("tomcat");
    }

    public static boolean isEAP6AppServer() {
        return CURRENT_APP_SERVER.equals("eap6");
    }

    public static boolean isEAPAppServer() {
        return CURRENT_APP_SERVER.equals("eap");
    }

    public static boolean isWASAppServer() {
        return CURRENT_APP_SERVER.equals("was");
    }

    public static boolean isWLSAppServer() {
        return CURRENT_APP_SERVER.equals("wls");
    }

    public static boolean isRemoteAppServer() {
        return CURRENT_APP_SERVER.contains("remote");
    }

    private boolean isJBossBased() {
        return testContext.getAppServerInfo().isJBossBased();
    }
}
