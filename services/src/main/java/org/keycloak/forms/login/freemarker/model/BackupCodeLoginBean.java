package org.keycloak.forms.login.freemarker.model;

import org.keycloak.credential.CredentialModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.BackupCodeCredentialModel;

public class BackupCodeLoginBean {

    private final int codeNumber;

    public BackupCodeLoginBean(KeycloakSession session, RealmModel realm, UserModel user) {
        CredentialModel credentialModel = session.userCredentialManager().getStoredCredentialsByTypeStream(realm, user, BackupCodeCredentialModel.TYPE).findFirst().get();

        BackupCodeCredentialModel backupCodeCredentialModel = BackupCodeCredentialModel.createFromCredentialModel(credentialModel);

        this.codeNumber = backupCodeCredentialModel.getNextBackupCode().getNumber();
    }

    public int getCodeNumber() {
        return this.codeNumber;
    }

}
