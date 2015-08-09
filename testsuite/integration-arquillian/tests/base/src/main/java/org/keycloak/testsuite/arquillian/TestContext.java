package org.keycloak.testsuite.arquillian;

import java.net.URL;

/**
 *
 * @author tkyjovsk
 */
public final class TestContext {

    private URL authServerContextRoot;
    private URL appServerContextRoot;

    private boolean adminLoggedIn;

    public TestContext() {
        this.adminLoggedIn = false;
    }

    public TestContext(URL authServerContextRoot, URL appServerContextRoot) {
        this();
        this.authServerContextRoot = authServerContextRoot;
        this.appServerContextRoot = appServerContextRoot;
    }

    public URL getAuthServerContextRoot() {
        return authServerContextRoot;
    }

    public URL getAppServerContextRoot() {
        return appServerContextRoot;
    }

    public boolean isAdminLoggedIn() {
        return adminLoggedIn;
    }

    public void setAdminLoggedIn(boolean adminLoggedIn) {
        this.adminLoggedIn = adminLoggedIn;
    }

    public void setAuthServerContextRoot(URL authServerContextRoot) {
        this.authServerContextRoot = authServerContextRoot;
    }

    public void setAppServerContextRoot(URL appServerContextRoot) {
        this.appServerContextRoot = appServerContextRoot;
    }

}
