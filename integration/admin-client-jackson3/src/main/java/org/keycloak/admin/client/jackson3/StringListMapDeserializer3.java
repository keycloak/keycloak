package org.keycloak.admin.client.jackson3;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ValueDeserializer;

class StringListMapDeserializer3 extends ValueDeserializer<Object> {

    @Override
    public Object deserialize(JsonParser p, DeserializationContext ctxt) {
        JsonNode jsonNode = ctxt.readTree(p);
        Map<String, List<String>> map = new HashMap<>();
        for (Map.Entry<String, JsonNode> e : jsonNode.properties()) {
            List<String> values = new LinkedList<>();
            if (!e.getValue().isArray()) {
                values.add(e.getValue().isNull() ? null : e.getValue().asString());
            } else {
                for (JsonNode node : e.getValue()) {
                    values.add(node.isNull() ? null : node.asString());
                }
            }
            map.put(e.getKey(), values);
        }
        return map;
    }
}
