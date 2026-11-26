package org.keycloak.ssf.subject;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A Subject Identifier is structured information that describes a subject related to a security event, using named
 * formats to define its encoding as JSON objects within Security Event Tokens.
 *
 * <p>This class is deliberately NOT annotated with a class-level
 * {@code @JsonDeserialize}: doing so would propagate to every concrete
 * subclass via Jackson's annotation inheritance and {@link SubjectIdJsonDeserializer}
 * itself dispatches to a concrete subclass through {@code treeToValue},
 * which would loop. Call sites that need to deserialize the abstract
 * {@code SubjectId} type either use a field-level
 * {@code @JsonDeserialize(using = SubjectIdJsonDeserializer.class)}
 * (e.g. {@code SsfEmitEventRequest.sub_id}) or invoke the deserializer
 * via {@code SubjectIds.fromTree(...)}.
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
