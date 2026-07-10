package org.keycloak.ssf.event;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a generic SSF event.
 * <p>
 * See: https://datatracker.ietf.org/doc/html/rfc8417
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public abstract class SsfEvent {

    private static final ConcurrentMap<Class<?>, Set<String>> DECLARED_JSON_PROPERTIES = new ConcurrentHashMap<>();

    /**
     * Internal (shorter) alias for the event type.
     */
    @JsonIgnore
    protected String alias;

    /**
     * The event type URI
     */
    @JsonIgnore
    protected String eventType;

    /**
     * Additional unmapped event-specific fields.
     */
    @JsonIgnore
    protected Map<String, Object> attributes = new HashMap<>();

    public SsfEvent(String eventType) {
        this(eventType, null);
    }

    public SsfEvent(String eventType, String alias) {
        this.eventType = eventType;
        // use the simple class name as the default alias
        this.alias = alias == null ? getClass().getSimpleName() : alias;
    }

    public String getEventType() {
        return eventType;
    }

    /**
     * Exposes {@link #attributes} as a Jackson "any-getter" so each entry is
     * rendered as a top-level JSON field on the event object (flattened
     * alongside the declared {@code @JsonProperty} fields), matching the
     * SSF §4.2.3 extension-field placement rather than nesting them under
     * an {@code "attributes"} key.
     */
    @JsonAnyGetter
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    @JsonAnySetter
    public void setAttributeValue(String key, Object value) {
        if (declaredJsonPropertyNames(getClass()).contains(key)) {
            throw new IllegalArgumentException(
                    "Custom attribute key '" + key + "' collides with a declared @JsonProperty on "
                    + getClass().getName());
        }
        attributes.put(key, value);
    }

    private static Set<String> declaredJsonPropertyNames(Class<?> type) {
        return DECLARED_JSON_PROPERTIES.computeIfAbsent(type, t -> {
            Set<String> names = new HashSet<>();
            for (Class<?> c = t; c != null && c != Object.class; c = c.getSuperclass()) {
                for (Field f : c.getDeclaredFields()) {
                    JsonProperty ann = f.getAnnotation(JsonProperty.class);
                    if (ann != null && !ann.value().isEmpty()) {
                        names.add(ann.value());
                    }
                }
            }
            return Set.copyOf(names);
        });
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    /**
     * Verify that this event instance carries the fields the SSF /
     * CAEP / RISC spec marks as REQUIRED. Called by the synthetic-emit
     * pipeline after Jackson has materialised the event from the
     * caller-supplied JSON, so a missing-field mistake is rejected
     * with a clear error before the SET is signed and dispatched.
     *
     * <p>Default implementation is a no-op — most CAEP/RISC events have
     * no strictly-required fields beyond the subject, which the
     * pipeline validates separately. Subclasses with mandatory fields
     * (e.g. {@code CaepCredentialChange.change_type}) override this
     * to throw {@link SsfEventValidationException} with a message
     * naming the missing field.
     *
     * <p>Native event production (the SSF event listener) builds
     * instances from typed Keycloak event details that always supply
     * the required fields, so the hook only matters on the
     * synthetic-emit path. Custom extension events use this same hook
     * to enforce their own invariants.
     */
    public void validate() {
        // no-op — overridden by event subclasses that have spec-required fields
    }

    @Override
    public String toString() {
        Map<String, Object> fields = new LinkedHashMap<>();
        appendFields(fields);
        if (attributes != null && !attributes.isEmpty()) {
            fields.putIfAbsent("attributes", attributes);
        }
        StringJoiner rendered = new StringJoiner(", ");
        for (Map.Entry<String, Object> entry : fields.entrySet()) {
            Object value = entry.getValue();
            if (value == null) {
                continue;
            }
            rendered.add(entry.getKey() + "=" + (value instanceof String ? "'" + value + '\'' : value));
        }
        String name = alias != null ? alias : getClass().getSimpleName();
        return rendered.length() == 0 ? name : name + "::" + rendered;
    }

    /**
     * Contributes the fields of this level of the event hierarchy to the
     * {@link #toString()} output; insertion order is the render order.
     * Subclasses override this (calling {@code super.appendFields(fields)} first)
     * instead of {@code toString()} itself. Values may be put unconditionally —
     * {@code null} entries are filtered and {@link String} values quoted centrally
     * when rendering, and the extension {@link #attributes} map is appended
     * automatically when non-empty.
     */
    protected void appendFields(Map<String, Object> fields) {
        fields.put("eventType", eventType);
    }
}
