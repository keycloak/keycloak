package org.keycloak.protocol.oid4vc.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class PreAuthorizedCode {

    @JsonProperty("pre-authorized_code")
    private String preAuthorizedCode;
    @JsonProperty("pre-user_pin_required")
    private boolean userPinRequired;

    public String getPreAuthorizedCode() {
        return preAuthorizedCode;
    }

    public PreAuthorizedCode setPreAuthorizedCode(String preAuthorizedCode) {
        this.preAuthorizedCode = preAuthorizedCode;
        return this;
    }

    public boolean getUserPinRequired() {
        return userPinRequired;
    }

    public PreAuthorizedCode setUserPinRequired(boolean userPinRequired) {
        this.userPinRequired = userPinRequired;
        return this;
    }
}