package org.keycloak.testsuite.arquillian;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import org.keycloak.testsuite.arquillian.annotation.AppServerContainer;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainer;
import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.test.spi.event.suite.BeforeClass;
import org.junit.AfterClass;
import org.keycloak.testsuite.arquillian.annotation.AdapterLibsLocationProperty;

/**
 *
 * @author tkyjovsk
 */
public class ContainersManager {

    @Inject
    private Instance<ContainerController> containerController;

    private String authServerQualifier;
    private String appServerQualifier;

    private static final String AUTH_SERVER_CONTAINER_PROPERTY = "auth.server.container";
    private static final String AUTH_SERVER_CONTAINER_DEFAULT = "auth-server-undertow";

    public void startContainers(@Observes(precedence = -1) BeforeClass event) {
        resolveQualifiersFromTestClass(event.getTestClass().getJavaClass());
        startContainers();
    }

    private void resolveQualifiersFromTestClass(Class testClass) {
        this.authServerQualifier = getAuthServerQualifier(testClass);
        this.appServerQualifier = getAppServerQualifier(testClass);
        System.out.println(testClass.getSimpleName());
        System.out.println("Auth server: " + authServerQualifier);
        if (!appServerQualifier.equals(authServerQualifier)) {
            System.out.println("App server:  " + appServerQualifier);
        }
    }

    private void startContainers() {
        ContainerController controller = containerController.get();
        if (!controller.isStarted(authServerQualifier)) {
            System.out.println("Starting Auth server: " + authServerQualifier);
            controller.start(authServerQualifier);
        }
        if (!appServerQualifier.equals(authServerQualifier)
                && !controller.isStarted(appServerQualifier)) {
            System.out.println("Starting App server: " + appServerQualifier);
            controller.start(appServerQualifier);
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

    public static String getAuthServerQualifier(Class testClass) {
        Class<Object> annotatedClass = getNearestSuperclassWithAnnotation(testClass, AuthServerContainer.class);
        String authServerQ = "";
        if (annotatedClass != null) {
            authServerQ = annotatedClass.getAnnotation(AuthServerContainer.class).value();
        }
        if (authServerQ == null || authServerQ.isEmpty()) {
            // default to Undertow
            authServerQ = System.getProperty(AUTH_SERVER_CONTAINER_PROPERTY, AUTH_SERVER_CONTAINER_DEFAULT);
        }
        return authServerQ;
    }

    public static String getAppServerQualifier(Class testClass) {
        Class<? extends ContainersManager> annotatedClass = getNearestSuperclassWithAnnotation(testClass, AppServerContainer.class);

        String appServerQ = (annotatedClass == null ? null
                : annotatedClass.getAnnotation(AppServerContainer.class).value());

        return appServerQ == null || appServerQ.isEmpty()
                ? getAuthServerQualifier(testClass) // app server == auth server
                : appServerQ;
    }

    public static boolean hasAppServerContainerAnnotation(Class testClass) {
        return getNearestSuperclassWithAnnotation(testClass, AppServerContainer.class) != null;
    }

    public static boolean isRelative(Class testClass) {
        return getAppServerQualifier(testClass).equals(getAuthServerQualifier(testClass));
    }

    public static String getAdapterLibsLocationProperty(Class testClass) {
        Class<? extends ContainersManager> annotatedClass = getNearestSuperclassWithAnnotation(testClass, AdapterLibsLocationProperty.class);
        return (annotatedClass == null ? null
                : annotatedClass.getAnnotation(AdapterLibsLocationProperty.class).value());
    }

//    public void beforeClassAdminPassword(@Observes BeforeClass event) {
//        if (authServerQualifier.contains("wildfly")) {
//            // for wildfly set admin pwd status from system property
//            AdminPasswordUpdateTracker.setAdminPasswordUpdatedFor(authServerQualifier,
//                    Boolean.parseBoolean(System.getProperty("adminPasswordUpdated", "false")));
//        }
//    }
    public void afterClassAdminPassword(@Observes AfterClass event) {
        if (authServerQualifier.contains("undertow")) {
            // reset admin pwd status only for undertow
            AdminPasswordUpdateTracker.setAdminPasswordUpdatedFor(authServerQualifier, false);
        }
    }

    public static class AdminPasswordUpdateTracker {

        private static final Set<String> authServersWithUpdatedAdminPassword = new HashSet<>();

        public static boolean isAdminPasswordUpdated(Class testClass) {
            return isAdminPasswordUpdated(getAuthServerQualifier(testClass));
        }

        public static boolean isAdminPasswordUpdated(String containerQualifier) {
            return authServersWithUpdatedAdminPassword.contains(containerQualifier);
        }

        public static void setAdminPasswordUpdatedFor(Class testClass, boolean updated) {
            setAdminPasswordUpdatedFor(getAuthServerQualifier(testClass), updated);
        }

        public static void setAdminPasswordUpdatedFor(String containerQualifier, boolean updated) {
            if (updated) {
                authServersWithUpdatedAdminPassword.add(containerQualifier);
            } else {
                authServersWithUpdatedAdminPassword.remove(containerQualifier);
            }
        }
    }

}
