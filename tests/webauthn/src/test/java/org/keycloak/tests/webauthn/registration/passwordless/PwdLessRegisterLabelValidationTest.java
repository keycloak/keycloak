package org.keycloak.tests.webauthn.registration.passwordless;

import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.tests.webauthn.WebAuthnRegisterLabelValidationTest;

/**
 * Passwordless variant of {@link WebAuthnRegisterLabelValidationTest}.
 */
@KeycloakIntegrationTest
public class PwdLessRegisterLabelValidationTest extends WebAuthnRegisterLabelValidationTest {

    @Override
    public boolean isPasswordless() {
        return true;
    }

    @Override
    protected void switchExecutionInBrowserFormToPasswordless() {
        // Registration succeeds into a login; use a flow that does not also require a two-factor passkey.
        managedRealm.updateWithCleanup(r -> r.browserFlow("passwordless-only"));
    }
}
