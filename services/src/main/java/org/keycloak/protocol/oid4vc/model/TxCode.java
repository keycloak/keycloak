package org.keycloak.protocol.oid4vc.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a transaction code as used in the pre-authorized grant in the Credential Offer in OID4VCI
 * {@see https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html#name-credential-offer}
 *
 * @author <a href="https://github.com/wistefan">Stefan Wiedemann</a>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TxCode {

    @JsonProperty("input_mode")
    private String inputMode;

    @JsonProperty("length")
    private int length;

    @JsonProperty("description")
    private String description;

    public String getInputMode() {
        return inputMode;
    }

    public TxCode setInputMode(String inputMode) {
        this.inputMode = inputMode;
        return this;
    }

    public int getLength() {
        return length;
    }

    public TxCode setLength(int length) {
        this.length = length;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public TxCode setDescription(String description) {
        this.description = description;
        return this;
    }
}
