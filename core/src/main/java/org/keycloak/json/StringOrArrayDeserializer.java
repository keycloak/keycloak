package org.keycloak.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

public class StringOrArrayDeserializer extends JsonDeserializer<Object> {

    @Override
    public Object deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        JsonNode jsonNode = jsonParser.readValueAsTree();
        if (jsonNode.isArray()) {
            ArrayList<String> a = new ArrayList<>(1);
            Iterator<JsonNode> itr = jsonNode.iterator();
            while (itr.hasNext()) {
                a.add(itr.next().textValue());
            }
            return a.toArray(new String[a.size()]);
        } else {
            return new String[] { jsonNode.textValue() };
        }
    }

}
