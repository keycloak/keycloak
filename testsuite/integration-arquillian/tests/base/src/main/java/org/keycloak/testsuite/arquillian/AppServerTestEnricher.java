package org.keycloak.testsuite.arquillian;

import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.InstanceProducer;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.test.spi.annotation.ClassScoped;
import org.jboss.arquillian.test.spi.event.suite.BeforeClass;
import org.jboss.logging.Logger;
import org.keycloak.testsuite.arquillian.annotation.AdapterLibsLocationProperty;
import org.keycloak.testsuite.arquillian.annotation.AppServerContainer;
import org.keycloak.testsuite.util.LogChecker;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import static org.keycloak.testsuite.util.IOUtil.execCommand;
import static org.keycloak.testsuite.util.WaitUtils.pause;
import static org.keycloak.testsuite.arquillian.AuthServerTestEnricher.getAuthServerContextRoot;

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
        int httpPort = Integer.parseInt(System.getProperty("app.server.http.port")); // property must be set
        int httpsPort = Integer.parseInt(System.getProperty("app.server.https.port")); // property must be set

        boolean sslRequired = Boolean.parseBoolean(System.getProperty("app.server.ssl.required"));
        String scheme = sslRequired ? "https" : "http";
        int port = sslRequired ? httpsPort : httpPort;

        return String.format("%s://%s:%s", scheme, host, port + clusterPortOffset);
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

    @Inject
    private Instance<ContainerController> containerConrollerInstance;

    public void startAppServer(@Observes(precedence = -1) BeforeClass event) throws MalformedURLException, InterruptedException, IOException {
        if (testContext.isAdapterTest() && !testContext.isRelativeAdapterTest()) {
            ContainerController controller = containerConrollerInstance.get();
            if (!controller.isStarted(testContext.getAppServerInfo().getQualifier())) {
                log.info("Starting app server: " + testContext.getAppServerInfo().getQualifier());
                controller.start(testContext.getAppServerInfo().getQualifier());
            }
        }
    }

//    public void installAdapterLibs(@Observes BeforeDeploy event) {
//        log.debug("BEFORE DEPLOY - INSTALL ADAPTER LIBS");
//        if (testContext.isAdapterTest()) {
//            // install adapter libs on JBoss-based container via CLI
//            if (testContext.getAppServerInfo().isJBossBased()) {
//                try {
//                    installAdapterLibsUsingJBossCLIClient(testContext.getAppServerInfo());
//                } catch (InterruptedException | IOException ex) {
//                    throw new RuntimeException("Failed to install adapter libs.", ex);
//                }
//            }
//        }
//    }
    private void installAdapterLibsUsingJBossCLIClient(ContainerInfo appServerInfo) throws InterruptedException, IOException {
        if (!appServerInfo.isAdapterLibsInstalled()) {

            if (!appServerInfo.isJBossBased()) {
                throw new IllegalArgumentException("App server must be JBoss-based to run jboss-cli-client.");
            }

            String jbossHomePath = appServerInfo.getProperties().get("jbossHome");

            File bin = new File(jbossHomePath + "/bin");

            File clientJar = new File(jbossHomePath + "/bin/client/jboss-cli-client.jar");
            if (!clientJar.exists()) {
                clientJar = new File(jbossHomePath + "/bin/client/jboss-client.jar"); // AS7
            }
            if (!clientJar.exists()) {
                throw new IOException("JBoss CLI client JAR not found.");
            }

            String command = "java -jar " + clientJar.getAbsolutePath();
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
                if (System.getProperty("app.server.log.check", "true").equals("true")) {
                    LogChecker.checkJBossServerLog(jbossHomePath);
                }
            }

            appServerInfo.setAdapterLibsInstalled(true);
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
        return getAppServerQualifier(testClass).equals(AuthServerTestEnricher.AUTH_SERVER_CONTAINER);
    }

    public static String getAdapterLibsLocationProperty(Class testClass) {
        Class<? extends AuthServerTestEnricher> annotatedClass = getNearestSuperclassWithAnnotation(testClass, AdapterLibsLocationProperty.class);
        return (annotatedClass == null ? "adapter.libs.home"
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
