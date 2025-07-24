package org.keycloak.representations.openid_federation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TrustMarks {

    @JsonProperty("trust_mark_type")
    private String trustMarkType;
    @JsonProperty("trust_mark")
    private String trustMark;
    private Map<String, Object> otherClaims = new HashMap<String, Object>();

    public String getTrustMarkType() {
        return trustMarkType;
    }

    public void setTrustMarkType(String trustMarkType) {
        this.trustMarkType = trustMarkType;
    }

    public String getTrustMark() {
        return trustMark;
    }

    public void setTrustMark(String trustMark) {
        this.trustMark = trustMark;
    }

    public Map<String, Object> getOtherClaims() {
        return otherClaims;
    }

    public void setOtherClaims(Map<String, Object> otherClaims) {
        this.otherClaims = otherClaims;
    }
}
