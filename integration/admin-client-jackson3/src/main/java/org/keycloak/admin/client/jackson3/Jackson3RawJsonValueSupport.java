package org.keycloak.admin.client.jackson3;

import org.keycloak.json.RawJsonValue;
import org.keycloak.json.RawJsonValueSupport;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

public final class Jackson3RawJsonValueSupport implements RawJsonValueSupport {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    @Override
    public int getPriority() {
        // Jackson 3 must have priority as our test framework brings Jackson 2
        return 1;
    }

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
        return toNode(value).stringValue();
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
