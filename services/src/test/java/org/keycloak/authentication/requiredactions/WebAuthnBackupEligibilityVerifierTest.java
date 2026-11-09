package org.keycloak.authentication.requiredactions;

import org.keycloak.models.Constants;
import org.keycloak.models.WebAuthnPolicy;

import org.junit.jupiter.api.Test;

import static org.keycloak.services.messages.Messages.WEBAUTHN_ERROR_BACKUP_ELIGIBLE_CHANGED;
import static org.keycloak.services.messages.Messages.WEBAUTHN_ERROR_BACKUP_ELIGIBLE_NOT_ALLOWED;
import static org.keycloak.services.messages.Messages.WEBAUTHN_ERROR_BACKUP_ELIGIBLE_REQUIRED;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class WebAuthnBackupEligibilityVerifierTest {

    @Test
    public void notConfiguredAcceptsBoth() {
        for (String value : new String[] { null, "", Constants.DEFAULT_WEBAUTHN_POLICY_NOT_SPECIFIED }) {
            WebAuthnPolicy policy = policy(value);
            assertFalse(WebAuthnBackupEligibilityVerifier.isConfigured(policy));
            assertDoesNotThrow(() -> WebAuthnBackupEligibilityVerifier.verify(true, policy));
            assertDoesNotThrow(() -> WebAuthnBackupEligibilityVerifier.verify(false, policy));
        }
    }

    @Test
    public void deviceBoundRejectsBackupEligibleCredentials() {
        WebAuthnPolicy policy = policy(Constants.WEBAUTHN_POLICY_OPTION_DEVICE_BOUND);
        assertTrue(WebAuthnBackupEligibilityVerifier.isConfigured(policy));
        assertDoesNotThrow(() -> WebAuthnBackupEligibilityVerifier.verify(false, policy));

        WebAuthnPolicyException e = assertThrows(WebAuthnPolicyException.class, () -> WebAuthnBackupEligibilityVerifier.verify(true, policy));
        assertEquals(WEBAUTHN_ERROR_BACKUP_ELIGIBLE_NOT_ALLOWED, e.getMessageKey());
    }

    @Test
    public void backupEligibleRejectsDeviceBoundCredentials() {
        WebAuthnPolicy policy = policy(Constants.WEBAUTHN_POLICY_OPTION_BACKUP_ELIGIBLE);
        assertTrue(WebAuthnBackupEligibilityVerifier.isConfigured(policy));
        assertDoesNotThrow(() -> WebAuthnBackupEligibilityVerifier.verify(true, policy));

        WebAuthnPolicyException e = assertThrows(WebAuthnPolicyException.class, () -> WebAuthnBackupEligibilityVerifier.verify(false, policy));
        assertEquals(WEBAUTHN_ERROR_BACKUP_ELIGIBLE_REQUIRED, e.getMessageKey());
    }

    @Test
    public void changedBackupEligibleFlagIsRejectedWhenPolicyIsConfigured() {
        for (String value : new String[] { Constants.WEBAUTHN_POLICY_OPTION_DEVICE_BOUND, Constants.WEBAUTHN_POLICY_OPTION_BACKUP_ELIGIBLE }) {
            WebAuthnPolicy policy = policy(value);
            // unknown stored value (credential registered before the flag was recorded) accepts either
            assertFalse(WebAuthnBackupEligibilityVerifier.verifyUnchanged(null, true, "cred", policy));
            assertFalse(WebAuthnBackupEligibilityVerifier.verifyUnchanged(null, false, "cred", policy));
            assertFalse(WebAuthnBackupEligibilityVerifier.verifyUnchanged(true, true, "cred", policy));
            assertFalse(WebAuthnBackupEligibilityVerifier.verifyUnchanged(false, false, "cred", policy));

            WebAuthnPolicyException e = assertThrows(WebAuthnPolicyException.class, () -> WebAuthnBackupEligibilityVerifier.verifyUnchanged(true, false, "cred", policy));
            assertEquals(WEBAUTHN_ERROR_BACKUP_ELIGIBLE_CHANGED, e.getMessageKey());
            e = assertThrows(WebAuthnPolicyException.class, () -> WebAuthnBackupEligibilityVerifier.verifyUnchanged(false, true, "cred", policy));
            assertEquals(WEBAUTHN_ERROR_BACKUP_ELIGIBLE_CHANGED, e.getMessageKey());
        }
    }

    @Test
    public void changedBackupEligibleFlagIsOnlyReportedWithoutPolicy() {
        WebAuthnPolicy policy = policy(Constants.DEFAULT_WEBAUTHN_POLICY_NOT_SPECIFIED);
        assertFalse(WebAuthnBackupEligibilityVerifier.verifyUnchanged(true, true, "cred", policy));
        assertTrue(WebAuthnBackupEligibilityVerifier.verifyUnchanged(true, false, "cred", policy));
        assertTrue(WebAuthnBackupEligibilityVerifier.verifyUnchanged(false, true, "cred", policy));
    }

    private static WebAuthnPolicy policy(String backupEligibility) {
        WebAuthnPolicy policy = new WebAuthnPolicy();
        policy.setBackupEligibility(backupEligibility);
        return policy;
    }
}
