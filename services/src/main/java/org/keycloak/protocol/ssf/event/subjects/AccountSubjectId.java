package org.keycloak.protocol.ssf.event.subjects;

/**
 * See: https://datatracker.ietf.org/doc/html/rfc9493#name-email-identifier-format
 */
public class AccountSubjectId extends SubjectId {

    public static final String TYPE = "account";

    protected String uri;

    public AccountSubjectId() {
        super(TYPE);
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    @Override
    public String toString() {
        return "AccountSubjectId{" +
               "uri='" + uri + '\'' +
               '}';
    }
}
