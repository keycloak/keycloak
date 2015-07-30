package org.keycloak.testsuite.page.auth;

import java.net.URI;
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

    public String getAuthRoot() {
        URI uri = buildUri();
        return uri.getScheme() + "://" + uri.getAuthority() + "/auth";
    }

}
