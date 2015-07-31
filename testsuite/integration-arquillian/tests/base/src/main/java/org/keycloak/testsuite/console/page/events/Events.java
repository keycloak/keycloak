package org.keycloak.testsuite.console.page.events;

import org.keycloak.testsuite.console.page.AdminConsoleRealm;

/**
 *
 * @author tkyjovsk
 */
public class Events extends AdminConsoleRealm {

    @Override
    public String getFragment() {
        return super.getFragment() + "/events";
    }

}
