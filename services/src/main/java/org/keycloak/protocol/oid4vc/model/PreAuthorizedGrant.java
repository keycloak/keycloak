package org.keycloak.protocol.oid4vc.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class PreAuthorizedGrant {

    @JsonProperty("urn:ietf:params:oauth:grant-type:pre-authorized_code")
    private PreAuthorizedCode preAuthorizedCode;

    public PreAuthorizedCode getPreAuthorizedCode() {
        return preAuthorizedCode;
    }

    public PreAuthorizedGrant setPreAuthorizedCode(PreAuthorizedCode preAuthorizedCode) {
        this.preAuthorizedCode = preAuthorizedCode;
        return this;
    }
}