package org.keycloak.representations.idm.authorization;

import java.io.IOException;
import java.util.Map;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

public class PolicyConfigSerializer extends StdSerializer<Map<String, String>> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public PolicyConfigSerializer() {
        this(null);
    }

    public PolicyConfigSerializer(Class<Map<String, String>> t) {
        super(t);
    }

    @Override
    public void serialize(Map<String, String> map, JsonGenerator generator, SerializerProvider provider) throws IOException {
        generator.writeStartObject();

        for (Map.Entry<String, String> entry : map.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            generator.writeFieldName(key);

            // check if the value string looks like a JSON array
            if (value != null && value.startsWith("[\"") && value.endsWith("\"]")) {
                try {
                    // attempt to read the string as a JSON node (which should be an ArrayNode)
                    ArrayNode arrayNode = (ArrayNode) MAPPER.readTree(value);
                    // write the ArrayNode directly to the generator
                    generator.writeTree(arrayNode);
                } catch (Exception e) {
                    // if parsing fails, write the value as a plain string
                    generator.writeString(value);
                }
            } else {
                // not an array string, so write as a plain string
                generator.writeString(value);
            }
        }
        generator.writeEndObject();
    }
}
