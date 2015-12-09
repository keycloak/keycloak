package org.keycloak.testsuite.auth.page.login;

/**
 * @author mhajas
 */
public class SAMLLogin extends Login {
    SAMLLogin() {
        setProtocol(SAML);
    }
}
