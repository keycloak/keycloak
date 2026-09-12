package org.keycloak.representations.idm.authorization;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.ArrayNode;

public class PolicyConfigDeserializer extends StdDeserializer<Map<String,String>> {

    public PolicyConfigDeserializer() {
        this(null);
    }

    public PolicyConfigDeserializer(Class<Map<String, String>> t) {
        super(t);
    }

    @Override
    public Map<String, String> deserialize(JsonParser parser, DeserializationContext context) throws IOException {

        // ensure we are at the start of the JSON object for the map
        if (parser.currentToken() != JsonToken.START_OBJECT) {
            context.reportWrongTokenException(Map.class, JsonToken.START_OBJECT,
                    "Expected START_OBJECT for config map");
        }

        Map<String, String> map = new HashMap<>();
        ObjectMapper mapper = (ObjectMapper) parser.getCodec();

        // loop through key-value pairs in the JSON object
        while (parser.nextToken() != JsonToken.END_OBJECT) {
            // get the key and value
            String key = parser.currentName();
            parser.nextToken();

            // check the type of the value token
            if (parser.currentToken() == JsonToken.START_ARRAY) {
                // case 1: value is a JSON array
                ArrayNode arrayNode = mapper.readTree(parser);

                // convert the ArrayNode back into a single string (the original format)
                String originalStringFormat = arrayNode.toString();
                map.put(key, originalStringFormat);
            } else if (parser.currentToken() == JsonToken.VALUE_STRING) {
                // case 2: value is a JSON String (the regular case)
                String value = parser.getText();
                map.put(key, value);
            } else {
                // handle other unexpected types, if necessary
                context.reportWrongTokenException(Map.class, parser.currentToken(),
                        "Expected String or Array for config value");
            }
        }
        return map;
    }
}
