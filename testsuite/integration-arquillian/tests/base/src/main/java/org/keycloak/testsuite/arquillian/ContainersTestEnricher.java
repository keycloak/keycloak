package org.keycloak.testsuite.arquillian;

import java.net.MalformedURLException;
import java.net.URL;
import org.jboss.arquillian.container.spi.event.StartSuiteContainers;
import org.jboss.arquillian.container.spi.event.StopSuiteContainers;
import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.core.api.Event;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.InstanceProducer;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.test.spi.annotation.ClassScoped;
import org.jboss.arquillian.test.spi.annotation.SuiteScoped;
import org.jboss.arquillian.container.spi.event.container.AfterStart;
import org.jboss.arquillian.test.spi.event.suite.BeforeClass;
import org.jboss.arquillian.test.spi.event.suite.BeforeSuite;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.models.Constants;
import org.keycloak.testsuite.arquillian.annotation.AdapterLibsLocationProperty;
import org.keycloak.testsuite.arquillian.annotation.AppServerContainer;
import static org.keycloak.testsuite.auth.page.AuthRealm.ADMIN;
import static org.keycloak.testsuite.auth.page.AuthRealm.MASTER;

/**
 *
 * @author tkyjovsk
 * @author vramik
 */
public class ContainersTestEnricher {

    @Inject
    private Instance<ContainerController> containerController;

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

    private ContainerController controller;

    private final boolean migrationTests = System.getProperty("migration", "false").equals("true");
    private boolean alreadyStopped = false;

    public void startSuiteContainers(@Observes(precedence = 1) StartSuiteContainers event) {
        if (migrationTests) {
            System.out.println("\n### Starting keycloak with previous version ###\n");
        }
    }

    public void stopMigrationContainer(@Observes AfterStart event) {
        if (migrationTests && !alreadyStopped) {
            System.out.println("\n### Stopping keycloak with previous version ###\n");
            stopSuiteContainers.fire(new StopSuiteContainers());
        }
        alreadyStopped = true;
    }
    
    public void beforeSuite(@Observes BeforeSuite event) {
        suiteContext.set(new SuiteContext());
    }

    public void startContainers(@Observes(precedence = -1) BeforeClass event) {
        controller = containerController.get();

        Class testClass = event.getTestClass().getJavaClass();
        appServerQualifier = getAppServerQualifier(testClass);

        if (!controller.isStarted(appServerQualifier)) {
            System.out.println("\nSTARTING APP SERVER: " + appServerQualifier + "\n");
            controller.start(appServerQualifier);
            System.out.println("");
        }

        initializeTestContext(testClass);
        initializeAdminClient();
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
                MASTER, ADMIN, ADMIN, Constants.ADMIN_CONSOLE_CLIENT_ID));
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
        return "http://localhost:" + Integer.parseInt(
                System.getProperty("auth.server.http.port", "8180"));
    }

    public static String getAppServerContextRootFromSystemProperty() {
        return "http://localhost:" + Integer.parseInt(
                System.getProperty("app.server.http.port", "8280"));
    }

}
