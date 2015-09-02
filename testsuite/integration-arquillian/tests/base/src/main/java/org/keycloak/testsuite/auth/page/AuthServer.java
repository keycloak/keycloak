package org.keycloak.testsuite.auth.page;

import java.net.URI;
import javax.ws.rs.core.UriBuilder;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.keycloak.admin.client.Keycloak;

/**
 * Context path of Keycloak auth server.
 * 
 * URL: http://localhost:${auth.server.http.port}/auth
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

    @ArquillianResource
    protected Keycloak keycloak;

    public Keycloak keycloak() {
        return keycloak;
    }

}
