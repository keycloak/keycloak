package org.keycloak.models.credential.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class RecoveryAuthnCodesSecretData {

    private final List<RecoveryAuthnCodeRepresentation> codes;

    @JsonCreator
    public RecoveryAuthnCodesSecretData(@JsonProperty("codes") List<RecoveryAuthnCodeRepresentation> codes) {
        this.codes = codes;
    }

    public List<RecoveryAuthnCodeRepresentation> getCodes() {
        return this.codes;
    }

    public void removeNextBackupCode() {
        this.codes.remove(0);
    }

}
