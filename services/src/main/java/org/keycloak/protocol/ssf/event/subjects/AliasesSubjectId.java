package org.keycloak.protocol.ssf.event.subjects;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * See: https://datatracker.ietf.org/doc/html/rfc9493#name-aliases-identifier-format
 */
public class AliasesSubjectId extends SubjectId {

    public static final String TYPE = "aliases";

    @JsonProperty("identifiers")
    protected List<Map<String, String>> identifiers;

    public AliasesSubjectId() {
        super(TYPE);
    }

    public List<Map<String, String>> getIdentifiers() {
        return identifiers;
    }

    public void setIdentifiers(List<Map<String, String>> identifiers) {
        this.identifiers = identifiers;
    }

    @Override
    public String toString() {
        return "AliasesSubjectId{" +
               "identifiers=" + identifiers +
               '}';
    }
}
