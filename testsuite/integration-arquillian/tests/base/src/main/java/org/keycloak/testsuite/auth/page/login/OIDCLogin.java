package org.keycloak.testsuite.auth.page.login;

/**
 *
 * @author tkyjovsk
 */
public class OIDCLogin extends Login {

    public static final String OIDC = "openid-connect";

    public OIDCLogin() {
        setProtocol(OIDC);
    }

}
