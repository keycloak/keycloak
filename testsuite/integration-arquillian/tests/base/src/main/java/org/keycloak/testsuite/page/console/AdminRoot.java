package org.keycloak.testsuite.page.console;

import javax.ws.rs.core.UriBuilder;

/**
 *
 * @author tkyjovsk
 */
public class AdminRoot extends AuthServer {

    @Override
    public UriBuilder createUriBuilder() {
        return super.createUriBuilder().path("admin");
    }
    
}
