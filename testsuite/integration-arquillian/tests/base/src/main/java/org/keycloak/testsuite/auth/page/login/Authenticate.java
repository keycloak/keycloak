package org.keycloak.testsuite.auth.page.login;

import javax.ws.rs.core.UriBuilder;
import org.jboss.arquillian.graphene.page.Page;

/**
 *
 * @author tkyjovsk
 */
public abstract class Authenticate extends LoginActions {

    @Override
    public UriBuilder createUriBuilder() {
        return super.createUriBuilder().path("authenticate");
    }

    @Page
    private LoginForm login;

    public LoginForm loginForm() {
        return login;
    }

}
