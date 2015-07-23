package org.keycloak.testsuite.arquillian;

/**
 * ContextRootStore. Stores auth and app server URLs.
 * Produced by ContainersTestEnricher on @BeforeClass.
 * Consumed by URLProvider when injecting @ArquillianResource URLs.
 * @author tkyjovsk
 */
import java.net.URL;

public class ContextRootStore {

    private final URL authServerContextRoot;
    private final URL appServerContextRoot;

    public ContextRootStore(URL authServerContextRoot, URL appServerContextRoot) {
        this.authServerContextRoot = authServerContextRoot;
        this.appServerContextRoot = appServerContextRoot;
    }

    public URL getAuthServerContextRoot() {
        return authServerContextRoot;
    }

    public URL getAppServerContextRoot() {
        return appServerContextRoot;
    }

}
