package org.keycloak.testsuite.console.page.events;

/**
 *
 * @author tkyjovsk
 */
public class Config extends Events {

    @Override
    public String getFragment() {
        return super.getFragment() + "/events-settings";
    }

}
