package org.keycloak.testsuite.arquillian;

/**
 *
 * @author tkyjovsk
 */
public final class SuiteContext {

    private boolean adminPasswordUpdated;

    public SuiteContext() {
        this.adminPasswordUpdated = false;
    }

    public boolean isAdminPasswordUpdated() {
        return adminPasswordUpdated;
    }

    public void setAdminPasswordUpdated(boolean adminPasswordUpdated) {
        this.adminPasswordUpdated = adminPasswordUpdated;
    }

}
