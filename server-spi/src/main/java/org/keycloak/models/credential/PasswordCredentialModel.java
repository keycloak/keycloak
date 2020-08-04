package org.keycloak.models.credential;

import org.keycloak.credential.CredentialModel;
import org.keycloak.models.credential.dto.PasswordCredentialData;
import org.keycloak.models.credential.dto.PasswordSecretData;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;

public class PasswordCredentialModel extends CredentialModel {

    public static final String TYPE = "password";
    public static final String PASSWORD_HISTORY = "password-history";

    private final PasswordCredentialData credentialData;
    private final PasswordSecretData secretData;

    private PasswordCredentialModel(PasswordCredentialData credentialData, PasswordSecretData secretData) {
        this.credentialData = credentialData;
        this.secretData = secretData;
    }

    public static PasswordCredentialModel createFromValues(PasswordCredentialData credentialData, PasswordSecretData secretData) {
        return new PasswordCredentialModel(credentialData, secretData);
    }

    public static PasswordCredentialModel createFromValues(String algorithm, byte[] salt, int hashIterations, String encodedPassword){
        PasswordCredentialData credentialData = new PasswordCredentialData(hashIterations, algorithm);
        PasswordSecretData secretData = new PasswordSecretData(encodedPassword, salt);

        PasswordCredentialModel passwordCredentialModel = new PasswordCredentialModel(credentialData, secretData);

        try {
            passwordCredentialModel.setCredentialData(JsonSerialization.writeValueAsString(credentialData));
            passwordCredentialModel.setSecretData(JsonSerialization.writeValueAsString(secretData));
            passwordCredentialModel.setType(TYPE);
            return passwordCredentialModel;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static PasswordCredentialModel createFromCredentialModel(CredentialModel credentialModel) {
        try {
            PasswordCredentialData credentialData = JsonSerialization.readValue(credentialModel.getCredentialData(),
                    PasswordCredentialData.class);
            PasswordSecretData secretData = JsonSerialization.readValue(credentialModel.getSecretData(), PasswordSecretData.class);

            PasswordCredentialModel passwordCredentialModel = new PasswordCredentialModel(credentialData, secretData);
            passwordCredentialModel.setCreatedDate(credentialModel.getCreatedDate());
            passwordCredentialModel.setCredentialData(credentialModel.getCredentialData());
            passwordCredentialModel.setId(credentialModel.getId());
            passwordCredentialModel.setSecretData(credentialModel.getSecretData());
            passwordCredentialModel.setType(credentialModel.getType());
            passwordCredentialModel.setUserLabel(credentialModel.getUserLabel());

            return passwordCredentialModel;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    public PasswordCredentialData getPasswordCredentialData() {
        return credentialData;
    }

    public PasswordSecretData getPasswordSecretData() {
        return secretData;
    }


}
