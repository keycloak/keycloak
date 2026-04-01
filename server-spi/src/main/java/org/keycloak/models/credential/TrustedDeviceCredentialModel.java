package org.keycloak.models.credential;

import java.io.IOException;

import org.keycloak.common.util.Time;
import org.keycloak.credential.CredentialModel;
import org.keycloak.models.credential.dto.TrustedDeviceCredentialData;
import org.keycloak.models.credential.dto.TrustedDeviceSecretData;
import org.keycloak.util.JsonSerialization;

public class TrustedDeviceCredentialModel extends CredentialModel {
    public static final String TYPE = "trusted-device";

    private final TrustedDeviceCredentialData credentialData;
    private final TrustedDeviceSecretData secretData;

    private TrustedDeviceCredentialModel(TrustedDeviceCredentialData credentialData,
                                         TrustedDeviceSecretData secretData) {
        this.credentialData = credentialData;
        this.secretData = secretData;
    }

    public Long getExpireTime() {
        return credentialData.getExpireTime();
    }

    public String getDeviceId() {
        return secretData.getDeviceId();
    }

    public static TrustedDeviceCredentialModel create(String userLabel, String deviceId, Long expireTime) {
        TrustedDeviceCredentialData trustedDeviceCredentialData = new TrustedDeviceCredentialData(expireTime);
        TrustedDeviceSecretData trustedDeviceSecretData = new TrustedDeviceSecretData(deviceId);
        TrustedDeviceCredentialModel credentialModel = new TrustedDeviceCredentialModel(
                trustedDeviceCredentialData, trustedDeviceSecretData);

        credentialModel.setType(TYPE);

        credentialModel.fillCredentialModelFields();
        credentialModel.setUserLabel(userLabel);

        return credentialModel;
    }

    public static TrustedDeviceCredentialModel createFromCredentialModel(CredentialModel credentialModel) {
        try {
            TrustedDeviceCredentialData credentialData = JsonSerialization.readValue(
                    credentialModel.getCredentialData(), TrustedDeviceCredentialData.class);
            TrustedDeviceSecretData secretData = JsonSerialization.readValue(
                    credentialModel.getSecretData(),
                    TrustedDeviceSecretData.class
            );

            TrustedDeviceCredentialModel trustedDeviceCredentialModel = new TrustedDeviceCredentialModel(
                    credentialData, secretData);
            trustedDeviceCredentialModel.setUserLabel(credentialModel.getUserLabel());
            trustedDeviceCredentialModel.setCreatedDate(credentialModel.getCreatedDate());
            trustedDeviceCredentialModel.setType(credentialModel.getType());
            trustedDeviceCredentialModel.setId(credentialModel.getId());
            trustedDeviceCredentialModel.setSecretData(credentialModel.getSecretData());
            trustedDeviceCredentialModel.setCredentialData(credentialModel.getCredentialData());
            return trustedDeviceCredentialModel;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void fillCredentialModelFields() {
        try {
            setCredentialData(JsonSerialization.writeValueAsString(credentialData));
            setSecretData(JsonSerialization.writeValueAsString(secretData));
            setCreatedDate(Time.currentTimeMillis());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
