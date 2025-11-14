package org.keycloak.sdjwt.vp;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.keycloak.OID4VCConstants.CLAIM_NAME_AUD;
import static org.keycloak.OID4VCConstants.CLAIM_NAME_EXP;
import static org.keycloak.OID4VCConstants.CLAIM_NAME_IAT;
import static org.keycloak.OID4VCConstants.CLAIM_NAME_NBF;
import static org.keycloak.OID4VCConstants.CLAIM_NAME_NONCE;
import static org.keycloak.OID4VCConstants.SD_HASH;

public class KeyBindingPayload {

    @JsonProperty(CLAIM_NAME_IAT)
    private Long issuedAt;

    @JsonProperty(CLAIM_NAME_AUD)
    private String audience;

    @JsonProperty(CLAIM_NAME_NONCE)
    private String nonce;

    @JsonProperty(SD_HASH)
    private String sdHash;

    @JsonProperty(CLAIM_NAME_EXP)
    private Long exp;

    @JsonProperty(CLAIM_NAME_NBF)
    private Long nbf;

    private Map<String, Object> otherClaims = new HashMap<>();

    public Long getIssuedAt() {
        return issuedAt;
    }

    public void setIssuedAt(Long issuedAt) {
        this.issuedAt = issuedAt;
    }

    public String getAudience() {
        return audience;
    }

    public void setAudience(String audience) {
        this.audience = audience;
    }

    public String getNonce() {
        return nonce;
    }

    public void setNonce(String nonce) {
        this.nonce = nonce;
    }

    public String getSdHash() {
        return sdHash;
    }

    public void setSdHash(String sdHash) {
        this.sdHash = sdHash;
    }

    public Long getExp() {
        return exp;
    }

    public void setExp(Long exp) {
        this.exp = exp;
    }

    public Long getNbf() {
        return nbf;
    }

    public void setNbf(Long nbf) {
        this.nbf = nbf;
    }

    @JsonAnyGetter
    public Map<String, Object> getOtherClaims() {
        return otherClaims;
    }

    @JsonAnySetter
    public void setOtherClaims(String name, Object value) {
        otherClaims.put(name, value);
    }

    @Override
    public String toString() {
        try {
            return JsonSerialization.writeValueAsString(this);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
