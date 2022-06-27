package org.keycloak.forms.login.freemarker.model;

import org.keycloak.credential.CredentialModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.RecoveryAuthnCodesCredentialModel;

public class RecoveryAuthnCodeInputLoginBean {

    private final int codeNumber;

    public RecoveryAuthnCodeInputLoginBean(KeycloakSession session, RealmModel realm, UserModel user) {
        CredentialModel credentialModel = user.credentialManager().getStoredCredentialsByTypeStream(RecoveryAuthnCodesCredentialModel.TYPE)
                                                 .findFirst().get();

        RecoveryAuthnCodesCredentialModel recoveryCodeCredentialModel = RecoveryAuthnCodesCredentialModel.createFromCredentialModel(credentialModel);

        this.codeNumber = recoveryCodeCredentialModel.getNextRecoveryAuthnCode().get().getNumber();
    }

    public int getCodeNumber() {
        return this.codeNumber;
    }

}
