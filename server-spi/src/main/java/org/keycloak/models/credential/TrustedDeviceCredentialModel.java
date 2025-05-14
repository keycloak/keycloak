package org.keycloak.models.credential;

import org.keycloak.common.util.Time;
import org.keycloak.credential.CredentialModel;
import org.keycloak.models.credential.dto.TrustedDeviceCredentialData;
import org.keycloak.models.credential.dto.TrustedDeviceSecretData;
import org.keycloak.representations.account.DeviceRepresentation;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;
import java.util.UUID;

import static org.keycloak.utils.StringUtil.isBlank;

public class TrustedDeviceCredentialModel extends CredentialModel {
    public static final String TYPE = "trusted-device";

    private final TrustedDeviceCredentialData credentialData;
    private final TrustedDeviceSecretData secretData;

    private TrustedDeviceCredentialModel(TrustedDeviceCredentialData credentialData, TrustedDeviceSecretData secretData) {
        this.credentialData = credentialData;
        this.secretData = secretData;
    }

    public static TrustedDeviceCredentialModel create(DeviceRepresentation device, String secret) {
        TrustedDeviceCredentialData credentialData = new TrustedDeviceCredentialData(device);
        TrustedDeviceSecretData secretData = new TrustedDeviceSecretData(secret);
        String userLabel = device.getOs() + ' ' + device.getOsVersion() + " / " + device.getBrowser();

        try {
            TrustedDeviceCredentialModel tdCredentialModel = new TrustedDeviceCredentialModel(credentialData, secretData);

            tdCredentialModel.setType(TYPE);
            tdCredentialModel.setUserLabel(userLabel);
            tdCredentialModel.setCreatedDate(Time.currentTimeMillis());
            tdCredentialModel.setSecretData(JsonSerialization.writeValueAsString(secretData));
            tdCredentialModel.setCredentialData(JsonSerialization.writeValueAsString(credentialData));
            tdCredentialModel.setId(UUID.randomUUID().toString()); // Generate a new ID manually, to be able to use it for cookie creation

            return tdCredentialModel;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static TrustedDeviceCredentialModel createFromCredentialModel(CredentialModel credentialModel) {
        try {
            TrustedDeviceCredentialData credentialData = isBlank(credentialModel.getCredentialData()) ?
                    null :
                    JsonSerialization.readValue(credentialModel.getCredentialData(), TrustedDeviceCredentialData.class);

            TrustedDeviceSecretData secretData = isBlank(credentialModel.getSecretData()) ?
                    null :
                    JsonSerialization.readValue(credentialModel.getSecretData(), TrustedDeviceSecretData.class);

            return getTrustedDeviceCredentialModel(credentialModel, credentialData, secretData);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static TrustedDeviceCredentialModel getTrustedDeviceCredentialModel(CredentialModel credentialModel, TrustedDeviceCredentialData credentialData, TrustedDeviceSecretData secretData) {
        TrustedDeviceCredentialModel tdCredentialModel = new TrustedDeviceCredentialModel(credentialData, secretData);
        tdCredentialModel.setId(credentialModel.getId());
        tdCredentialModel.setType(credentialModel.getType());
        tdCredentialModel.setUserLabel(credentialModel.getUserLabel());
        tdCredentialModel.setCreatedDate(credentialModel.getCreatedDate());
        tdCredentialModel.setSecretData(credentialModel.getSecretData());
        tdCredentialModel.setCredentialData(credentialModel.getCredentialData());
        return tdCredentialModel;
    }

    public TrustedDeviceCredentialData getTrustedDeviceCredentialData() {
        return credentialData;
    }

    public TrustedDeviceSecretData getTrustedDeviceSecretData() {
        return secretData;
    }
}
