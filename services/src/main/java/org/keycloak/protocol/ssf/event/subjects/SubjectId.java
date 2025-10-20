package org.keycloak.protocol.ssf.event.subjects;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.Map;

/**
 * A Subject Identifier is structured information that describes a subject related to a security event, using named
 * formats to define its encoding as JSON objects within Security Event Tokens.
 *
 * See: https://datatracker.ietf.org/doc/html/rfc9493
 */
public abstract class SubjectId {

    @JsonProperty("format")
    protected String format;

    @JsonIgnore
    protected Map<String, Object> attributes = new HashMap<>();

    public SubjectId(String format) {
        this.format = format;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    @JsonAnySetter
    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }
}
