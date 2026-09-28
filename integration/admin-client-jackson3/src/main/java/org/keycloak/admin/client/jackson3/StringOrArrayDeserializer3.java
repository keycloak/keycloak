package org.keycloak.admin.client.jackson3;

import java.util.ArrayList;

import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ValueDeserializer;

class StringOrArrayDeserializer3 extends ValueDeserializer<Object> {

    @Override
    public Object deserialize(JsonParser p, DeserializationContext ctxt) {
        JsonNode jsonNode = ctxt.readTree(p);
        if (jsonNode.isArray()) {
            ArrayList<String> a = new ArrayList<>(1);
            for (JsonNode child : jsonNode) {
                a.add(child.stringValue());
            }
            return a.toArray(new String[0]);
        } else {
            return new String[] { jsonNode.stringValue() };
        }
    }
}
