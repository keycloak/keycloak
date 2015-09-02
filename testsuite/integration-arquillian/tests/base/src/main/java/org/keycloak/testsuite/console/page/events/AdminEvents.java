package org.keycloak.testsuite.console.page.events;

/**
 *
 * @author tkyjovsk
 */
public class AdminEvents extends Events {

    @Override
    public String getUriFragment() {
        return super.getUriFragment() + "/admin-events";
    }

}
