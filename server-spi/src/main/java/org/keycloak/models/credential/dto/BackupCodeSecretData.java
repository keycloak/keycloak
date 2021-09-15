package org.keycloak.models.credential.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class BackupCodeSecretData {

    private final List<BackupCode> codes;

    @JsonCreator
    public BackupCodeSecretData(@JsonProperty("codes") List<BackupCode> codes) {
        this.codes = codes;
    }

    public List<BackupCode> getCodes() {
        return this.codes;
    }

    public void removeNextBackupCode() {
        this.codes.remove(0);
    }

}
