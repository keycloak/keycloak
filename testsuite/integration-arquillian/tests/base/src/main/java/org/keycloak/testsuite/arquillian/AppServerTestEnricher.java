package org.keycloak.testsuite.arquillian;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import org.jboss.arquillian.container.spi.Container;
import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.InstanceProducer;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.test.spi.annotation.ClassScoped;
import org.jboss.arquillian.test.spi.event.suite.BeforeClass;
import org.jboss.logging.Logger;
import static org.keycloak.testsuite.arquillian.AuthServerTestEnricher.getAuthServerContextRoot;
import static org.keycloak.testsuite.arquillian.AuthServerTestEnricher.getAuthServerQualifier;
import org.keycloak.testsuite.arquillian.annotation.AdapterLibsLocationProperty;
import org.keycloak.testsuite.arquillian.annotation.AppServerContainer;
import static org.keycloak.testsuite.util.IOUtil.execCommand;
import org.keycloak.testsuite.util.LogChecker;
import static org.keycloak.testsuite.util.WaitUtils.pause;

/**
 *
 * @author tkyjovsk
 */
public class AppServerTestEnricher {

    protected final Logger log = Logger.getLogger(this.getClass());

    @Inject
    @ClassScoped
    private InstanceProducer<TestContext> testContextProducer;
    private TestContext testContext;

    @Inject
    private Instance<ContainerController> containerController;

    public static String getAppServerQualifier(Class testClass) {
        Class<? extends AuthServerTestEnricher> annotatedClass = getNearestSuperclassWithAnnotation(testClass, AppServerContainer.class);

        String appServerQ = (annotatedClass == null ? null
                : annotatedClass.getAnnotation(AppServerContainer.class).value());

        return appServerQ;
    }

    public static String getAppServerContextRoot() {
        return getAppServerContextRoot(0);
    }

    public static String getAppServerContextRoot(int clusterPortOffset) {
        int httpPort = Integer.parseInt(System.getProperty("app.server.http.port")); // property must be set
        int httpsPort = Integer.parseInt(System.getProperty("app.server.https.port")); // property must be set
        boolean sslRequired = Boolean.parseBoolean(System.getProperty("app.server.ssl.required"));

        return sslRequired
                ? "https://localhost:" + (httpsPort + clusterPortOffset)
                : "http://localhost:" + (httpPort + clusterPortOffset);
    }

    private ContainerInfo initializeAppServerInfo(Container appServerContainer) {
        return initializeAppServerInfo(appServerContainer, 0);
    }

    private ContainerInfo initializeAppServerInfo(Container appServerContainer, int clusterPortOffset) {
        ContainerInfo appServerInfo = new ContainerInfo(appServerContainer);
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

    public void updateTestContextWithAppServerInfo(@Observes BeforeClass event) {
        testContext = testContextProducer.get();
        String appServerQualifier = getAppServerQualifier(testContext.getTestClass());
        for (Container container : testContext.getSuiteContext().getArquillianContainers()) {
            if (container.getContainerConfiguration().getContainerName().equals(appServerQualifier)) {
                testContext.setAppServerInfo(initializeAppServerInfo(container));
            }
        }
        // validate app server
        if (appServerQualifier != null && testContext.getAppServerInfo() == null) {
            throw new RuntimeException(String.format("No app server container matching '%s' was activated. Check if defined and enabled in arquillian.xml.", appServerQualifier));
        }
        log.info("\n\n" + testContext);
    }

    public void startAppServer(@Observes(precedence = -1) BeforeClass event) throws MalformedURLException, InterruptedException, IOException {
        ContainerController controller = containerController.get();
        if (testContext.isAdapterTest()) {
            String appServerQualifier = testContext.getAppServerInfo().getQualifier();
            if (!controller.isStarted(appServerQualifier)) {
                controller.start(appServerQualifier);
            }
            log.info("\n\n\nAPP SERVER STARTED\n\n\n");
            // install adapter libs on JBoss-based container via CLI
//            if (testContext.getAppServerInfo().isJBossBased()) {
                installAdapterLibsUsingJBossCLIClient(testContext.getAppServerInfo());
//            }
        }
    }

    private void installAdapterLibsUsingJBossCLIClient(ContainerInfo appServerInfo) throws InterruptedException, IOException {
        
        log.info("Installing adapter via CLI client");
        
        if (!appServerInfo.isJBossBased()) {
            throw new IllegalArgumentException("App server must be JBoss-based to run jboss-cli-client.");
        }

        String jbossHomePath = appServerInfo.getProperties().get("jbossHome");

        File bin = new File(jbossHomePath + "/bin");
        String command = "java -jar " + jbossHomePath + "/bin/client/jboss-cli-client.jar";
        String adapterScript = "adapter-install.cli";
        String samlAdapterScript = "adapter-install-saml.cli";
        String managementPort = appServerInfo.getProperties().get("managementPort");

        String controllerArg = " --controller=localhost:" + managementPort;
        if (new File(bin, adapterScript).exists()) {
            log.info("Installing adapter to app server via cli script");
            execCommand(command + " --connect --file=" + adapterScript + controllerArg, bin);
        }
        if (new File(bin, samlAdapterScript).exists()) {
            log.info("Installing saml adapter to app server via cli script");
            execCommand(command + " --connect --file=" + samlAdapterScript + controllerArg, bin);
        }
        if (new File(bin, adapterScript).exists() || new File(bin, samlAdapterScript).exists()) {
            log.info("Restarting container");
            execCommand(command + " --connect --command=reload" + controllerArg, bin);
            log.info("Container restarted");
            pause(5000);
            LogChecker.checkJBossServerLog(jbossHomePath);
        }
    }

    /**
     *
     * @param testClass
     * @param annotationClass
     * @return testClass or the nearest superclass of testClass annotated with
     * annotationClass
     */
    public static Class getNearestSuperclassWithAnnotation(Class testClass, Class annotationClass) {
        return testClass.isAnnotationPresent(annotationClass) ? testClass
                : (testClass.getSuperclass().equals(Object.class) ? null // stop recursion
                        : getNearestSuperclassWithAnnotation(testClass.getSuperclass(), annotationClass)); // continue recursion
    }

    public static boolean hasAppServerContainerAnnotation(Class testClass) {
        return getNearestSuperclassWithAnnotation(testClass, AppServerContainer.class) != null;
    }

    public static boolean isRelative(Class testClass) {
        return getAppServerQualifier(testClass).equals(getAuthServerQualifier());
    }

    public static String getAdapterLibsLocationProperty(Class testClass) {
        Class<? extends AuthServerTestEnricher> annotatedClass = getNearestSuperclassWithAnnotation(testClass, AdapterLibsLocationProperty.class);
        return (annotatedClass == null ? null
                : annotatedClass.getAnnotation(AdapterLibsLocationProperty.class).value());
    }

    public static boolean isWildflyAppServer(Class testClass) {
        return getAppServerQualifier(testClass).contains("wildfly");
    }

    public static boolean isTomcatAppServer(Class testClass) {
        return getAppServerQualifier(testClass).contains("tomcat");
    }

    public static boolean isOSGiAppServer(Class testClass) {
        String q = getAppServerQualifier(testClass);
        return q.contains("karaf") || q.contains("fuse");
    }

}
