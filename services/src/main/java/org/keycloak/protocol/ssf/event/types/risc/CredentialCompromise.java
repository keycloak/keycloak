package org.keycloak.protocol.ssf.event.types.risc;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A Credential Compromise event signals that the identifier specified in the subject was found to be compromised.
 */
public class CredentialCompromise extends RiscEvent {

    /**
     * See: https://openid.net/specs/openid-risc-profile-specification-1_0.html#rfc.section.2.7
     */
    public static final String TYPE = "https://schemas.openid.net/secevent/risc/event-type/credential-compromise";

    /**
     * REQUIRED. The type of credential that is compromised. The value of this attribute must be one of the values specified for the similarly named field in the Credential Change event defined in the CAEP Specification.
     */
    @JsonProperty("credential_type")
    private String credentialType;

    public CredentialCompromise() {
        super(TYPE);
    }

    public String getCredentialType() {
        return credentialType;
    }

    public void setCredentialType(String credentialType) {
        this.credentialType = credentialType;
    }
}
