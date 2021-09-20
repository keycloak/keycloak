package org.keycloak.credential;

import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.RecoveryAuthnCodesCredentialModel;
import org.keycloak.models.utils.RecoveryAuthnCodesUtils;

import java.util.Optional;


public class RecoveryAuthnCodesCredentialProvider implements CredentialProvider<RecoveryAuthnCodesCredentialModel>, CredentialInputValidator {

    private static final Logger logger = Logger.getLogger(RecoveryAuthnCodesCredentialProvider.class);

    private final KeycloakSession session;

    public RecoveryAuthnCodesCredentialProvider(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public String getType() {
        return RecoveryAuthnCodesCredentialModel.TYPE;
    }

    @Override
    public CredentialModel createCredential(RealmModel realm,
                                            UserModel user,
                                            RecoveryAuthnCodesCredentialModel credentialModel) {

        session.userCredentialManager()
               .getStoredCredentialsByTypeStream(realm, user, getType())
               .findFirst()
               .ifPresent(model -> deleteCredential(realm, user, model.getId()));

        return session.userCredentialManager().createCredential(realm, user, credentialModel);
    }

    @Override
    public boolean deleteCredential(RealmModel realm, UserModel user, String credentialId) {
        return session.userCredentialManager().removeStoredCredential(realm, user, credentialId);
    }

    @Override
    public RecoveryAuthnCodesCredentialModel getCredentialFromModel(CredentialModel model) {
        return RecoveryAuthnCodesCredentialModel.createFromCredentialModel(model);
    }

    @Override
    public CredentialTypeMetadata getCredentialTypeMetadata(CredentialTypeMetadataContext metadataContext) {
        CredentialTypeMetadata.CredentialTypeMetadataBuilder builder = CredentialTypeMetadata.builder()
                .type(getType())
                .category(CredentialTypeMetadata.Category.TWO_FACTOR)
                .displayName("recovery-authn-codes-display-name")
                .helpText("recovery-authn-codes-help-text")
                .iconCssClass("kcAuthenticatorRecoveryAuthnCodesClass")
                .removeable(true);

        UserModel user = metadataContext.getUser();

        if (user != null && !isConfiguredFor(session.getContext().getRealm(), user, getType())) {
            builder.createAction(UserModel.RequiredAction.CONFIGURE_RECOVERY_AUTHN_CODES.name());
        }

        return builder.build(session);
    }

    @Override
    public boolean supportsCredentialType(String credentialType) {
        return getType().equals(credentialType);
    }

    @Override
    public boolean isConfiguredFor(RealmModel realm, UserModel user, String credentialType) {
        return session.userCredentialManager()
                      .getStoredCredentialsByTypeStream(realm, user, credentialType)
                      .findAny()
                      .isPresent();
    }

    @Override
    public boolean isValid(RealmModel realm, UserModel user, CredentialInput credentialInput) {
        boolean isValidResult = false;
        String rawInputRecoveryAuthnCode;
        Optional<CredentialModel> credential;
        RecoveryAuthnCodesCredentialModel backupCodeCredentialModel;
        String hashedSavedBackupCode;

        // TODO: Copied from elsewhere, is this even possible?
        if (!(credentialInput instanceof UserCredentialModel)) {

            logger.debug("Expected instance of UserCredentialModel");

        } else {

            rawInputRecoveryAuthnCode = credentialInput.getChallengeResponse();

            credential = session.userCredentialManager()
                                .getStoredCredentialsByTypeStream(realm, user, getType())
                                .findFirst();

            if (credential.isPresent()) {

                backupCodeCredentialModel = RecoveryAuthnCodesCredentialModel.createFromCredentialModel(credential.get());

                if (!backupCodeCredentialModel.allCodesUsed()) {

                    hashedSavedBackupCode = backupCodeCredentialModel.getNextRecoveryAuthnCode().getEncodedHashedValue();

                    if (RecoveryAuthnCodesUtils.verifyRecoveryCodeInput(rawInputRecoveryAuthnCode, hashedSavedBackupCode)) {

                        backupCodeCredentialModel.removeRecoveryAuthnCode();
                        session.userCredentialManager().updateCredential(realm, user, backupCodeCredentialModel);
                        isValidResult = true;

                    }
                }
            }
        }

        return isValidResult;
    }

}
