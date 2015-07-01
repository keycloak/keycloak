package org.keycloak.testsuite.page.console;

import javax.ws.rs.core.UriBuilder;

/**
 *
 * @author tkyjovsk
 */
public class Realms extends AdminConsole {

    @Override
    public UriBuilder createUriBuilder() {
        return super.createUriBuilder().path("#/realms");
    }
    
}
