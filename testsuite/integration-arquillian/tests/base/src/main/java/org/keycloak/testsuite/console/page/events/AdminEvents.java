package org.keycloak.testsuite.console.page.events;

/**
 *
 * @author tkyjovsk
 */
public class AdminEvents extends Events {

    @Override
    public String getFragment() {
        return super.getFragment() + "/admin-events";
    }

}
