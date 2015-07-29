package org.keycloak.testsuite.page.auth;

import java.net.URI;
import javax.ws.rs.core.UriBuilder;
import org.keycloak.protocol.oidc.OIDCLoginProtocolService;

/**
 *
 * @author tkyjovsk
 */
public class AuthRealm extends AuthServer {

    public static final String AUTH_REALM = "authRealm";

    public static final String MASTER = "master";
    public static final String DEMO = "demo";
    public static final String TEST = "test";

    public AuthRealm() {
        setUriParameter(AUTH_REALM, MASTER);
    }

    @Override
    public UriBuilder createUriBuilder() {
        return super.createUriBuilder()
                .path("realms/{" + AUTH_REALM + "}");
    }

    public void setAuthRealm(String loginRealm) {
        setUriParameter(AUTH_REALM, loginRealm);
    }

    public String getAuthRealm() {
        return (String) getUriParameter(AUTH_REALM);
    }

    public String getAuthRoot() {
        URI uri = buildUri();
        return uri.getScheme() + "://" + uri.getAuthority() + "/auth";
    }

    public URI getOIDCLoginUrl() {
        return OIDCLoginProtocolService.authUrl(UriBuilder.fromPath(getAuthRoot()))
                .build(getAuthRealm());
    }

}
