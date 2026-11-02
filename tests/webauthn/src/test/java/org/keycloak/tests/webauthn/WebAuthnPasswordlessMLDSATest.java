package org.keycloak.tests.webauthn;

import org.keycloak.testframework.annotations.KeycloakIntegrationTest;

@KeycloakIntegrationTest(config = WebAuthnMLDSATest.ServerConfig.class)
public class WebAuthnPasswordlessMLDSATest extends WebAuthnMLDSATest {

    @Override
    public boolean isPasswordless() {
        return true;
    }

    @Override
    protected void switchExecutionInBrowserFormToPasswordless() {
        managedRealm.updateWithCleanup(r -> r.browserFlow("passwordless-only"));
    }
}
