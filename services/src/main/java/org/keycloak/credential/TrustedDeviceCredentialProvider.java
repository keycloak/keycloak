package org.keycloak.credential;

import org.keycloak.common.util.ObjectUtil;
import org.keycloak.common.util.Time;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.TrustedDeviceCredentialModel;

import org.jboss.logging.Logger;

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

    @Override
    public boolean deleteCredential(RealmModel realm, UserModel user, String credentialId) {
        return user.credentialManager().removeStoredCredentialById(credentialId);
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
            .displayName("trustedDeviceDisplayName")
            .helpText("trustedDeviceHelpText")
            .iconCssClass("kcAuthenticatorWebAuthnClass")
            .removeable(true)
            .build(session);
    }

    @Override
    public boolean supportsCredentialType(String type) {
        return type.equals(getType());
    }

    @Override
    public boolean isConfiguredFor(RealmModel realm, UserModel user, String credentialType) {
        if (!supportsCredentialType(credentialType)) return false;
        return user.credentialManager().getStoredCredentialsByTypeStream(credentialType).findAny().isPresent();
    }

    @Override
    public boolean isValid(RealmModel realm, UserModel user, CredentialInput credentialInput) {
        if (!(credentialInput instanceof UserCredentialModel)) {
            logger.debug("Expected instance of UserCredentialModel for CredentialInput");
            return false;

        }
        String challengeResponse = credentialInput.getChallengeResponse();
        if (challengeResponse == null) {
            return false;
        }
        if (ObjectUtil.isBlank(credentialInput.getCredentialId())) {
            logger.debugf("CredentialId is null when validating credential of user %s", user.getUsername());
            return false;
        }

        CredentialModel credential = user.credentialManager().getStoredCredentialById(credentialInput.getCredentialId());
        TrustedDeviceCredentialModel trustedDeviceCredentialModel = getCredentialFromModel(credential);
        return trustedDeviceCredentialModel != null
                && challengeResponse.equals(trustedDeviceCredentialModel.getDeviceId())
                && trustedDeviceCredentialModel.getExpireTime() > Time.currentTime();
    }
}
