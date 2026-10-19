package org.keycloak.tests.webauthn.authenticators;

import java.util.HashMap;
import java.util.Map;

import org.openqa.selenium.virtualauthenticator.VirtualAuthenticatorOptions;

/**
 * Virtual authenticator options which additionally set the default Backup Eligible (BE) and Backup State (BS)
 * flags of credentials created by the authenticator. Selenium does not expose these options, but chromedriver
 * forwards them to the virtual authenticator, so a synced passkey can be simulated.
 */
public class BackupFlagsVirtualAuthenticatorOptions extends VirtualAuthenticatorOptions {

    public static final String DEFAULT_BACKUP_ELIGIBILITY = "defaultBackupEligibility";
    public static final String DEFAULT_BACKUP_STATE = "defaultBackupState";

    private final boolean backupEligibility;
    private final boolean backupState;

    public BackupFlagsVirtualAuthenticatorOptions(boolean backupEligibility, boolean backupState) {
        this.backupEligibility = backupEligibility;
        this.backupState = backupState;
    }

    /**
     * @return options for a platform authenticator whose credentials are backup eligible and backed up, like a
     * passkey synced through a cloud credential manager
     */
    public static BackupFlagsVirtualAuthenticatorOptions syncedPasskey() {
        BackupFlagsVirtualAuthenticatorOptions options = new BackupFlagsVirtualAuthenticatorOptions(true, true);
        options.setTransport(Transport.INTERNAL)
                .setHasResidentKey(true)
                .setHasUserVerification(true)
                .setIsUserVerified(true)
                .setIsUserConsenting(true);
        return options;
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>(super.toMap());
        map.put(DEFAULT_BACKUP_ELIGIBILITY, backupEligibility);
        map.put(DEFAULT_BACKUP_STATE, backupState);
        return map;
    }
}
