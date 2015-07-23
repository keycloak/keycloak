package org.keycloak.testsuite.page.console;

import javax.ws.rs.core.UriBuilder;

/**
 *
 * @author tkyjovsk
 */
public class AuthServer extends AuthServerContextRoot {

    @Override
    public UriBuilder createUriBuilder() {
        return super.createUriBuilder().path("auth");
    }
    
}
