package org.keycloak.representations.workflows;

import java.io.IOException;
import java.util.Map.Entry;

import org.keycloak.common.util.MultivaluedHashMap;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

public final class MultivaluedHashMapValueDeserializer extends JsonDeserializer {

    @Override
    public Object deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        MultivaluedHashMap<String, String> map = new MultivaluedHashMap<>();
        JsonNode node = p.getCodec().readTree(p);

        if (node.isObject()) {
            for (Entry<String, JsonNode> property : node.properties()) {
                String key = property.getKey();
                JsonNode values = property.getValue();

                if (values.isArray()) {
                    for (JsonNode value : values) {
                        map.add(key, value.asText());
                    }
                } else {
                    map.add(key, values.asText());
                }
            }
        }

        return map;
    }
}
