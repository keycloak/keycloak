package org.keycloak.models.credential.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class RecoveryAuthnCodeRepresentation {

    private final int number;
    private final String encodedHashedValue;

    @JsonCreator
    public RecoveryAuthnCodeRepresentation(@JsonProperty("number") int number,
            @JsonProperty("encodedHashedValue") String encodedHashedValue) {
        this.number = number;
        this.encodedHashedValue = encodedHashedValue;
    }

    public int getNumber() {
        return this.number;
    }

    public String getEncodedHashedValue() {
        return this.encodedHashedValue;
    }

}
