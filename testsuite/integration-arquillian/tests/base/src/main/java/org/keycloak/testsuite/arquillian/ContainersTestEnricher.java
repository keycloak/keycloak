package org.keycloak.testsuite.arquillian;

import java.net.MalformedURLException;
import java.net.URL;
import org.keycloak.testsuite.arquillian.annotation.AppServerContainer;
import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.InstanceProducer;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.test.spi.annotation.ClassScoped;
import org.jboss.arquillian.test.spi.annotation.SuiteScoped;
import org.jboss.arquillian.test.spi.event.suite.BeforeClass;
import org.jboss.arquillian.test.spi.event.suite.BeforeSuite;
import org.keycloak.testsuite.arquillian.annotation.AdapterLibsLocationProperty;

/**
 *
 * @author tkyjovsk
 */
public class ContainersTestEnricher {

    @Inject
    private Instance<ContainerController> containerController;

    private String authServerQualifier;
    private String appServerQualifier;

    private static final String AUTH_SERVER_CONTAINER_PROPERTY = "auth.server.container";
    private static final String AUTH_SERVER_CONTAINER_DEFAULT = "auth-server-undertow";

    @Inject
    @SuiteScoped
    private InstanceProducer<SuiteContext> suiteContext;

    @Inject
    @ClassScoped
    private InstanceProducer<TestContext> testContext;

    private ContainerController controller;

    public void beforeSuite(@Observes BeforeSuite event) {
        suiteContext.set(new SuiteContext());
    }

    public void startContainers(@Observes(precedence = -1) BeforeClass event) {
        controller = containerController.get();

        Class testClass = event.getTestClass().getJavaClass();
        System.out.println("\nCONTAINERS LIFECYCLE FOR: " + testClass.getSimpleName() + "\n");

        authServerQualifier = getAuthServerQualifier();
        appServerQualifier = getAppServerQualifier(testClass);

        System.out.println("STARTING AUTH SERVER: " + authServerQualifier + "\n");
        controller.start(authServerQualifier);
        System.out.println("");
        if (!controller.isStarted(appServerQualifier)) {
            System.out.println("STARTING APP SERVER: " + appServerQualifier + "\n");
            controller.start(appServerQualifier);
            System.out.println("");
        }

        initializeTestContext(testClass);
    }

    private void initializeTestContext(Class testClass) {
        String authServerContextRootStr = getAuthServerContextRootFromSystemProperty();
        String appServerContextRootStr = isRelative(testClass)
                ? authServerContextRootStr
                : getAppServerContextRootFromSystemProperty();
        try {
            URL authServerContextRoot = new URL(authServerContextRootStr);
            URL appServerContextRoot = new URL(appServerContextRootStr);

            TestContext context = new TestContext(authServerContextRoot, appServerContextRoot);
            context.setTestClass(testClass);

            testContext.set(context);

        } catch (MalformedURLException ex) {
            throw new IllegalStateException("Malformed url.", ex);
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
