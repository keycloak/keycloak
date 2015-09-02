package org.keycloak.testsuite.auth.page.login;

/**
 *
 * @author tkyjovsk
 */
public class OIDCLogin extends Login {

    public OIDCLogin() {
        setProtocol(OIDC);
    }

}
