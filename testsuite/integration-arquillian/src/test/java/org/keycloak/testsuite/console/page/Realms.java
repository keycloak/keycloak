package org.keycloak.testsuite.console.page;

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
