package org.keycloak.tests.webauthn.passwordless;

import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.tests.webauthn.WebAuthnBackupEligibilityTest;

/**
 * Runs the backup eligibility tests against the WebAuthn Passwordless policy and credential type.
 */
@KeycloakIntegrationTest
public class PwdLessBackupEligibilityTest extends WebAuthnBackupEligibilityTest {

    @Override
    public boolean isPasswordless() {
        return true;
    }

    @Override
    protected void switchExecutionInBrowserFormToPasswordless() {
        // register a passwordless-only credential, so use a login flow that goes straight from the password form to passwordless webauthn
        managedRealm.updateWithCleanup(r -> r.browserFlow("passwordless-only"));
    }
}
