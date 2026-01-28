package org.keycloak.testsuite.auth.page.login;

/**
 * @author mhajas
 */
public class SAMLPostLogin extends Login {
    SAMLPostLogin() {
        setProtocol(LOGIN_ACTION);
    }
}
