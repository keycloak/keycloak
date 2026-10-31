package org.keycloak.authentication.requiredactions;

import org.keycloak.models.Constants;
import org.keycloak.models.WebAuthnPolicy;

import org.jboss.logging.Logger;

import static org.keycloak.services.messages.Messages.WEBAUTHN_ERROR_BACKUP_ELIGIBLE_CHANGED;
import static org.keycloak.services.messages.Messages.WEBAUTHN_ERROR_BACKUP_ELIGIBLE_NOT_ALLOWED;
import static org.keycloak.services.messages.Messages.WEBAUTHN_ERROR_BACKUP_ELIGIBLE_REQUIRED;

/**
 * Enforces the {@link WebAuthnPolicy#getBackupEligibility() backup eligibility} policy option against the
 * Backup Eligible (BE) flag of the authenticator data.
 * <p>
 * The BE flag is set by the authenticator when the credential private key may be backed up and synced to other
 * devices, for example a passkey stored in a cloud credential manager. Authenticators that keep the key on the
 * device, such as security keys or a platform authenticator without sync, report the flag as unset. Per the WebAuthn
 * specification the flag is fixed for the lifetime of a credential, so it is checked at registration and again on
 * every assertion. The latter also covers credentials registered before the policy was configured.
 */
public final class WebAuthnBackupEligibilityVerifier {

    private static final Logger logger = Logger.getLogger(WebAuthnBackupEligibilityVerifier.class);

    private WebAuthnBackupEligibilityVerifier() {
    }

    /**
     * @return {@code true} when the policy restricts the backup eligibility of credentials
     */
    public static boolean isConfigured(WebAuthnPolicy policy) {
        String requirement = policy.getBackupEligibility();
        return Constants.WEBAUTHN_POLICY_OPTION_DEVICE_BOUND.equals(requirement)
                || Constants.WEBAUTHN_POLICY_OPTION_BACKUP_ELIGIBLE.equals(requirement);
    }

    /**
     * The BE flag is fixed for the lifetime of a credential. An assertion reporting a different value than the one
     * recorded for the credential indicates a cloned, tampered or broken authenticator. When a backup eligibility
     * policy is configured the assertion is rejected, as otherwise a credential known to be backup eligible could
     * pass a device-bound policy by relabeling itself. Without a policy the change is only logged, so realms that
     * did not opt into the feature see no new login failures; the recorded flag is kept either way.
     *
     * @param storedBackupEligible the BE flag recorded for the credential, {@code null} when unknown for
     *                             credentials registered before the flag was recorded
     * @param backupEligible the BE flag from the authenticator data of the assertion
     * @return {@code true} when the asserted flag differs from the recorded one
     * @throws WebAuthnPolicyException when the flag changed and a backup eligibility policy is configured
     */
    public static boolean verifyUnchanged(Boolean storedBackupEligible, boolean backupEligible, String credentialId, WebAuthnPolicy policy) {
        if (storedBackupEligible == null || storedBackupEligible == backupEligible) {
            return false;
        }
        if (isConfigured(policy)) {
            logger.warnf("Backup eligible flag of WebAuthn credential %s changed from %s to %s, which the WebAuthn specification does not allow. Rejecting the assertion.",
                    credentialId, storedBackupEligible, backupEligible);
            throw new WebAuthnPolicyException(WEBAUTHN_ERROR_BACKUP_ELIGIBLE_CHANGED,
                    "Backup eligible flag of credential " + credentialId + " changed from " + storedBackupEligible + " to " + backupEligible);
        }
        logger.warnf("Backup eligible flag of WebAuthn credential %s changed from %s to %s, which the WebAuthn specification does not allow. Keeping the recorded flag.",
                credentialId, storedBackupEligible, backupEligible);
        return true;
    }

    /**
     * @param backupEligible the BE flag from the authenticator data
     * @throws WebAuthnPolicyException when the flag does not satisfy the policy
     */
    public static void verify(boolean backupEligible, WebAuthnPolicy policy) {
        String requirement = policy.getBackupEligibility();
        if (Constants.WEBAUTHN_POLICY_OPTION_DEVICE_BOUND.equals(requirement) && backupEligible) {
            logger.debug("Rejected backup eligible credential: policy requires device-bound credentials");
            throw new WebAuthnPolicyException(WEBAUTHN_ERROR_BACKUP_ELIGIBLE_NOT_ALLOWED,
                    "Policy requires device-bound credentials but the credential is backup eligible (synced)");
        }
        if (Constants.WEBAUTHN_POLICY_OPTION_BACKUP_ELIGIBLE.equals(requirement) && !backupEligible) {
            logger.debug("Rejected device-bound credential: policy requires backup eligible credentials");
            throw new WebAuthnPolicyException(WEBAUTHN_ERROR_BACKUP_ELIGIBLE_REQUIRED,
                    "Policy requires backup eligible (synced) credentials but the credential is device-bound");
        }
    }
}
