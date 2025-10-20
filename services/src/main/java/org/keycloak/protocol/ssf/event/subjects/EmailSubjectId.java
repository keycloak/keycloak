package org.keycloak.protocol.ssf.event.subjects;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * See: https://datatracker.ietf.org/doc/html/rfc9493#name-email-identifier-format
 */
public class EmailSubjectId extends SubjectId {

    public static final String TYPE = "email";

    @JsonProperty("email")
    protected String email;

    public EmailSubjectId() {
        super(TYPE);
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @Override
    public String toString() {
        return "EmailSubjectId{" +
               "email='" + email + '\'' +
               '}';
    }
}
