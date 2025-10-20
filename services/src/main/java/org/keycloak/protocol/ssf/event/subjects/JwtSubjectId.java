package org.keycloak.protocol.ssf.event.subjects;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * See: https://openid.net/specs/openid-sse-framework-1_0.html#sub-id-jwt-id
 */
public class JwtSubjectId extends SubjectId {

    public static final String TYPE = "jwt_id";

    @JsonProperty("iss")
    protected String iss;

    @JsonProperty("jti")
    protected String jti;

    public JwtSubjectId() {
        super(TYPE);
    }

    public String getIss() {
        return iss;
    }

    public void setIss(String iss) {
        this.iss = iss;
    }

    public String getJti() {
        return jti;
    }

    public void setJti(String jti) {
        this.jti = jti;
    }

    @Override
    public String toString() {
        return "JwtSubjectId{" +
               "iss='" + iss + '\'' +
               ", jti='" + jti + '\'' +
               '}';
    }
}
