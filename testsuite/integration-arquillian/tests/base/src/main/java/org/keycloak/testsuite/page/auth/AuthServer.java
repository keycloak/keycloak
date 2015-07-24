package org.keycloak.testsuite.page.auth;

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
