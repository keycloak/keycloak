package org.keycloak.credential;

import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.BackupCodeCredentialModel;

import java.util.Optional;


public class BackupCodeCredentialProvider implements CredentialProvider<BackupCodeCredentialModel>, CredentialInputValidator {

    private static final Logger logger = Logger.getLogger(BackupCodeCredentialProvider.class);

    private final KeycloakSession session;

    BackupCodeCredentialProvider(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public String getType() {
        return BackupCodeCredentialModel.TYPE;
    }

    @Override
    public CredentialModel createCredential(RealmModel realm, UserModel user, BackupCodeCredentialModel credentialModel) {
        // TODO: Should we handle the case where BackupCodes already exist here? Only possible through direct HTTP calls.
        return session.userCredentialManager().createCredential(realm, user, credentialModel);
    }

    @Override
    public boolean deleteCredential(RealmModel realm, UserModel user, String credentialId) {
        return session.userCredentialManager().removeStoredCredential(realm, user, credentialId);
    }

    @Override
    public BackupCodeCredentialModel getCredentialFromModel(CredentialModel model) {
        return BackupCodeCredentialModel.createFromCredentialModel(model);
    }

    @Override
    public CredentialTypeMetadata getCredentialTypeMetadata(CredentialTypeMetadataContext metadataContext) {
        CredentialTypeMetadata.CredentialTypeMetadataBuilder builder = CredentialTypeMetadata.builder()
                .type(getType())
                .category(CredentialTypeMetadata.Category.TWO_FACTOR)
                .displayName("backup-codes-display-name")
                .helpText("backup-codes-help-text")
                .iconCssClass("kcAuthenticatorBackupCodeClass")
                .removeable(true);

        UserModel user = metadataContext.getUser();

        if (user != null && !isConfiguredFor(session.getContext().getRealm(), user, getType())) {
            builder.createAction(UserModel.RequiredAction.CONFIGURE_BACKUP_CODES.name());
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
        // TODO: Copied from elsewhere, is this even possible?
        if (!(credentialInput instanceof UserCredentialModel)) {
            logger.debug("Expected instance of UserCredentialModel");
            return false;
        }

        String response = credentialInput.getChallengeResponse();

        Optional<CredentialModel> credential = session.userCredentialManager()
                .getStoredCredentialsByTypeStream(realm, user, getType())
                .findFirst();

        if (!credential.isPresent()) {
            return false;
        }

        BackupCodeCredentialModel backupCodeCredentialModel = BackupCodeCredentialModel.createFromCredentialModel(credential.get());

        if (backupCodeCredentialModel.allCodesUsed()) {
            return false;
        }

        if (backupCodeCredentialModel.getNextBackupCode().getValue().equals(response)) {
            backupCodeCredentialModel.removeBackupCode();
            session.userCredentialManager().updateCredential(realm, user, backupCodeCredentialModel);
            return true;
        }

        return false;
    }

}
