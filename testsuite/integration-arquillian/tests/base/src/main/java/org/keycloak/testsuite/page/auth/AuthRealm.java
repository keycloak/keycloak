package org.keycloak.testsuite.page.auth;

import javax.ws.rs.core.UriBuilder;
import static org.keycloak.testsuite.console.page.Realm.MASTER;

/**
 *
 * @author tkyjovsk
 */
public class AuthRealm extends AuthServer {

    public static final String AUTH_REALM = "loginRealm";

    public AuthRealm() {
        setUriParameter(AUTH_REALM, MASTER);
    }

    @Override
    public UriBuilder createUriBuilder() {
        return super.createUriBuilder()
                .path("realms/{" + AUTH_REALM + "}");
    }

    public void setLoginRealm(String loginRealm) {
        setUriParameter(AUTH_REALM, loginRealm);
    }

}
