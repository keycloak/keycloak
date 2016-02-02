package org.keycloak.testsuite.arquillian;

import static org.keycloak.testsuite.auth.page.AuthRealm.ADMIN;
import static org.keycloak.testsuite.auth.page.AuthRealm.MASTER;
import static org.keycloak.testsuite.util.WaitUtils.pause;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedList;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.jboss.arquillian.container.spi.Container;
import org.jboss.arquillian.container.spi.ContainerRegistry;
import org.jboss.arquillian.container.spi.event.StartSuiteContainers;
import org.jboss.arquillian.container.spi.event.StopSuiteContainers;
import org.jboss.arquillian.container.spi.event.container.AfterStart;
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
import org.keycloak.admin.client.Keycloak;
import org.keycloak.models.Constants;
import org.keycloak.testsuite.arquillian.annotation.AdapterLibsLocationProperty;
import org.keycloak.testsuite.arquillian.annotation.AppServerContainer;
import org.keycloak.testsuite.util.OAuthClient;

/**
 *
 * @author tkyjovsk
 * @author vramik
 */
public class ContainersTestEnricher {

    protected final Logger log = Logger.getLogger(this.getClass());

    @Inject
    private Instance<ContainerController> containerController;

    @Inject
    private Instance<ContainerRegistry> containerRegistry;

    @Inject
    private Event<StopSuiteContainers> stopSuiteContainers;

    private String appServerQualifier;

    private static final String AUTH_SERVER_CONTAINER_PROPERTY = "auth.server.container";
    private static final String AUTH_SERVER_CONTAINER_DEFAULT = "auth-server-undertow";

    @Inject
    @SuiteScoped
    private InstanceProducer<SuiteContext> suiteContext;

    @Inject
    @ClassScoped
    private InstanceProducer<TestContext> testContext;

    @Inject
    @ClassScoped
    private InstanceProducer<Keycloak> adminClient;

    @Inject
    @ClassScoped
    private InstanceProducer<OAuthClient> oauthClient;

    private ContainerController controller;
    private LinkedList<Container> containers;

    private String jbossHomePath;
    private final boolean migrationTests = System.getProperty("migration", "false").equals("true");
    private final boolean skipInstallAdapters = System.getProperty("skip.install.adapters", "false").equals("true");
    private boolean alreadyInstalled = false;
    private boolean alreadyStopped = false;
    private boolean init = false;

    private void init() {
        if (!init) {
            containers = new LinkedList<>(containerRegistry.get().getContainers());
        }
        init = true;
    }
    
    /*
     * non-javadoc
     *
     * Before starting suite containers. Initialization of containers is done 
     * (only once during class life cycle)
     */
    public void startSuiteContainers(@Observes(precedence = 1) StartSuiteContainers event) {
        init();
        if (migrationTests) {
            log.info("\n\n### Starting keycloak " + System.getProperty("version", "- previous") + " ###\n");
        }
    }

    /*
     * non-javadoc
     *
     * After start container. Server logs are checked (in case jboss based container).
     * In case of migration scenario: previous container is stopped.
     */
    public void afterStart(@Observes AfterStart event) throws IOException, InterruptedException {
        Container container = containers.pollFirst();

        if (isJBossBased(container)) {
            jbossHomePath = container.getContainerConfiguration().getContainerProperties().get("jbossHome");
            log.debug("jbossHome: " + jbossHomePath + "\n");
        }
        checkServerLog(jbossHomePath);

        if (migrationTests && !alreadyStopped) {
            log.info("\n\n### Stopping keycloak " + System.getProperty("version", "- previous") + " ###\n");
            stopSuiteContainers.fire(new StopSuiteContainers());
            log.info("\n\n### Starting keycloak current version ###\n");
            alreadyStopped = true;
        }
        installAdapters(container);
    }

    /*
     * non-javadoc
     *
     * check server logs whether there are no ERRORs or SEVEREs
     */
    private void checkServerLog(String jbossHomePath) throws IOException {
        if (jbossHomePath != null && System.getProperty("check.server.log", "true").equals("true")) {
            File serverLog = new File(jbossHomePath + "/standalone/log/server.log");
            String serverLogContent = FileUtils.readFileToString(serverLog);

            boolean containsError
                    = serverLogContent.contains("ERROR")
                    || serverLogContent.contains("SEVERE")
                    || serverLogContent.contains("Exception ");
            //There is expected string "Exception" in server log: Adding provider 
            //singleton org.keycloak.services.resources.ModelExceptionMapper

            if (containsError) {
                throw new RuntimeException(serverLog.getPath() + " contains ERROR.");
            }
            log.info(serverLog.getPath() + " doesn't contain Error");
        } 
    }

    public void beforeSuite(@Observes BeforeSuite event) {
        suiteContext.set(new SuiteContext());
    }

    public void startContainers(@Observes(precedence = -1) BeforeClass event) {
        controller = containerController.get();

        Class testClass = event.getTestClass().getJavaClass();
        appServerQualifier = getAppServerQualifier(testClass);

        if (!controller.isStarted(appServerQualifier)) {
            controller.start(appServerQualifier);
        }

        initializeTestContext(testClass);
        initializeAdminClient();
        initializeOAuthClient();
    }

    private void initializeTestContext(Class testClass) {
        String authServerContextRootStr = getAuthServerContextRootFromSystemProperty();
        String appServerContextRootStr = isRelative(testClass)
                ? authServerContextRootStr
                : getAppServerContextRootFromSystemProperty();
        try {
            URL authServerContextRoot = new URL(authServerContextRootStr);
            URL appServerContextRoot = new URL(appServerContextRootStr);

            testContext.set(new TestContext(authServerContextRoot, appServerContextRoot));

        } catch (MalformedURLException ex) {
            throw new IllegalStateException("Malformed url.", ex);
        }
    }

    private void initializeAdminClient() {
        adminClient.set(Keycloak.getInstance(
                getAuthServerContextRootFromSystemProperty() + "/auth",
                MASTER, ADMIN, ADMIN, Constants.ADMIN_CLI_CLIENT_ID));
    }

    private void initializeOAuthClient() {
        oauthClient.set(new OAuthClient(getAuthServerContextRootFromSystemProperty() + "/auth"));
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

    public static String getAuthServerQualifier() {
        return System.getProperty(
                AUTH_SERVER_CONTAINER_PROPERTY,
                AUTH_SERVER_CONTAINER_DEFAULT);
    }

    public static String getAppServerQualifier(Class testClass) {
        Class<? extends ContainersTestEnricher> annotatedClass = getNearestSuperclassWithAnnotation(testClass, AppServerContainer.class);

        String appServerQ = (annotatedClass == null ? null
                : annotatedClass.getAnnotation(AppServerContainer.class).value());

        return appServerQ == null || appServerQ.isEmpty()
                ? getAuthServerQualifier() // app server == auth server
                : appServerQ;
    }

    public static boolean hasAppServerContainerAnnotation(Class testClass) {
        return getNearestSuperclassWithAnnotation(testClass, AppServerContainer.class) != null;
    }

    public static boolean isRelative(Class testClass) {
        return getAppServerQualifier(testClass).equals(getAuthServerQualifier());
    }

    public static String getAdapterLibsLocationProperty(Class testClass) {
        Class<? extends ContainersTestEnricher> annotatedClass = getNearestSuperclassWithAnnotation(testClass, AdapterLibsLocationProperty.class);
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

    public static String getAuthServerContextRootFromSystemProperty() {
        // TODO find if this can be extracted from ARQ metadata instead of System properties
        boolean sslRequired = Boolean.parseBoolean(System.getProperty("auth.server.ssl.required"));
        if (sslRequired) {
            return "https://localhost:" + Integer.parseInt(System.getProperty("auth.server.https.port", "8543"));
        }
        return "http://localhost:" + Integer.parseInt(System.getProperty("auth.server.http.port", "8180"));
    }

    public static String getAppServerContextRootFromSystemProperty() {
        boolean sslRequired = Boolean.parseBoolean(System.getProperty("app.server.ssl.required"));
        if (sslRequired) {
            return "https://localhost:" + Integer.parseInt(System.getProperty("app.server.https.port", "8643"));
        }
        return "http://localhost:" + Integer.parseInt(System.getProperty("app.server.http.port", "8280"));
    }

    private void installAdapters(Container container) throws InterruptedException, IOException {
        if (!alreadyInstalled && !skipInstallAdapters && isJBossBased(container)) {
            File bin = new File(jbossHomePath + "/bin");
            String command = "java -jar " + jbossHomePath + "/bin/client/jboss-cli-client.jar";
            String adapterScript = "adapter-install.cli";
            String samlAdapterScript = "adapter-install-saml.cli";
            String managementPort = container.getContainerConfiguration().getContainerProperties().get("managementPort");

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
                checkServerLog(jbossHomePath);
            }
            if (container.getName().startsWith("app-server")) {
                alreadyInstalled = true;
            }
        }
    }

    private void execCommand(String command, File dir) throws IOException, InterruptedException {
        Process process = Runtime.getRuntime().exec(command, null, dir);
        if (process.waitFor(10, TimeUnit.SECONDS)) {
            if (process.exitValue() != 0) {
                getOutput("ERROR", process.getErrorStream());
                throw new RuntimeException("Adapter installation failed. Process exitValue: " 
                        + process.exitValue());
            }
            getOutput("OUTPUT", process.getInputStream());
            log.debug("process.isAlive(): " + process.isAlive());
        } else {
            if (process.isAlive()) {
                process.destroyForcibly();
            }
            throw new RuntimeException("Timeout after 10 seconds.");
        }
    }

    private void getOutput(String type, InputStream is) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder builder = new StringBuilder();
        builder.append("<").append(type).append(">");
        System.out.println(builder);
        builder = new StringBuilder();
        while (reader.ready()) {
            System.out.println(reader.readLine());
        }
        builder.append("</").append(type).append(">");
        System.out.println(builder);
    }

    private boolean isJBossBased(Container container) {
        if (container == null) {
            return false;
        }
        return container.getName().matches("a.*-server-wildfly")
                || container.getName().matches("a.*-server-eap.")
                || container.getName().equals("app-server-as7");
    }
}
