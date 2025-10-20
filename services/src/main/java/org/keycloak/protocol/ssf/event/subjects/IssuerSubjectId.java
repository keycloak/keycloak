package org.keycloak.protocol.ssf.event.subjects;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * See: https://datatracker.ietf.org/doc/html/rfc9493#name-issuer-and-subject-identifi
 */
public class IssuerSubjectId extends SubjectId {

    public static final String TYPE = "iss_sub";

    @JsonProperty("iss")
    protected String iss;

    @JsonProperty("sub")
    protected String sub;

    public IssuerSubjectId() {
        super(TYPE);
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

    @Override
    public String toString() {
        return "IssuerSubjectId{" +
               "iss='" + iss + '\'' +
               ", sub='" + sub + '\'' +
               '}';
    }
}
