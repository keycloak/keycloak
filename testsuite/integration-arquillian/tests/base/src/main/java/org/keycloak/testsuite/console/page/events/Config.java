package org.keycloak.testsuite.console.page.events;

/**
 *
 * @author tkyjovsk
 */
public class Config extends Events {

    @Override
    public String getUriFragment() {
        return super.getUriFragment() + "/events-settings";
    }

}
