package org.keycloak.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Jackson 2 implementation of {@link RawJsonValueSupport} that delegates all navigation
 * to the real {@link JsonNode} from {@code com.fasterxml.jackson.databind}.
 */
public final class Jackson2RawJsonValueSupport implements RawJsonValueSupport {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static JsonNode toNode(Object value) {
        if (value instanceof JsonNode) {
            return (JsonNode) value;
        }
        return MAPPER.valueToTree(value);
    }

    @Override
    public RawJsonValue get(RawJsonValue node, String key) {
        JsonNode jn = toNode(node.getValue());
        JsonNode child = jn.get(key);
        if (child == null) {
            return null;
        }
        return RawJsonValue.of(child);
    }

    @Override
    public boolean asBoolean(Object value) {
        return toNode(value).asBoolean();
    }

    @Override
    public int asInt(Object value) {
        return toNode(value).asInt();
    }

    @Override
    public long asLong(Object value) {
        return toNode(value).asLong();
    }

    @Override
    public String asText(Object value) {
        return toNode(value).asText();
    }

    @Override
    public String textValue(Object value) {
        return toNode(value).textValue();
    }

    @Override
    public boolean isEmpty(Object value) {
        return toNode(value).isEmpty();
    }

    @Override
    public String toJsonString(Object value) {
        return toNode(value).toString();
    }

    @Override
    public boolean valuesEqual(Object a, Object b) {
        return toNode(a).equals(toNode(b));
    }

    @Override
    public int valueHashCode(Object value) {
        return toNode(value).hashCode();
    }
}
