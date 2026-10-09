package org.keycloak.admin.client.jackson3;

import java.util.Map.Entry;

import org.keycloak.common.util.MultivaluedHashMap;

import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ValueDeserializer;

class MultivaluedHashMapValueDeserializer3 extends ValueDeserializer<Object> {

    @Override
    public Object deserialize(JsonParser p, DeserializationContext ctxt) {
        MultivaluedHashMap<String, String> map = new MultivaluedHashMap<>();
        JsonNode node = ctxt.readTree(p);

        if (node.isObject()) {
            for (Entry<String, JsonNode> property : node.properties()) {
                String key = property.getKey();
                JsonNode values = property.getValue();

                if (values.isArray()) {
                    for (JsonNode value : values) {
                        map.add(key, value.asString());
                    }
                } else {
                    map.add(key, values.asString());
                }
            }
        }

        return map;
    }
}
