package org.keycloak.testsuite.console.page;

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
