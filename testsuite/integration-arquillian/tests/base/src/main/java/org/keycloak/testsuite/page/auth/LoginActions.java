package org.keycloak.testsuite.page.auth;

import javax.ws.rs.core.UriBuilder;

/**
 *
 * @author tkyjovsk
 */
public class LoginActions extends AuthRealm {

    @Override
    public UriBuilder createUriBuilder() {
        return super.createUriBuilder()
                .path("login-actions");
    }

}
