package org.keycloak.testsuite.auth.page.login;

import javax.ws.rs.core.UriBuilder;

/**
 *
 * @author tkyjovsk
 */
public class PasswordReset extends LoginActions {

    @Override
    public UriBuilder createUriBuilder() {
        return super.createUriBuilder().path("password-reset");
    }

}
