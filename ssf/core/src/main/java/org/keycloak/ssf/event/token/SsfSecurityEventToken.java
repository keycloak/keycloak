package org.keycloak.ssf.event.token;

import org.keycloak.ssf.subject.SubjectId;
import org.keycloak.ssf.subject.SubjectIdJsonDeserializer;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 * Represents a RFC8417 Security Event Token (SET).
 *
 * See: https://datatracker.ietf.org/doc/html/rfc8417
 */
public class SsfSecurityEventToken extends AbstractSecurityEventToken {

    @JsonProperty("sub_id")
    @JsonDeserialize(using = SubjectIdJsonDeserializer.class)
    protected SubjectId subjectId;

    @JsonProperty("txn")
    protected String txn;

    public SubjectId getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(SubjectId subjectId) {
        this.subjectId = subjectId;
    }

    public SsfSecurityEventToken subjectId(SubjectId subjectId) {
        setSubjectId(subjectId);
        return this;
    }

    public SsfSecurityEventToken txn(String txn) {
        setTxn(txn);
        return this;
    }

    public String getTxn() {
        return txn;
    }

    public void setTxn(String txn) {
        this.txn = txn;
    }
}
