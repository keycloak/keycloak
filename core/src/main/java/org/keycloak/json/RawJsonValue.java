package org.keycloak.json;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * A Jackson-version-agnostic replacement for {@code JsonNode}.
 * <p>
 * Jackson 2 uses {@code com.fasterxml.jackson.databind.JsonNode}, Jackson 3 uses
 * {@code tools.jackson.databind.JsonNode} — different classes in different packages.
 * This type wraps an arbitrary JSON value and delegates all navigation to a
 * {@link RawJsonValueSupport} implementation backed by the real {@code JsonNode}
 * from whichever Jackson version is on the classpath.
 * <p>
 * Self-describing to any Jackson version via shared annotations
 * ({@link JsonCreator} and {@link JsonValue} from {@code com.fasterxml.jackson.annotation}).
 */
public final class RawJsonValue implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Object value;

    private RawJsonValue(Object value) {
        this.value = value;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static RawJsonValue of(Object value) {
        if (value instanceof RawJsonValue) {
            return (RawJsonValue) value;
        }
        return new RawJsonValue(value);
    }

    @JsonValue
    public Object getValue() {
        return value;
    }

    public RawJsonValue get(String key) {
        return support().get(this, key);
    }

    public boolean asBoolean() {
        return support().asBoolean(value);
    }

    public int asInt() {
        return support().asInt(value);
    }

    public long asLong() {
        return support().asLong(value);
    }

    public String asText() {
        return support().asText(value);
    }

    public String textValue() {
        return support().textValue(value);
    }

    public boolean isEmpty() {
        return support().isEmpty(value);
    }

    /**
     * Converts the underlying value to the given type using the {@link KeycloakJsonMapper}.
     * Useful for server-side code that needs a Jackson-version-specific type (e.g., {@code JsonNode}).
     */
    public static <T> T unwrap(Class<T> type, RawJsonValue rawJsonValue) {
        Object value = rawJsonValue == null ? null : rawJsonValue.value;
        if (type.isInstance(value)) {
            return type.cast(value);
        }
        return KeycloakJsonMapperFactory.mapper().convertValue(value, type);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RawJsonValue)) return false;
        return support().valuesEqual(value, ((RawJsonValue) o).value);
    }

    @Override
    public int hashCode() {
        return support().valueHashCode(value);
    }

    @Override
    public String toString() {
        return support().toJsonString(value);
    }

    private static RawJsonValueSupport support() {
        return RawJsonValueSupport.getInstance();
    }
}
