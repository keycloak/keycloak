package org.keycloak.credential;

import org.apache.commons.codec.binary.Hex;
import org.jboss.logging.Logger;
import org.keycloak.common.util.ObjectUtil;
import org.keycloak.common.util.Time;
import org.keycloak.cookie.CookieProvider;
import org.keycloak.cookie.CookieType;
import org.keycloak.device.DeviceRepresentationProvider;
import org.keycloak.models.*;
import org.keycloak.models.credential.TrustedDeviceCredentialModel;
import org.keycloak.models.credential.dto.TrustedDeviceSecretData;
import org.keycloak.representations.account.DeviceRepresentation;

import java.security.SecureRandom;

public class TrustedDeviceCredentialProvider implements CredentialProvider<TrustedDeviceCredentialModel>, CredentialInputValidator {
    private static final Logger logger = Logger.getLogger(TrustedDeviceCredentialProvider.class);
    private final KeycloakSession session;

    public TrustedDeviceCredentialProvider(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public String getType() {
        return TrustedDeviceCredentialModel.TYPE;
    }

    @Override
    public CredentialModel createCredential(RealmModel realm, UserModel user, TrustedDeviceCredentialModel credentialModel) {

        if (credentialModel.getCreatedDate() == null) {
            credentialModel.setCreatedDate(Time.currentTimeMillis());
        }

        return user.credentialManager().createStoredCredential(credentialModel);
    }

    public TrustedDeviceCredentialModel createTrustedDeviceCredential(RealmModel realm, UserModel user) {

        TrustedDevicePolicy policy = realm.getTrustedDevicePolicy();

        // Should not happen, but here is a safety check before creating the new credential
        if (!policy.isEnabled()) {
            logger.warn("Skipping trusted device creation: Trusted device policy is disabled");
            return null;
        }

        DeviceRepresentation device = session.getProvider(DeviceRepresentationProvider.class).deviceRepresentation();
        SubjectCredentialManager credentialManager = user.credentialManager();

        // Clean-up expired Trusted Device credentials
        deleteExpiredCredentials(realm, credentialManager);

        // Creating new credential
        String secret = generateSecret();
        var credential = TrustedDeviceCredentialModel.create(device, secret);
        credentialManager.createStoredCredential(credential);

        // Move new credential to first place
        // This is a workaround, because the auth flows' order during the authentication process is based on the
        // order of the user's stored credentials
        credentialManager.moveStoredCredentialTo(credential.getId(), null);

        // Set cookie
        setTrustedDeviceCookie(user.getId(), credential, policy.getTrustExpiration());

        return credential;
    }

    private void setTrustedDeviceCookie(String userId, TrustedDeviceCredentialModel credential, int expiration) {
        CookieProvider cookieProvider = session.getProvider(CookieProvider.class);
        CookieType cookieType = CookieType.getTrustedDeviceCookie(userId);
        String cookie = credential.getId() + ':' + credential.getTrustedDeviceSecretData().getValue();
        cookieProvider.set(cookieType, cookie, expiration);
    }

    @Override
    public boolean deleteCredential(RealmModel realm, UserModel user, String credentialId) {
        return user.credentialManager().removeStoredCredentialById(credentialId);
    }

    public void deleteExpiredCredentials(RealmModel realm, SubjectCredentialManager credentialManager) {
        TrustedDevicePolicy policy = realm.getTrustedDevicePolicy();
        long expirationLimit = Time.currentTimeMillis() - policy.getTrustExpiration();

        credentialManager.getStoredCredentialsByTypeStream(TrustedDeviceCredentialModel.TYPE)
                .filter(c -> c.getCreatedDate() <= expirationLimit)
                .forEach(c -> credentialManager.removeStoredCredentialById(c.getId()));
    }

    @Override
    public TrustedDeviceCredentialModel getCredentialFromModel(CredentialModel model) {
        return TrustedDeviceCredentialModel.createFromCredentialModel(model);
    }

    @Override
    public CredentialTypeMetadata getCredentialTypeMetadata(CredentialTypeMetadataContext metadataContext) {
        return CredentialTypeMetadata.builder()
                .type(getType())
                .category(CredentialTypeMetadata.Category.TWO_FACTOR)
                .displayName("trusted-device-display-name")
                .helpText("trusted-device-help-text")
                .iconCssClass("kcAuthenticatorWebAuthnClass")
                .removeable(true)
                .build(session);
    }

    @Override
    public boolean supportsCredentialType(String type) {
        return type.equals(TrustedDeviceCredentialModel.TYPE);
    }

    @Override
    public boolean isConfiguredFor(RealmModel realm, UserModel user, String credentialType) {
        if (!supportsCredentialType(credentialType)) return false;
        return user.credentialManager().getStoredCredentialsByTypeStream(credentialType).findAny().isPresent();
    }

    @Override
    public boolean isValid(RealmModel realm, UserModel user, CredentialInput credentialInput) {
        if (!(credentialInput instanceof TrustedDeviceCredentialInputModel)) {
            logger.debug("Expected instance of TrustedDeviceCredentialInputModel for CredentialInput");
            return false;
        }

        if (ObjectUtil.isBlank(credentialInput.getCredentialId())) {
            logger.debugf("CredentialId is null when validating credential of user %s", user.getUsername());
            return false;
        }

        String challengeSecret = credentialInput.getChallengeResponse();
        if (challengeSecret == null || challengeSecret.isEmpty())
            return false;

        TrustedDevicePolicy trustedDevicePolicy = realm.getTrustedDevicePolicy();

        // Check whether trusted device policy is enabled
        if (!trustedDevicePolicy.isEnabled())
            return false;

        // Get user saved credential
        CredentialModel credential = user.credentialManager().getStoredCredentialById(credentialInput.getCredentialId());

        if (credential == null)
            return false;

        TrustedDeviceCredentialModel tdCredential = TrustedDeviceCredentialModel.createFromCredentialModel(credential);
        TrustedDeviceSecretData secretData = tdCredential.getTrustedDeviceSecretData();

        long credentialExpiresAt = tdCredential.getCreatedDate() + trustedDevicePolicy.getTrustExpiration();

        if (credentialExpiresAt <= Time.currentTimeMillis()) {
            // Remove the credential if expired
            deleteCredential(realm, user, credentialInput.getCredentialId());
            return false;
        }

        return challengeSecret.equals(secretData.getValue());
    }

    private String generateSecret() {
        byte[] bytes = new byte[32];
        SecureRandom secureRandom = new SecureRandom();
        secureRandom.nextBytes(bytes);
        return Hex.encodeHexString(bytes);
    }
}
