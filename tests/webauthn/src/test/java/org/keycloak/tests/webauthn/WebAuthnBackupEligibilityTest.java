package org.keycloak.tests.webauthn;

import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.keycloak.WebAuthnConstants;
import org.keycloak.events.EventType;
import org.keycloak.models.Constants;
import org.keycloak.models.credential.WebAuthnCredentialModel;
import org.keycloak.models.credential.dto.WebAuthnCredentialData;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.WebAuthnRealmData;
import org.keycloak.tests.utils.admin.AdminApiUtil;
import org.keycloak.tests.webauthn.authenticators.BackupFlagsVirtualAuthenticatorOptions;
import org.keycloak.util.JsonSerialization;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.virtualauthenticator.Credential;
import org.openqa.selenium.virtualauthenticator.VirtualAuthenticator;
import org.openqa.selenium.virtualauthenticator.VirtualAuthenticatorOptions;

import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Tests the <em>Backup eligibility</em> WebAuthn policy option, which distinguishes device-bound passkeys from
 * passkeys synced through a credential manager using the Backup Eligible (BE) flag of the authenticator data.
 */
@KeycloakIntegrationTest
public class WebAuthnBackupEligibilityTest extends AbstractWebAuthnVirtualTest {

    private static final String DEVICE_BOUND_ERROR = "Your organization requires device-bound passkeys.";
    private static final String SYNCED_ERROR = "Your organization requires synced passkeys.";

    @Test
    public void backupEligibilityIsNotSpecifiedByDefault() {
        WebAuthnRealmData realmData = new WebAuthnRealmData(managedRealm.admin().toRepresentation(), isPasswordless());
        assertThat(realmData.getBackupEligibility(), anyOf(nullValue(), is(Constants.DEFAULT_WEBAUTHN_POLICY_NOT_SPECIFIED)));
    }

    @Test
    public void backupFlagsAreRecordedForSyncedPasskey() throws IOException {
        useAuthenticator(BackupFlagsVirtualAuthenticatorOptions.syncedPasskey());

        registerDefaultUser();
        Assertions.assertTrue(oAuthClient.parseLoginResponse().isSuccess());
        String userId = AdminApiUtil.findUserByUsername(managedRealm.admin(), USERNAME).getId();

        EventRepresentation registration = pollEvent(EventType.UPDATE_CREDENTIAL);
        assertThat(registration.getDetails().get(WebAuthnConstants.PUBKEY_CRED_BACKUP_ELIGIBLE_ATTR), is("true"));
        assertThat(registration.getDetails().get(WebAuthnConstants.PUBKEY_CRED_BACKUP_STATE_ATTR), is("true"));

        WebAuthnCredentialData data = webAuthnCredentialData(userId);
        assertThat(data.getBackupEligible(), is(true));
        assertThat(data.getBackupState(), is(true));

        logout();
        events.clear();

        // the flags are reported with every assertion and recorded on the login event
        authenticateUser(USERNAME, PASSWORD, true);
        EventRepresentation login = pollEvent(EventType.LOGIN);
        assertThat(login.getDetails().get(WebAuthnConstants.PUBKEY_CRED_BACKUP_ELIGIBLE_ATTR), is("true"));
        assertThat(login.getDetails().get(WebAuthnConstants.PUBKEY_CRED_BACKUP_STATE_ATTR), is("true"));
        logout();

        // the backup state may change over the lifetime of a credential and is refreshed on the next login
        setBackupState(false);
        authenticateUser(USERNAME, PASSWORD, true);
        data = webAuthnCredentialData(userId);
        assertThat(data.getBackupEligible(), is(true));
        assertThat(data.getBackupState(), is(false));
    }

    @Test
    public void backupFlagsAreRecordedForDeviceBoundPasskey() throws IOException {
        registerDefaultUser();
        Assertions.assertTrue(oAuthClient.parseLoginResponse().isSuccess());
        String userId = AdminApiUtil.findUserByUsername(managedRealm.admin(), USERNAME).getId();

        WebAuthnCredentialData data = webAuthnCredentialData(userId);
        assertThat(data.getBackupEligible(), is(false));
        assertThat(data.getBackupState(), is(false));

        EventRepresentation registration = pollEvent(EventType.UPDATE_CREDENTIAL);
        assertThat(registration.getDetails().get(WebAuthnConstants.PUBKEY_CRED_BACKUP_ELIGIBLE_ATTR), is("false"));
    }

    @Test
    public void deviceBoundPolicyRejectsSyncedPasskeyAtRegistration() {
        managedRealm.updateWithCleanup(r -> r.webAuthn(isPasswordless(), builder ->
                builder.backupEligibility(Constants.WEBAUTHN_POLICY_OPTION_DEVICE_BOUND)));
        useAuthenticator(BackupFlagsVirtualAuthenticatorOptions.syncedPasskey());

        registerDefaultUser(false);
        if (webAuthnRegisterPage.isRegisterAlertPresent()) {
            webAuthnRegisterPage.registerWebAuthnCredential("synced");
        }
        webAuthnErrorPage.assertCurrent();
        assertThat(webAuthnErrorPage.getError(), containsString(DEVICE_BOUND_ERROR));
    }

    @Test
    public void deviceBoundPolicyAcceptsDeviceBoundPasskey() {
        managedRealm.updateWithCleanup(r -> r.webAuthn(isPasswordless(), builder ->
                builder.backupEligibility(Constants.WEBAUTHN_POLICY_OPTION_DEVICE_BOUND)));

        registerDefaultUser();
        Assertions.assertTrue(oAuthClient.parseLoginResponse().isSuccess());
        logout();
        authenticateUser(USERNAME, PASSWORD, true);
    }

    @Test
    public void syncedPolicyRejectsDeviceBoundPasskeyAtRegistration() {
        managedRealm.updateWithCleanup(r -> r.webAuthn(isPasswordless(), builder ->
                builder.backupEligibility(Constants.WEBAUTHN_POLICY_OPTION_BACKUP_ELIGIBLE)));

        registerDefaultUser(false);
        if (webAuthnRegisterPage.isRegisterAlertPresent()) {
            webAuthnRegisterPage.registerWebAuthnCredential("device-bound");
        }
        webAuthnErrorPage.assertCurrent();
        assertThat(webAuthnErrorPage.getError(), containsString(SYNCED_ERROR));
    }

    @Test
    public void syncedPolicyAcceptsSyncedPasskey() {
        managedRealm.updateWithCleanup(r -> r.webAuthn(isPasswordless(), builder ->
                builder.backupEligibility(Constants.WEBAUTHN_POLICY_OPTION_BACKUP_ELIGIBLE)));
        useAuthenticator(BackupFlagsVirtualAuthenticatorOptions.syncedPasskey());

        registerDefaultUser();
        Assertions.assertTrue(oAuthClient.parseLoginResponse().isSuccess());
        logout();
        authenticateUser(USERNAME, PASSWORD, true);
    }

    @Test
    public void deviceBoundPolicyRejectsPreviouslyRegisteredSyncedPasskeyAtLogin() {
        useAuthenticator(BackupFlagsVirtualAuthenticatorOptions.syncedPasskey());

        // registered while the policy still allowed synced passkeys
        registerDefaultUser();
        Assertions.assertTrue(oAuthClient.parseLoginResponse().isSuccess());
        logout();

        managedRealm.updateWithCleanup(r -> r.webAuthn(isPasswordless(), builder ->
                builder.backupEligibility(Constants.WEBAUTHN_POLICY_OPTION_DEVICE_BOUND)));

        authenticateUser(USERNAME, PASSWORD, false);
        webAuthnErrorPage.assertCurrent();
        assertThat(webAuthnErrorPage.getError(), containsString(DEVICE_BOUND_ERROR));

        EventRepresentation error = pollEvent(EventType.LOGIN_ERROR);
        assertThat(error.getDetails().get(WebAuthnConstants.AUTH_ERR_LABEL), is("webauthn-error-backup-eligible-not-allowed"));
    }

    @Test
    public void changedBackupEligibleFlagIsRejectedAtLogin() throws IOException {
        useAuthenticator(BackupFlagsVirtualAuthenticatorOptions.syncedPasskey());

        registerDefaultUser();
        Assertions.assertTrue(oAuthClient.parseLoginResponse().isSuccess());
        String userId = AdminApiUtil.findUserByUsername(managedRealm.admin(), USERNAME).getId();
        logout();

        // a device-bound policy would be satisfied by the forged flag, but the recorded flag says otherwise
        managedRealm.updateWithCleanup(r -> r.webAuthn(isPasswordless(), builder ->
                builder.backupEligibility(Constants.WEBAUTHN_POLICY_OPTION_DEVICE_BOUND)));
        setBackupEligibility(false);

        authenticateUser(USERNAME, PASSWORD, false);
        webAuthnErrorPage.assertCurrent();
        assertThat(webAuthnErrorPage.getError(), containsString("The passkey reported a different backup capability than when it was registered"));

        EventRepresentation error = pollEvent(EventType.LOGIN_ERROR);
        assertThat(error.getDetails().get(WebAuthnConstants.AUTH_ERR_LABEL), is("webauthn-error-backup-eligible-changed"));

        // the recorded flag is kept
        WebAuthnCredentialData data = webAuthnCredentialData(userId);
        assertThat(data.getBackupEligible(), is(true));
    }

    private void useAuthenticator(VirtualAuthenticatorOptions options) {
        getVirtualAuthManager().useAuthenticator(options);
    }

    /**
     * Flips the Backup State flag of the registered credential through the Chrome DevTools protocol, like a
     * credential manager that backs up or stops backing up a passkey.
     */
    private void setBackupState(boolean backupState) {
        setCredentialProperties(Map.of("backupState", backupState));
    }

    /**
     * Makes the virtual authenticator report a different Backup Eligible flag, which a real authenticator must
     * never do. Simulates a cloned or tampered credential. The Backup State flag is cleared as well, since
     * authenticator data with BS set but BE unset is invalid and would be rejected before the flag comparison.
     */
    private void setBackupEligibility(boolean backupEligibility) {
        setCredentialProperties(Map.of("backupEligibility", backupEligibility, "backupState", backupEligibility));
    }

    private void setCredentialProperties(Map<String, Object> properties) {
        VirtualAuthenticator authenticator = getVirtualAuthManager().getCurrent().getAuthenticator();
        List<Credential> credentials = authenticator.getCredentials();
        assertThat(credentials.size(), is(1));
        Map<String, Object> params = new HashMap<>(properties);
        params.put("authenticatorId", authenticator.getId());
        params.put("credentialId", Base64.getEncoder().encodeToString(credentials.get(0).getId()));
        ((ChromeDriver) driver.driver()).executeCdpCommand("WebAuthn.setCredentialProperties", params);
    }

    private WebAuthnCredentialData webAuthnCredentialData(String userId) throws IOException {
        List<CredentialRepresentation> credentials = managedRealm.admin().users().get(userId).credentials();
        CredentialRepresentation credential = credentials.stream()
                .filter(c -> WebAuthnCredentialModel.TYPE_TWOFACTOR.equals(c.getType()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no webauthn credential registered"));
        return JsonSerialization.readValue(credential.getCredentialData(), WebAuthnCredentialData.class);
    }

    private EventRepresentation pollEvent(EventType type) {
        for (int i = 0; i < 10; i++) {
            EventRepresentation event = events.poll();
            if (event == null) {
                break;
            }
            if (type.name().equals(event.getType())) {
                return event;
            }
        }
        throw new AssertionError("Event of type " + type + " not found");
    }
}
