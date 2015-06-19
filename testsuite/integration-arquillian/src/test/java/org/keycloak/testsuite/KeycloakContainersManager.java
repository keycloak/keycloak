package org.keycloak.testsuite;

import org.keycloak.testsuite.arquillian.AuthServerContainer;
import org.keycloak.testsuite.arquillian.AppServerContainer;
import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.junit.Before;
import org.junit.runner.RunWith;

/**
 *
 * @author tkyjovsk
 */
@RunWith(Arquillian.class)
public abstract class KeycloakContainersManager {
    
    @ArquillianResource
    protected ContainerController controller;
    
    private final String authServerQualifier;
    private final String appServerQualifier;
    
    public KeycloakContainersManager() {
        this.authServerQualifier = getAuthServerQualifier(this.getClass());
        this.appServerQualifier = getAppServerQualifier(this.getClass());
        System.out.println("Containers for: " + this.getClass().getSimpleName());
        System.out.println("Auth server: " + authServerQualifier);
        System.out.println("App server:  " + appServerQualifier);
        if (appServerQualifier.equals(authServerQualifier)) {
            System.out.println("App server == Auth server");
        }
    }

    public static String getAuthServerQualifier(Class clazz) {
        Class<? extends KeycloakContainersManager> annotatedClass = getNearestSuperclassWithAnnotation(clazz, AuthServerContainer.class);
        if (annotatedClass == null) {
            throw new IllegalStateException("Couldn't find @AuthServerContainer on the test class or any of its superclasses.");
        }
        String authServerQualifier = annotatedClass.getAnnotation(AuthServerContainer.class).value();
        if (authServerQualifier == null || authServerQualifier.isEmpty()) {
            throw new IllegalStateException("Null or empty qualifier for keycloak auth server.");
            // TODO add fallback mechanism when embedded undertow is ready
        }
        return authServerQualifier;
    }
    
    public static String getAppServerQualifier(Class clazz) {
        Class<? extends KeycloakContainersManager> annotatedClass = getNearestSuperclassWithAnnotation(clazz, AppServerContainer.class);
        
        String appServerQualifier = (annotatedClass == null ? null
                : annotatedClass.getAnnotation(AppServerContainer.class).value());
        
        return appServerQualifier == null || appServerQualifier.isEmpty()
                ? getAuthServerQualifier(clazz) // app server == auth server
                : appServerQualifier;
    }
    
    public static boolean isRelative(Class clazz) {
        return getAppServerQualifier(clazz).equals(getAuthServerQualifier(clazz));
    }
    
    public static Class<? extends KeycloakContainersManager>
            getNearestSuperclassWithAnnotation(Class clazz, Class annotationClass) {
        return clazz.isAnnotationPresent(annotationClass) ? clazz
                : (clazz.equals(Object.class) || clazz.equals(KeycloakContainersManager.class) ? null // stop recursion
                        : getNearestSuperclassWithAnnotation(clazz.getSuperclass(), annotationClass)); // continue recursion
    }
    
    public boolean isRelative() {
        return appServerQualifier.equals(authServerQualifier);
    }
    
    @Before
    public void startContainers() {
        if (!controller.isStarted(authServerQualifier)) {
            System.out.println("Starting Auth server: " + authServerQualifier);
            controller.start(authServerQualifier);
        }
        if (!controller.isStarted(appServerQualifier)) {
            System.out.println("Starting App server: " + appServerQualifier);
            controller.start(appServerQualifier);
        }
    }
    
}
