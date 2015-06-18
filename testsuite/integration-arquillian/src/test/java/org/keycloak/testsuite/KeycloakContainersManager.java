package org.keycloak.testsuite;

import org.keycloak.testsuite.arquillian.AuthServerContainer;
import org.keycloak.testsuite.arquillian.AppServerContainer;
import java.util.logging.Logger;
import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.junit.Before;
import org.junit.runner.RunWith;

/**
 *
 * @author tkyjovsk
 */
@RunWith(Arquillian.class)
@RunAsClient
public abstract class KeycloakContainersManager {

    @ArquillianResource
    protected ContainerController controller;

    private static final Logger log = Logger.getLogger(KeycloakContainersManager.class.getName());

    private final String authServerQualifier;
    private final String appServerQualifier;

    public KeycloakContainersManager() {
        this.authServerQualifier = getAuthServerQualifier(this.getClass());
        this.appServerQualifier = getAppServerQualifier(this.getClass());
        if (authServerQualifier == null) {
            throw new IllegalStateException("Couldn't determine container qualifier for keycloak server.");
        }
        System.out.println("Test: " + this.getClass().getCanonicalName());
        System.out.println("Auth server: " + authServerQualifier);
        System.out.println("App server:  " + appServerQualifier);
        if (appServerQualifier == null || appServerQualifier.equals(authServerQualifier)) {
            System.out.println("App server == Auth server");
        }
    }

    public static String getAuthServerQualifier(Class clazz) {
        Class<? extends KeycloakContainersManager> annotatedClass = getNearestSuperclassWithAnnotation(clazz, AuthServerContainer.class);

        return annotatedClass == null ? null
                : annotatedClass.getAnnotation(AuthServerContainer.class).value();
    }

    public static String getAppServerQualifier(Class clazz) {
        Class<? extends KeycloakContainersManager> annotatedClass = getNearestSuperclassWithAnnotation(clazz, AppServerContainer.class);
        return annotatedClass == null ? null
                : annotatedClass.getAnnotation(AppServerContainer.class).value();
    }

    public static Class<? extends KeycloakContainersManager>
            getNearestSuperclassWithAnnotation(Class clazz, Class annotationClass) {
        return clazz.isAnnotationPresent(annotationClass) ? clazz
                : (clazz.equals(Object.class) || clazz.equals(KeycloakContainersManager.class) ? null // stop recursion
                        : getNearestSuperclassWithAnnotation(clazz.getSuperclass(), annotationClass)); // continue recursion
    }

    @Before
    public void startContainers() {
        System.out.println("Starting Auth server: " + authServerQualifier);
        controller.start(authServerQualifier);
        if (!isRelative()) {
            System.out.println("Starting App server: " + appServerQualifier);
            controller.start(appServerQualifier);
        }
    }

    public boolean isRelative() {
        return appServerQualifier == null
                || appServerQualifier.equals(authServerQualifier);
    }

}
