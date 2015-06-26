package org.keycloak.testsuite.arquillian;

import org.keycloak.testsuite.arquillian.annotation.AppServerContainer;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainer;
import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.test.spi.event.suite.BeforeClass;

/**
 *
 * @author tkyjovsk
 */
public class ContainersManager {

    @Inject
    private Instance<ContainerController> containerController;

    private String authServerQualifier;
    private String appServerQualifier;

    public void startContainers(@Observes(precedence = -1) BeforeClass event) {
        this.authServerQualifier = getAuthServerQualifier(event.getTestClass().getJavaClass());
        this.appServerQualifier = getAppServerQualifier(event.getTestClass().getJavaClass());
        System.out.println(event.getTestClass().getJavaClass().getSimpleName());
        System.out.println("Auth server: " + authServerQualifier);
        System.out.println("App server:  " + appServerQualifier);
        if (appServerQualifier.equals(authServerQualifier)) {
            System.out.println("App server == Auth server");
        }
        startContainers();
    }

    private void startContainers() {
        ContainerController controller = containerController.get();
        if (!controller.isStarted(authServerQualifier)) {
            System.out.println("Starting Auth server: " + authServerQualifier);
            controller.start(authServerQualifier);
        }
        if (!controller.isStarted(appServerQualifier)) {
            System.out.println("Starting App server: " + appServerQualifier);
            controller.start(appServerQualifier);
        }
    }

    public static Class<? extends ContainersManager>
            getNearestSuperclassWithAnnotation(Class clazz, Class annotationClass) {
        return clazz.isAnnotationPresent(annotationClass) ? clazz
                : (clazz.equals(Object.class) || clazz.equals(ContainersManager.class) ? null // stop recursion
                        : getNearestSuperclassWithAnnotation(clazz.getSuperclass(), annotationClass)); // continue recursion
    }

    public static String getAuthServerQualifier(Class clazz) {
        Class<? extends ContainersManager> annotatedClass = getNearestSuperclassWithAnnotation(clazz, AuthServerContainer.class);
        if (annotatedClass == null) {
            throw new IllegalStateException("Couldn't find @AuthServerContainer on the test class or any of its superclasses.");
        }
        String authServerQ = annotatedClass.getAnnotation(AuthServerContainer.class).value();
        if (authServerQ == null || authServerQ.isEmpty()) {
            throw new IllegalStateException("Null or empty qualifier for keycloak auth server.");
            // TODO add fallback mechanism when embedded undertow is ready
        }
        return authServerQ;
    }

    public static String getAppServerQualifier(Class clazz) {
        Class<? extends ContainersManager> annotatedClass = getNearestSuperclassWithAnnotation(clazz, AppServerContainer.class);

        String appServerQ = (annotatedClass == null ? null
                : annotatedClass.getAnnotation(AppServerContainer.class).value());

        return appServerQ == null || appServerQ.isEmpty()
                ? getAuthServerQualifier(clazz) // app server == auth server
                : appServerQ;
    }

    public static boolean isRelative(Class clazz) {
        return getAppServerQualifier(clazz).equals(getAuthServerQualifier(clazz));
    }

    public boolean isRelative() {
        return appServerQualifier.equals(authServerQualifier);
    }

}
