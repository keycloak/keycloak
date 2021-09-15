package org.keycloak.models.credential.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class BackupCode {

    private final int number;
    private final String value;

    @JsonCreator
    public BackupCode(@JsonProperty("number") int number, @JsonProperty("value") String value) {
        this.number = number;
        this.value = value;
    }

    public int getNumber() {
        return number;
    }

    public String getValue() {
        return value;
    }

}
