package org.keycloak.representations.openid_federation;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.Map;

public class TrustMark {

    @JsonProperty("trust_mark_id")
    private String trustMarkId;
    @JsonProperty("trust_mark")
    private String trustMark;
    private String iss;
    private String sub;
    private Long iat;
    @JsonProperty("logo_uri")
    private String logoUri;
    private Long exp;
    private String ref;
    private String delegation;
    protected Map<String, Object> otherClaims = new HashMap<String, Object>();

    public String getTrustMarkId() {
        return trustMarkId;
    }

    public void setTrustMarkId(String trustMarkId) {
        this.trustMarkId = trustMarkId;
    }

    public String getTrustMark() {
        return trustMark;
    }

    public void setTrustMark(String trustMark) {
        this.trustMark = trustMark;
    }

    public String getIss() {
        return iss;
    }

    public void setIss(String iss) {
        this.iss = iss;
    }

    public String getSub() {
        return sub;
    }

    public void setSub(String sub) {
        this.sub = sub;
    }

    public Long getIat() {
        return iat;
    }

    public void setIat(Long iat) {
        this.iat = iat;
    }

    public String getLogoUri() {
        return logoUri;
    }

    public void setLogoUri(String logoUri) {
        this.logoUri = logoUri;
    }

    public Long getExp() {
        return exp;
    }

    public void setExp(Long exp) {
        this.exp = exp;
    }

    public String getRef() {
        return ref;
    }

    public void setRef(String ref) {
        this.ref = ref;
    }

    public String getDelegation() {
        return delegation;
    }

    public void setDelegation(String delegation) {
        this.delegation = delegation;
    }

    @JsonAnyGetter
    public Map<String, Object> getOtherClaims() {
        return otherClaims;
    }

    @JsonAnySetter
    public void setOtherClaims(String name, Object value) {
        otherClaims.put(name, value);
    }

}
