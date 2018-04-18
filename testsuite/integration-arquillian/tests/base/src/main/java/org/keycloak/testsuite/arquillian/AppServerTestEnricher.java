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

import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.InstanceProducer;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.test.spi.annotation.ClassScoped;
import org.jboss.arquillian.test.spi.event.suite.BeforeClass;
import org.jboss.logging.Logger;
import org.keycloak.testsuite.arquillian.annotation.AppServerContainer;
import org.wildfly.extras.creaper.core.ManagementClient;
import org.wildfly.extras.creaper.core.online.ManagementProtocol;
import org.wildfly.extras.creaper.core.online.OnlineManagementClient;
import org.wildfly.extras.creaper.core.online.OnlineOptions;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import static org.keycloak.testsuite.arquillian.AuthServerTestEnricher.getAuthServerContextRoot;

/**
 *
 * @author tkyjovsk
 */
public class AppServerTestEnricher {

    protected final Logger log = Logger.getLogger(this.getClass());

    public static final String APP_SERVER_DEFAULT = "app-server-undertow";
    
    @Inject
    private Instance<ContainerController> containerConrollerInstance;
    @Inject
    @ClassScoped
    private InstanceProducer<TestContext> testContextProducer;
    private TestContext testContext;

    public static String getAppServerQualifier(Class testClass) {
        Class<? extends AuthServerTestEnricher> annotatedClass = getNearestSuperclassWithAnnotation(testClass, AppServerContainer.class);

        String appServerQ = (annotatedClass == null ? null
                : annotatedClass.getAnnotation(AppServerContainer.class).value());

        return annotatedClass == null ? null // no @AppServerContainer annotation --> no adapter test
                : (appServerQ == null || appServerQ.isEmpty() // @AppServerContainer annotation present but qualifier not set --> relative adapter test
                        ? AuthServerTestEnricher.AUTH_SERVER_CONTAINER // app server == auth server
                        : appServerQ);
    }

    public static String getAppServerContextRoot() {
        return getAppServerContextRoot(0);
    }

    public static String getAppServerContextRoot(int clusterPortOffset) {
        String host = System.getProperty("app.server.host", "localhost");
        
        boolean sslRequired = Boolean.parseBoolean(System.getProperty("app.server.ssl.required"));
  
        int port = sslRequired ? parsePort("app.server.https.port") : parsePort("app.server.http.port");
        String scheme = sslRequired ? "https" : "http";

        return String.format("%s://%s:%s", scheme, host, port + clusterPortOffset);
    }
    
    private static int parsePort(String property) {
        try {
            return Integer.parseInt(System.getProperty(property));
        } catch (NumberFormatException ex) {
            throw new RuntimeException("Failed to get " + property, ex);
        }
    }

    public void updateTestContextWithAppServerInfo(@Observes(precedence = 1) BeforeClass event) {
        testContext = testContextProducer.get();
        String appServerQualifier = getAppServerQualifier(testContext.getTestClass());
        for (ContainerInfo container : testContext.getSuiteContext().getContainers()) {
            if (container.getQualifier().equals(appServerQualifier)) {
                testContext.setAppServerInfo(updateWithAppServerInfo(container));
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

            String appServerContextRootStr = isRelative(testContext.getTestClass())
                    ? getAuthServerContextRoot(clusterPortOffset)
                    : getAppServerContextRoot(clusterPortOffset);

            appServerInfo.setContextRoot(new URL(appServerContextRootStr));

        } catch (MalformedURLException ex) {
            throw new IllegalArgumentException(ex);
        }
        return appServerInfo;
    }

    public static OnlineManagementClient getManagementClient() {
        OnlineManagementClient managementClient;
        try {
            managementClient = ManagementClient.online(OnlineOptions
                    .standalone()
                    .hostAndPort(System.getProperty("app.server.host"), System.getProperty("app.server","").startsWith("eap6") ? 10199 : 10190)
                    .protocol(System.getProperty("app.server","").startsWith("eap6") ? ManagementProtocol.REMOTE : ManagementProtocol.HTTP_REMOTING)
                    .build()
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return managementClient;
    }

    public void startAppServer(@Observes(precedence = -1) BeforeClass event) throws MalformedURLException, InterruptedException, IOException {
        if (testContext.isAdapterTest() && !testContext.isRelativeAdapterTest()) {
            ContainerController controller = containerConrollerInstance.get();
            if (!controller.isStarted(testContext.getAppServerInfo().getQualifier())) {
                log.info("Starting app server: " + testContext.getAppServerInfo().getQualifier());
                controller.start(testContext.getAppServerInfo().getQualifier());
            }
        }
    }

    /**
     *
     * @param testClass
     * @param annotationClass
     * @return testClass or the nearest superclass of testClass annotated with
     * annotationClass
     */
    public static Class getNearestSuperclassWithAnnotation(Class<?> testClass, Class annotationClass) {
        return testClass.isAnnotationPresent(annotationClass) ? testClass
                : (testClass.getSuperclass().equals(Object.class) ? null // stop recursion
                : getNearestSuperclassWithAnnotation(testClass.getSuperclass(), annotationClass)); // continue recursion
    }

    public static boolean hasAppServerContainerAnnotation(Class testClass) {
        return getNearestSuperclassWithAnnotation(testClass, AppServerContainer.class) != null;
    }

    public static boolean isRelative(Class testClass) {
        return getAppServerQualifier(testClass).equals(AuthServerTestEnricher.AUTH_SERVER_CONTAINER);
    }

    public static boolean isWildflyAppServer(Class testClass) {
        return getAppServerQualifier(testClass).contains("wildfly");
    }

    public static boolean isTomcatAppServer(Class testClass) {
        return getAppServerQualifier(testClass).contains("tomcat");
    }

    public static boolean isWASAppServer(Class testClass) {
        return getAppServerQualifier(testClass).contains("was");
    }

    public static boolean isWLSAppServer(Class testClass) {
        return getAppServerQualifier(testClass).contains("wls");
    }

    public static boolean isOSGiAppServer(Class testClass) {
        String q = getAppServerQualifier(testClass);
        return q.contains("karaf") || q.contains("fuse");
    }

}
