package org.keycloak.forms.login.freemarker.model;

import java.util.Optional;

import org.keycloak.credential.CredentialModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.RecoveryAuthnCodesCredentialModel;
import org.keycloak.models.utils.RecoveryAuthnCodesUtils;

public class RecoveryAuthnCodeInputLoginBean {

    private final int codeNumber;

    public RecoveryAuthnCodeInputLoginBean(KeycloakSession session, RealmModel realm, UserModel user) {
        Optional<CredentialModel> credentialModelOpt = RecoveryAuthnCodesUtils.getCredential(user);

        RecoveryAuthnCodesCredentialModel recoveryCodeCredentialModel = RecoveryAuthnCodesCredentialModel.createFromCredentialModel(credentialModelOpt.get());

        this.codeNumber = recoveryCodeCredentialModel.getNextRecoveryAuthnCode().get().getNumber();
    }

    public int getCodeNumber() {
        return this.codeNumber;
    }

}
